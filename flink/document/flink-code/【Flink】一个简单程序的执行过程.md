# 【Flink】一个简单程序的执行过程

[toc]

## 一、代码和概览

对于下面简单的 Flink 代码，想知道：

1. 如何创建从指定host和port接收数据的数据源；
2.  如何对创建好的数据源进行一系列操作来实现所需功能；
3. 如何将分析结果打印出来。

```java
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.Collector;

public class SocketWindowWordCount {
    public static void main(String[] args) throws Exception {
        /** 需要连接的主机名和端口 */
        final String hostname;
        final int port;
        try {
            final ParameterTool params = ParameterTool.fromArgs(args);
            hostname = params.get("hostname");
            port = params.getInt("port");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Please run 'SocketWindowWordCount --host <host> --port <port>'");
            return;
        }

        /** 获取{@link StreamExecutionEnvironment}的具体实现的实例 */
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        /** 通过连接给定地址和端口, 获取数据流的数据源 */
        DataStream<String> text = env.socketTextStream(hostname, port);

        /** 对数据流中的数据进行解析、分组、窗口、以及聚合 */
        DataStream<WordWithCount> windowCounts = text
                .flatMap(new FlatMapFunction<String, WordWithCount>() {
                    @Override
                    public void flatMap(String value, Collector<WordWithCount> out) {
                        for (String word : value.split("\\s")) {
                            out.collect(new WordWithCount(word, 1L));
                        }
                    }
                })
                .keyBy("word")
                .timeWindow(Time.seconds(5), Time.seconds(1))
                .reduce(new ReduceFunction<WordWithCount>() {
                    @Override
                    public WordWithCount reduce(WordWithCount a, WordWithCount b) {
                        return new WordWithCount(a.word, a.count + b.count);
                    }
                });

        /** 打印出分析结果 */
        windowCounts.print();

        /** 懒加载，执行处理程序 */
        env.execute("Socket Window WordCount");
    }

    /**
     * 单词和统计次数的数据结构
     */
    public static class WordWithCount {
        public String word;
        public long count;

        public WordWithCount(String word, long count) {
            this.word = word;
            this.count = count;
        }

        @Override
        public String toString() {
            return word + " : " + count;
        }
    }
}
```

## 二、构建数据源

在Flink中，数据源的构建是通过StreamExecutionEnviroment的具体实现的实例来构建的，如上述代码中的这句代码。

```java
DataStream<String> text = env.socketTextStream(hostname, port);
```

这句代码就在指定的host和port上构建了一个接受网络数据的数据源，其内部实现如下：

```java
public DataStreamSource<String> socketTextStream(String hostname, int port) {
    return socketTextStream(hostname, port, "\n");
}

public DataStreamSource<String> socketTextStream(String hostname, int port, String delimiter) {
    return socketTextStream(hostname, port, delimiter, 0);
}

public DataStreamSource<String> socketTextStream(String hostname, int port, String delimiter, long maxRetry) {
    return addSource(new SocketTextStreamFunction(hostname, port, delimiter, maxRetry),
                     "Socket Stream");
}
```

可以看出是对 socketTextStream 方法的重载调用，最后会根据传入的hostname、port，以及默认的行分隔符”\n”，和最大尝试次数0，构造一个SocketTextStreamFunction实例，默认的数据源节点名称为”Socket Stream”。

### 2.1、Function

SocketTextStreamFunction 是SourceFunction的一个子类，而SourceFunction是Flink中数据源的基础接口。

```java
@Public
public interface SourceFunction<T> extends Function, Serializable {
   void run(SourceContext<T> ctx) throws Exception;
   void cancel();
   @Public
   interface SourceContext<T> {
      void collect(T element);
      @PublicEvolving
      void collectWithTimestamp(T element, long timestamp);
      @PublicEvolving
      void emitWatermark(Watermark mark);
      @PublicEvolving
      void markAsTemporarilyIdle();
      Object getCheckpointLock();
      void close();
   }
}
```

主要有两个方法和一个内部接口：

* run(SourceContex)方法：实现数据获取逻辑的地方，并可以通过传入的参数ctx进行向下游节点的数据转发。
* cancel()方法：用来取消数据源的数据产生，一般在run方法中，会存在一个循环来持续产生数据，而cancel方法则可以使得该循环终止。
* 其内部接口SourceContex则是用来进行数据发送的接口。

了解了SourceFunction这个接口的功能后，来看下SocketTextStreamFunction的具体实现，也就是主要看其run方法的具体实现。

```java
public void run(SourceContext<String> ctx) throws Exception {
   final StringBuilder buffer = new StringBuilder();
   long attempt = 0;
   /** 这里是第一层循环，只要当前处于运行状态，该循环就不会退出，会一直循环 */
   while (isRunning) {
      try (Socket socket = new Socket()) {
         /** 对指定的hostname和port，建立Socket连接，并构建一个BufferedReader，用来从Socket中读取数据 */
         currentSocket = socket;
         LOG.info("Connecting to server socket " + hostname + ':' + port);
         socket.connect(new InetSocketAddress(hostname, port), CONNECTION_TIMEOUT_TIME);
         BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
         char[] cbuf = new char[8192];
         int bytesRead;
         /** 这里是第二层循环，对运行状态进行了双重校验，同时对从Socket中读取的字节数进行判断 */
         while (isRunning && (bytesRead = reader.read(cbuf)) != -1) {
            buffer.append(cbuf, 0, bytesRead);
            int delimPos;
            /** 这里是第三层循环，就是对从Socket中读取到的数据，按行分隔符进行分割，并将每行数据作为一个整体字符串向下游转发 */
            while (buffer.length() >= delimiter.length() && (delimPos = buffer.indexOf(delimiter)) != -1) {
               String record = buffer.substring(0, delimPos);
               if (delimiter.equals("\n") && record.endsWith("\r")) {
                  record = record.substring(0, record.length() - 1);
               }
               /** 用入参ctx，进行数据的转发 */
               ctx.collect(record);
               buffer.delete(0, delimPos + delimiter.length());
            }
         }
      }
      /** 如果由于遇到EOF字符，导致从循环中退出，则根据运行状态，以及设置的最大重试尝试次数，决定是否进行 sleep and retry，或者直接退出循环 */
      if (isRunning) {
         attempt++;
         if (maxNumRetries == -1 || attempt < maxNumRetries) {
            LOG.warn("Lost connection to server socket. Retrying in " + delayBetweenRetries + " msecs...");
            Thread.sleep(delayBetweenRetries);
         }
         else {
            break;
         }
      }
   }
   /** 在最外层的循环都退出后，最后检查下缓存中是否还有数据，如果有，则向下游转发 */
   if (buffer.length() > 0) {
      ctx.collect(buffer.toString());
   }
}
```

run方法的逻辑如上，逻辑很清晰，就是从指定的hostname和port持续不断的读取数据，按行分隔符划分成一个个字符串，然后转发到下游。

cancel方法的实现如下，就是将运行状态的标识isRunning属性设置为false，并根据需要关闭当前socket。

```java
public void cancel() {
   isRunning = false;
   Socket theSocket = this.currentSocket;
   /** 如果当前socket不为null，则进行关闭操作 */
   if (theSocket != null) {
      IOUtils.closeSocket(theSocket);
   }
}
```

### 2.2、StreamOperator

对SocketTextStreamFunction的实现清楚之后，回到StreamExecutionEnvironment中，看addSource方法。

```java
public <OUT> DataStreamSource<OUT> addSource(SourceFunction<OUT> function, String sourceName) {
   return addSource(function, sourceName, null);
}

public <OUT> DataStreamSource<OUT> addSource(SourceFunction<OUT> function, String sourceName, TypeInformation<OUT> typeInfo) {
   /** 如果传入的输出数据类型信息为null，则尝试提取输出数据的类型信息 */
   if (typeInfo == null) {
      if (function instanceof ResultTypeQueryable) {
         /** 如果传入的function实现了ResultTypeQueryable接口, 则直接通过接口获取 */
         typeInfo = ((ResultTypeQueryable<OUT>) function).getProducedType();
      } else {
         try {
            /** 通过反射机制来提取类型信息 */
            typeInfo = TypeExtractor.createTypeInfo(
                  SourceFunction.class,
                  function.getClass(), 0, null, null);
         } catch (final InvalidTypesException e) {
            /** 提取失败, 则返回一个MissingTypeInfo实例 */
            typeInfo = (TypeInformation<OUT>) new MissingTypeInfo(sourceName, e);
         }
      }
   }
   /** 根据function是否是ParallelSourceFunction的子类实例来判断是否是一个并行数据源节点 */
   boolean isParallel = function instanceof ParallelSourceFunction;
   /** 闭包清理, 可减少序列化内容, 以及防止序列化出错 */
   clean(function);
   StreamSource<OUT, ?> sourceOperator;
   /** 根据function是否是StoppableFunction的子类实例, 来决定构建不同的StreamOperator */
   if (function instanceof StoppableFunction) {
      sourceOperator = new StoppableStreamSource<>(cast2StoppableSourceFunction(function));
   } else {
      sourceOperator = new StreamSource<>(function);
   }
   /** 返回一个新构建的DataStreamSource实例 */
   return new DataStreamSource<>(this, typeInfo, sourceOperator, isParallel, sourceName);
}
```

通过对addSource重载方法的依次调用，最后得到了一个DataStreamSource的实例。

其中TypeInformation是Flink的类型系统中的核心类，用作函数输入和输出的类型都需要通过TypeInformation来表示，TypeInformation可以看做是数据类型的一个工具，可以通过它获取对应数据类型的序列化器和比较器等。

由于SocketTextStreamFunction不是继承自ParallelSourceFunction，且实现stoppableFunction接口，isParallel的值为false，以及sourceOperator变量对应的是一个StreamSource实例。

> StreamSource是StreamOperator接口的一个具体实现类，其构造函数的入参就是一个SourceFunction的子类实例，这里就是前面介绍过的SocketTextStreamFunciton的实例，构造过程如下：
>
> ```java
> public StreamSource(SRC sourceFunction) {
>    super(sourceFunction);
>    this.chainingStrategy = ChainingStrategy.HEAD;
> }
> 
> public AbstractUdfStreamOperator(F userFunction) {
>    this.userFunction = requireNonNull(userFunction);
>    checkUdfCheckpointingPreconditions();
> }
> 
> private void checkUdfCheckpointingPreconditions() {
>    if (userFunction instanceof CheckpointedFunction && userFunction instanceof ListCheckpointed) {
>       throw new IllegalStateException("User functions are not allowed to implement AND ListCheckpointed.");
>    }
> }
> ```

构造过程的逻辑很明了，把传入的userFunction赋值给自己的属性变量，并对传入的userFunction做了校验工作，然后将链接策略设置为HEAD。

Flink中为了优化执行效率，会对数据处理链中的相邻节点会进行合并处理，链接策略有三种：

- ALWAYS —— 尽可能的与前后节点进行链接；
- NEVER —— 不与前后节点进行链接；
- HEAD —— 只能与后面的节点链接，不能与前面的节点链接。

作为数据源的源头，是最顶端的节点了，所以只能采用HEAD或者NEVER，对于StreamSource，采用的是HEAD策略。

StreamOperator是Flink中流操作符的基础接口，其抽象子类AbstractStreamOperator实现了一些公共方法，用户自定义的数据处理逻辑会被封装在StreamOperator的具体实现子类中。

### 2.3、StreamTransformation & DataStream

在sourceOperator变量被赋值后，即开始进行DataStreamSource的实例构建，并作为数据源构造调用的返回结果。

```java
return new DataStreamSource<>(this, typeInfo, sourceOperator, isParallel, sourceName);
```

DataStreamSource是具有一个预定义输出类型的DataStream。

在Flink中，DataStream描述了一个具有相同数据类型的数据流，其提供了数据操作的各种API，如map、reduce等，通过这些API，可以对数据流中的数据进行各种操作，DataStreamSource的构建过程如下：

```java
public DataStreamSource(StreamExecutionEnvironment environment,
      TypeInformation<T> outTypeInfo, StreamSource<T, ?> operator,
      boolean isParallel, String sourceName) {
   super(environment, new SourceTransformation<>(sourceName, operator, outTypeInfo, environment.getParallelism()));
   this.isParallel = isParallel;
   if (!isParallel) {
      setParallelism(1);
   }
}

protected SingleOutputStreamOperator(StreamExecutionEnvironment environment, StreamTransformation<T> transformation) {
   super(environment, transformation);
}

public DataStream(StreamExecutionEnvironment environment, StreamTransformation<T> transformation) {
   this.environment = Preconditions.checkNotNull(environment, "Execution Environment must not be null.");
   this.transformation = Preconditions.checkNotNull(transformation, "Stream Transformation must not be null.");
}
```

可见构建过程就是初始化了DataStream中的environment和transformation这两个属性。

其中transformation赋值的是SourceTranformation的一个实例，SourceTransformation是StreamTransformation的子类，而StreamTransformation则描述了创建一个DataStream的操作。对于每个DataStream，其底层都是有一个StreamTransformation的具体实例的，所以在DataStream在构造初始时会为其属性transformation设置一个具体的实例。并且DataStream的很多接口的调用都是直接调用的StreamTransformation的相应接口，如并行度、id、输出数据类型信息、资源描述等。

通过上述过程，根据指定的hostname和port进行数据产生的数据源就构造完成了，获得的是一个DataStreamSource的实例，描述的是一个输出数据类型是String的数据流的源。

--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

在上述的数据源的构建过程中，出现Function(SourceFunction)、StreamOperator、StreamTransformation、DataStream这四个接口：

* Function接口：**用户通过继承该接口的不同子类来实现用户自己的数据处理逻辑**，如上述中实现了SourceFunction这个子类，来实现从指定hostname和port来接收数据，并转发字符串的逻辑；
* StreamOperator接口：**数据流操作符的基础接口**，该接口的具体实现子类中，会有保存用户自定义数据处理逻辑的函数的属性，负责对userFunction的调用，以及调用时传入所需参数，比如在StreamSource这个类中，在调用SourceFunction的run方法时，会构建一个SourceContext的具体实例，作为入参，用于run方法中，进行数据的转发；
* StreamTransformation接口：**该接口描述了构建一个DataStream的操作**，以及该操作的并行度、输出数据类型等信息，并有一个属性，用来持有StreamOperator的一个具体实例；
* DataStream：**描述的是一个具有相同数据类型的数据流**，底层是通过具体的StreamTransformation来实现，其负责提供各种对流上的数据进行操作转换的API接口。

通过上述的关系，最终用户自定义数据处理逻辑的函数，以及并行度、输出数据类型等就都包含在了DataStream中，而DataStream也就可以很好的描述一个具体的数据流了。

上述四个接口的包含关系是这样的：Function –> StreamOperator –> StreamTransformation –> DataStream。

通过数据源的构造，理清Flink数据流中的几个接口的关系后，接下来再来看如何在数据源上进行各种操作，达到最终的数据统计分析的目的。

## 三、操作数据流

对上述获取到的DataStreamSource，进行具体的转换操作，具体操作就是这段逻辑。

```java
DataStream<WordWithCount> windowCounts = text
        .flatMap(new FlatMapFunction<String, WordWithCount>() {
            @Override
            public void flatMap(String value, Collector<WordWithCount> out) {
                for (String word : value.split("\\s")) {
                    out.collect(new WordWithCount(word, 1L));
                }
            }
        })
        .keyBy("word")
        .timeWindow(Time.seconds(5), Time.seconds(1))
        .reduce(new ReduceFunction<WordWithCount>() {
            @Override
            public WordWithCount reduce(WordWithCount a, WordWithCount b) {
                return new WordWithCount(a.word, a.count + b.count);
            }
        });
```

这段逻辑中，对数据流做了四次操作，分别是flatMap、keyBy、timeWindow、reduce，接下来分别介绍每个转换都做了些什么操作。

### 3.1、flatMap转换

flatMap的入参是一个FlatMapFunction的具体实现，功能就是将接收到的字符串，按空格切割成不同单词，然后每个单词构建一个WordWithCount实例，然后向下游转发，用于后续的数据统计。然后调用DataStream的flatMap方法，进行数据流的转换，如下：

```java
public <R> SingleOutputStreamOperator<R> flatMap(FlatMapFunction<T, R> flatMapper) {
   TypeInformation<R> outType = TypeExtractor.getFlatMapReturnTypes(clean(flatMapper),
         getType(), Utils.getCallLocationName(), true);
   /** 根据传入的flatMapper这个Function，构建StreamFlatMap这个StreamOperator的具体子类实例 */
   return transform("Flat Map", outType, new StreamFlatMap<>(clean(flatMapper)));
}

public <R> SingleOutputStreamOperator<R> transform(String operatorName, TypeInformation<R> outTypeInfo, OneInputStreamOperator<T, R> operator) {
   /** 读取输入转换的输出类型, 如果是MissingTypeInfo, 则及时抛出异常, 终止操作 */
   transformation.getOutputType();
   OneInputTransformation<T, R> resultTransform = new OneInputTransformation<>(
         this.transformation,
         operatorName,
         operator,
         outTypeInfo,
         environment.getParallelism());
   @SuppressWarnings({ "unchecked", "rawtypes" })
   SingleOutputStreamOperator<R> returnStream = new SingleOutputStreamOperator(environment, resultTransform);
   getExecutionEnvironment().addOperator(resultTransform);
   return returnStream;
}
```

整个构建过程，与构建数据源的过程相似：

1. 先根据传入的flatMapper这个Function构建一个StreamOperator的具体子类StreamFlatMap的实例；
2. 根据第一步中构建的StreamFlatMap的实例，构建出OneInputTransFormation这个StreamTransformation的子类的实例；
3. 再构建出DataStream的子类SingleOutputStreamOperator的实例。

上述逻辑中，除了构建出了SingleOutputStreamOperator这个实例为并返回外，还有一句代码：

```java
getExecutionEnvironment().addOperator(resultTransform);

public void addOperator(StreamTransformation<?> transformation) {
   Preconditions.checkNotNull(transformation, "transformation must not be null.");
   this.transformations.add(transformation);
}
```

就是将上述构建的OneInputTransFormation的实例，添加到StreamExecutionEnvironment的类型为List的属性transformations中。

### 3.2、keyBy转换

这里的keyBy转换，入参是一个字符串”word”，意思是按照WordWithCount中的word字段进行分区操作。

```java
public KeyedStream<T, Tuple> keyBy(String... fields) {
   return keyBy(new Keys.ExpressionKeys<>(fields, getType()));
}
```

先根据传入的字段名数组，以及数据流的输出数据类型信息，构建出用来描述key的ExpressionKeys的实例，ExpressionKeys有两个属性：

```java
/** key字段的列表, FlatFieldDescriptor 描述了每个key, 在所在类型中的位置以及key自身的数据类信息 */
private List<FlatFieldDescriptor> keyFields;
/** 包含key的数据类型的类型信息, 与构造函数入参中的字段顺序一一对应 */
private TypeInformation<?>[] originalKeyTypes;
```

在获取key的描述之后，继续调用keyBy的重载方法：

```java
private KeyedStream<T, Tuple> keyBy(Keys<T> keys) {
   return new KeyedStream<>(this, clean(KeySelectorUtil.getSelectorForKeys(keys,
         getType(), getExecutionConfig())));
}
```

这里首先构建了一个KeySelector的子类ComparableKeySelector的实例，作用就是从具体的输入实例中，提取出key字段对应的值(可能是多个key字段)组成的元组(Tuple)。

对于这里的例子，就是从每个WordWithCount实例中，提取出word字段的值。

然后构建一个KeyedStream的实例，KeyedStream也是DataStream的子类。构建过程如下：

```java
public KeyedStream(DataStream<T> dataStream, KeySelector<T, KEY> keySelector) {
   this(dataStream, keySelector, TypeExtractor.getKeySelectorTypes(keySelector, dataStream.getType()));
}

public KeyedStream(DataStream<T> dataStream, KeySelector<T, KEY> keySelector, TypeInformation<KEY> keyType) {
   super(
      dataStream.getExecutionEnvironment(),
      new PartitionTransformation<>(
         dataStream.getTransformation(),
         new KeyGroupStreamPartitioner<>(keySelector, StreamGraphGenerator.DEFAULT_LOWER_BOUND_MAX_PARALLELISM)));
   this.keySelector = keySelector;
   this.keyType = validateKeyType(keyType);
}
```

在进行父类构造函数调用之前，先基于keySelector构造了一个KeyGroupStreamPartitioner的实例，再进一步构造了一个PartitionTransformation实例。

这里与flatMap的转换略有不同：

1. flatMap中，根据传入的flatMapper这个Function构建的是StreamOperator这个接口的子类的实例，而keyBy中，则是根据keySelector构建了ChannelSelector接口的子类实例；
2. keyBy中构建的StreamTransformation实例，并没有添加到StreamExecutionEnvironment的属性transformations这个列表中。

ChannelSelector只有一个接口，根据传入的数据流中的具体数据记录，以及下个节点的并行度来决定该条记录需要转发到哪个通道。

```java
public interface ChannelSelector<T extends IOReadableWritable> {
   int[] selectChannels(T record, int numChannels);
}

// KeyGroupStreamPartitioner中该方法的实现如下：
public int[] selectChannels(
   SerializationDelegate<StreamRecord<T>> record,
   int numberOfOutputChannels) {
   K key;
   try {
      /** 通过keySelector从传入的record中提取出对应的key */
      key = keySelector.getKey(record.getInstance().getValue());
   } catch (Exception e) {
      throw new RuntimeException("Could not extract key from " + record.getInstance().getValue(), e);
   }
   /** 根据提取的key，最大并行度，以及输出通道数，决定出record要转发到的通道编号 */
   returnArray[0] = KeyGroupRangeAssignment.assignKeyToParallelOperator(key, maxParallelism, numberOfOutputChannels);
   return returnArray;
}
```

再进一步看一下KeyGroupRangerAssignment的assignKeyToParallelOperator方法的实现逻辑。

```java
public static int assignKeyToParallelOperator(Object key, int maxParallelism, int parallelism) {
   return computeOperatorIndexForKeyGroup(maxParallelism, parallelism, assignToKeyGroup(key, maxParallelism));
}

public static int assignToKeyGroup(Object key, int maxParallelism) {
   return computeKeyGroupForKeyHash(key.hashCode(), maxParallelism);
}

public static int computeKeyGroupForKeyHash(int keyHash, int maxParallelism) {
   return MathUtils.murmurHash(keyHash) % maxParallelism;
}

public static int computeOperatorIndexForKeyGroup(int maxParallelism, int parallelism, int keyGroupId) {
   return keyGroupId * parallelism / maxParallelism;
}
```

1. 先通过key的hashCode，算出maxParallelism的余数，也就是可以得到一个[0, maxParallelism)的整数；
2. 再通过公式 keyGroupId * parallelism / maxParallelism ，计算出一个[0, parallelism)区间的整数，从而实现分区功能。

### 3.3、timeWindow转换

这里timeWindow转换的入参是两个时间，第一个参数表示窗口长度，第二个参数表示窗口滑动的时间间隔。

```java
public WindowedStream<T, KEY, TimeWindow> timeWindow(Time size, Time slide) {
   if (environment.getStreamTimeCharacteristic() == TimeCharacteristic.ProcessingTime) {
      return window(SlidingProcessingTimeWindows.of(size, slide));
   } else {
      return window(SlidingEventTimeWindows.of(size, slide));
   }
}
```

根据环境配置的数据流处理时间特征构建不同的WindowAssigner的具体实例。WindowAssigner的功能就是对于给定的数据流中的记录，决定出该记录应该放入哪些窗口中，并提供触发器等供。默认的时间特征是ProcessingTime，所以这里会构建一个SlidingProcessingTimeWindow实例，来看下SlidingProcessingTimeWindow类的assignWindows方法的实现逻辑。

```java
public Collection<TimeWindow> assignWindows(Object element, long timestamp, WindowAssignerContext context) {
   /** 根据传入的WindowAssignerContext获取当前处理时间 */
   timestamp = context.getCurrentProcessingTime();
   List<TimeWindow> windows = new ArrayList<>((int) (size / slide));
   /** 获取最近一次的窗口的开始时间 */
   long lastStart = TimeWindow.getWindowStartWithOffset(timestamp, offset, slide);
   /** 循环找出满足条件的所有窗口 */
   for (long start = lastStart;
      start > timestamp - size;
      start -= slide) {
      windows.add(new TimeWindow(start, start + size));
   }
   return windows;
}
```

看一下根据给定时间戳获取最近一次的窗口的开始时间的实现逻辑。

```java
public static long getWindowStartWithOffset(long timestamp, long offset, long windowSize) {
   return timestamp - (timestamp - offset + windowSize) % windowSize;
}
```

通过上述获取WindowAssigner的子类实例后，调用window方法：

```java
public <W extends Window> WindowedStream<T, KEY, W> window(WindowAssigner<? super T, W> assigner) {
   return new WindowedStream<>(this, assigner);
}
```

比keyBy转换的逻辑还简单，就是构建了一个WindowedStream实例，然后返回，就结束了。而WindowedStream是一个新的数据流，不是DataStream的子类。

WindowedStream描述一个数据流中的元素会基于key进行分组，并且对于每个key，对应的元素会被划分到多个时间窗口内。然后窗口会基于触发器，将对应窗口中的数据转发到下游节点。

### 3.4、reduce转换

reduce转换的入参是一个ReduceFunction的具体实现，这里的逻辑就是对收到的WordWithCount实例集合，将其中word字段相同的实际的count值累加。

```java
public SingleOutputStreamOperator<T> reduce(ReduceFunction<T> function) {
   if (function instanceof RichFunction) {
      throw new UnsupportedOperationException("ReduceFunction of reduce can not be a RichFunction. " +
         "Please use reduce(ReduceFunction, WindowFunction) instead.");
   }
   /** 闭包清理 */
   function = input.getExecutionEnvironment().clean(function);
   return reduce(function, new PassThroughWindowFunction<K, W, T>());
}

public <R> SingleOutputStreamOperator<R> reduce(
      ReduceFunction<T> reduceFunction,
      WindowFunction<T, R, K, W> function) {

   TypeInformation<T> inType = input.getType();
   TypeInformation<R> resultType = getWindowFunctionReturnType(function, inType);
   return reduce(reduceFunction, function, resultType);
}

public <R> SingleOutputStreamOperator<R> reduce(ReduceFunction<T> reduceFunction, ProcessWindowFunction<T, R, K, W> function, TypeInformation<R> resultType) {
   if (reduceFunction instanceof RichFunction) {
      throw new UnsupportedOperationException("ReduceFunction of apply can not be a RichFunction.");
   }
   function = input.getExecutionEnvironment().clean(function);
   reduceFunction = input.getExecutionEnvironment().clean(reduceFunction);
   String callLocation = Utils.getCallLocationName();
   String udfName = "WindowedStream." + callLocation;
   String opName;
   KeySelector<T, K> keySel = input.getKeySelector();
   OneInputStreamOperator<T, R> operator;
   if (evictor != null) {
      @SuppressWarnings({"unchecked", "rawtypes"})
      TypeSerializer<StreamRecord<T>> streamRecordSerializer =
            (TypeSerializer<StreamRecord<T>>) new StreamElementSerializer(input.getType().createSerializer(getExecutionEnvironment().getConfig()));
      ListStateDescriptor<StreamRecord<T>> stateDesc =
            new ListStateDescriptor<>("window-contents", streamRecordSerializer);
      opName = "TriggerWindow(" + windowAssigner + ", " + stateDesc + ", " + trigger + ", " + evictor + ", " + udfName + ")";
      operator =
            new EvictingWindowOperator<>(windowAssigner,
                  windowAssigner.getWindowSerializer(getExecutionEnvironment().getConfig()),
                  keySel,
                  input.getKeyType().createSerializer(getExecutionEnvironment().getConfig()),
                  stateDesc,
                  new InternalIterableProcessWindowFunction<>(new ReduceApplyProcessWindowFunction<>(reduceFunction, function)),
                  trigger,
                  evictor,
                  allowedLateness,
                  lateDataOutputTag);
   } else {
      ReducingStateDescriptor<T> stateDesc = new ReducingStateDescriptor<>("window-contents",
            reduceFunction,
            input.getType().createSerializer(getExecutionEnvironment().getConfig()));
      opName = "TriggerWindow(" + windowAssigner + ", " + stateDesc + ", " + trigger + ", " + udfName + ")";
      operator =
            new WindowOperator<>(windowAssigner,
                  windowAssigner.getWindowSerializer(getExecutionEnvironment().getConfig()),
                  keySel,
                  input.getKeyType().createSerializer(getExecutionEnvironment().getConfig()),
                  stateDesc,
                  new InternalSingleValueProcessWindowFunction<>(function),
                  trigger,
                  allowedLateness,
                  lateDataOutputTag);
   }
   return input.transform(opName, resultType, operator);
}
```

通过对reduce重载方法的逐步调用，会走到上述代码的else逻辑中，这里也是先构建了StreamOperator的具体子类实例。

```java
public <R> SingleOutputStreamOperator<R> transform(String operatorName,
      TypeInformation<R> outTypeInfo, OneInputStreamOperator<T, R> operator) {
   SingleOutputStreamOperator<R> returnStream = super.transform(operatorName, outTypeInfo, operator);
   OneInputTransformation<T, R> transform = (OneInputTransformation<T, R>) returnStream.getTransformation();
   transform.setStateKeySelector(keySelector);
   transform.setStateKeyType(keyType);
   return returnStream;
}
```

父类的transform中的逻辑如下：

```java
public <R> SingleOutputStreamOperator<R> transform(String operatorName, TypeInformation<R> outTypeInfo, OneInputStreamOperator<T, R> operator) {
   /** 读取输入转换的输出类型, 如果是MissingTypeInfo, 则及时抛出异常, 终止操作 */
   transformation.getOutputType();
   OneInputTransformation<T, R> resultTransform = new OneInputTransformation<>(
         this.transformation,
         operatorName,
         operator,
         outTypeInfo,
         environment.getParallelism());
   @SuppressWarnings({ "unchecked", "rawtypes" })
   SingleOutputStreamOperator<R> returnStream = new SingleOutputStreamOperator(environment, resultTransform);
   getExecutionEnvironment().addOperator(resultTransform);
   return returnStream;
}
```

逻辑与flatMap相似，也是基于StreamOperator构建了一个StreamTransformation的子类OneInputTransformation的实例，然后构建了DataStream的子类SingleOutputStreamOperator的实例，最后也将构建的StreamTransformation的子类实例添加到了StreamExecutionEnvironment的属性transformations这个列表中。

经过上述操作，对数据流中的数据进行分组聚合的操作就完成了。

## 四、输出统计结果

统计结果的输出如下：

```java
windowCounts.print();
```

print方法就是在数据流的最后添加了一个Sink，用于承接统计结果。

```java
public DataStreamSink<T> print() {
   PrintSinkFunction<T> printFunction = new PrintSinkFunction<>();
   return addSink(printFunction);
}
```

PrintSinkFunction作为一个SinkFunction接口的实现，看下其对invoke方法的实现：

```java
public void invoke(IN record) {
   if (prefix != null) {
      stream.println(prefix + record.toString());
   }
   else {
      stream.println(record.toString());
   }
}
```

实现逻辑很清晰，就是将记录输出打印。继续看addSink方法：

```java
public DataStreamSink<T> addSink(SinkFunction<T> sinkFunction) {
   transformation.getOutputType();
   if (sinkFunction instanceof InputTypeConfigurable) {
      ((InputTypeConfigurable) sinkFunction).setInputType(getType(), getExecutionConfig());
   }
   StreamSink<T> sinkOperator = new StreamSink<>(clean(sinkFunction));
   DataStreamSink<T> sink = new DataStreamSink<>(this, sinkOperator);
   getExecutionEnvironment().addOperator(sink.getTransformation());
   return sink;
}
```

实现逻辑与数据源是相似的，先构建StreamOperator，再构建DataStreamSink，在DataStreamSink的构建中，会构造出StreamTransformation实例，最后会将这个StreamTransformation实例添加到StreamExecutionEnvironment的属性transformations这个列表中。

经过上述步骤，就完成了数据流的源构造、数据流的转换操作、数据流的Sink构造，在这个过程中，每次转换都会产生一个新的数据流，而每个数据流下几乎都有一个StreamTransformation的子类实例，对于像flatMap、reduce这些转换得到的数据流里的StreamTransformation会被添加到StreamExecutionEnvironment的属性transformations这个列表中，这个属性在后续构建StreamGraph时会使用到。

另外在这个数据流的构建与转换过程中，每个DataStream中的StreamTransformation的具体子类中都有一个input属性，该属性会记录该StreamTransformation的上游的DataStream的StreamTransformation引用，从而使得整个数据流中的StreamTransformation构成了一个隐式的链表，由于一个数据流可能会转换成多个输出数据流，以及多个输入数据流又有可能会合并成一个输出数据流，确定的说，不是隐式列表，而是一张隐式的图。

上述数据转换完成后，就会进行任务的执行，就是执行如下代码：

```java
env.execute("Socket Window WordCount");
```

这里就会根据上述的转换过程，先生成StreamGraph，再根据StreamGraph生成JobGraph，然后通过客户端提交到集群进行调度执行。