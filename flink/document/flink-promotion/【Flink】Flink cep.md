# 【Flink】Flink cep

[TOC]

## 一、概念

什么是 CEP：

* 复合事件处理（Complex Event Processing，CEP）
* Flink cep 是在 flink 中实现的一个复杂事件处理库
* 一个或多个简单事件构成的事件流通过一定的规则匹配，然后输出用户得到的数据--满足规则的复杂事件

CEP 的特征如下：

* 目标：从有序的简单事件流中发现一些高阶特征

* 输入：一个或多个简单事件构成的事件流

* 处理：识别简单事件之间的内在联系，多个符合一定规则的简单事件构成复杂事件；

* 输出：满足规则的复杂事件

市场上有多种 CEP 的解决方案，例如Spark、Samza、Beam等，但他们都没有提供专门的库支持。然而，Flink提供了专门的CEP库。

Flink CEP 包含如下组件：Event Stream、Pattern定义、Pattern检测和生成Alert。

* 首先，开发人员要在 DataStream 流上定义出模式条件
* 之后 Flink CEP 引擎进行模式检测，必要时生成警告

简单来说一下，其实可以把使用 flink CEP 当做平时用的正则表达式，cep中的 Pattern 就是定义的正则表达式，flink 中的DataStream 就是正则表达式中待匹配的字符串，flink 通过DataStream 和 自定义的Pattern进行匹配，生成一个经过过滤之后的DataStream。

基于自定义的pattern，可以做很多工作，比如监控报警、风控、反爬等等。

## 二、核心--Pattern API

处理事件的规则，被叫作模式（Pattern）。Flink CEP提供了Pattern API用于对输入流数据进行复杂事件规则定义，用来提取符合规则的事件序列。

模式大致分为两类：

**个体模式（Individual Patterns）：组成复杂规则的每一个单独的模式定义，就是个体模式。**

```scala
start.times(3).where(_.behavior.startsWith("fav"))
```

**组合模式（Combining Patterns，也叫模式序列）：很多个体模式组合起来，就形成了整个的模式序列。**

```scala
val pattern = Pattern.begin[Event]("start").where(_.getId == 42)
  .next("middle").subtype(classOf[SubEvent]).where(_.getVolume >= 10.0)
  .followedBy("end").where(_.getName == "end")
```

### 2.1、个体模式

个体模式包括单例模式和循环模式。单例模式只接收一个事件，而循环模式可以接收多个事件。

#### 2.1.1、量词

可以在一个个体模式后追加量词，也就是指定循环次数。

```scala
// 匹配出现4次
start.time(4)
// 匹配出现0次或4次
start.time(4).optional
// 匹配出现2、3或4次
start.time(2,4)
// 匹配出现2、3或4次，并且尽可能多地重复匹配
start.time(2,4).greedy
// 匹配出现1次或多次
start.oneOrMore
// 匹配出现0、2或多次，并且尽可能多地重复匹配
start.timesOrMore(2).optional.greedy
```

#### 2.1.2、条件

每个模式都需要指定触发条件，作为模式是否接受事件进入的判断依据。

CEP中的个体模式主要通过调用.where()、.or()和.until()来指定条件。按不同的调用方式，可以分成以下几类：

**简单条件：**通过.where()方法对事件中的字段进行判断筛选，决定是否接收该事件

```scala
start.where(event=>event.getName.startsWith("foo"))   
```

**组合条件：**将简单的条件进行合并，or()方法表示或逻辑相连，where的直接组合就相当于与and。

```scala
Pattern.where(event => …/*some condition*/).or(event => /*or condition*/)
```

**终止条件：**如果使用了oneOrMore或者oneOrMore.optional，建议使用.until()作为终止条件，以便清理状态。

**迭代条件：**能够对模式之前所有接收的事件进行处理，调用.where((value,ctx) => {…})，可以调用ctx.getEventForPattern("name")

### 2.2、组合模式

了解了独立模式，现在看看如何将它们组合成一个完整的模式序列。

模式序列必须以初始模式开始，如下所示：

```scala
val start : Pattern[Event, _] = Pattern.begin("start")
```

接下来，可以通过指定它们之间所需的连续条件，为模式序列添加更多模式。 Flink CEP 支持**事件之间**以下形式的邻接：

* 严格连续性（Strict Contiguity）：预期所有匹配事件一个接一个地出现，中间没有任何不匹配的事件。
* 宽松连续性（Relaxed Contiguity）：忽略匹配的事件之间出现的不匹配事件。
* 非确定性宽松连续性（Non-Deterministic Relaxed Contiguity）：进一步放宽邻接，允许忽略一些匹配事件的其他匹配。

要在连续模式之间应用它们，可以使用：

* next（）：用于严格连续
* followBy（）：用于宽松连续性
* followAyAny（）：用于非确定性宽松连续性

除了以上模式序列外，还可以定义“不希望出现某种近邻关系”：

* notNext（）：不想让某个事件严格紧邻前一个事件发生
* notFollowedBy（）：不想让某个事件在两个其他事件类型之间的任何位置

**需要注意**：

* 所有模式序列必须以.begin()开始
* 模式序列不能以.notFollowedBy()结束
* “not”类型的模式不能被optional所修饰
* 可以为模式指定时间约束，用来要求在多长时间内匹配有效：next.within(Time.seconds(10))

```scala
val start : Pattern[Event, _] = Pattern.begin("start")

// strict contiguity
val strict: Pattern[Event, _] = start.next("middle").where(...)
 
// relaxed contiguity
val relaxed: Pattern[Event, _] = start.followedBy("middle").where(...)
 
// non-deterministic relaxed contiguity
val nonDetermin: Pattern[Event, _] = start.followedByAny("middle").where(...)
 
// NOT pattern with strict contiguity
val strictNot: Pattern[Event, _] = start.notNext("not").where(...)
 
// NOT pattern with relaxed contiguity
val relaxedNot: Pattern[Event, _] = start.notFollowedBy("not").where(...)
```

宽松的连续性（Relaxed contiguity ）意味着仅匹配第一个匹配事件，而具有非确定性的松弛连续性（non-deterministic relaxed contiguity），将为同一个开始发出多个匹配。 例如模式“a b”，给定事件序列“a”，“c”，“b1”，“b2”将给出以下结果：

* “a”和“b”之间的严格连续性：{}（不匹配），“a”之后的“c”导致“a”被丢弃。
* “a”和“b”之间的宽松连续性：{a b1}，因为宽松的连续性被视为“跳过非匹配事件直到下一个匹配事件”。
* “a”和“b”之间的非确定性宽松连续性：{a b1}，{a b2}，因为这是最一般的形式。

也可以为模式定义时间约束以使其有效。 例如，可以通过pattern.within（）方法定义模式应在10秒内发生。 处理和事件时间都支持时间模式。

**注意模式：**序列只能有一个时间约束。 如果在不同的单独模式上定义了多个这样的约束，则应用最小的约束。

#### 2.2.1、循环模式中的连续性

可以在循环模式中应用与上一节中讨论的相同的连续条件。 

连续性将应用于接受到这种模式的元素之间。 为了举例说明上述情况，模式序列“a b + c”（“a”后跟一个或多个“b”的任何（非确定性宽松）序列，后跟“c”），输入“a” “，”“b1”，“d1”，“b2”，“d2”，“b3”“c”将产生以下结果：

* **严格连续性**：{a b3 c} - “b1”之后的“d1”导致“b1”被丢弃，“b2”因“d2”而发生同样的情况。
* **宽松的连续性**：{a b1 c}，{a b1 b2 c}，{a b1 b2 b3 c}，{a b2 c}，{a b2 b3 c}，{a b3 c} - “d”被忽略。
* **非确定性宽松邻接**：{a b1 c}，{a b1 b2 c}，{a b1 b3 c}，{a b1 b2 b3 c}，{a b2 c}，{a b2 b3 c}，{a b3 c} - 注意{a b1 b3 c}，这是宽松“b”之间邻接的结果。

对于循环模式（例如oneOrMore（）和times（）），**默认是宽松的连续性**。 如果想要严格的连续性，必须使用continuous（）调用显式指定它，如果想要非确定性的松弛连续性，可以使用allowCombinations（）调用。

#### 2.2.2、模式操作

**consecutive()**     

与oneOrMore()和times()结合使用，并在匹配事件之间强加严格的连续性，即任何不匹配的元素都会中断匹配（像next()）。

如果不应用，则使用松弛的连续性（如followBy()）。

```scala
Pattern.begin("start").where(_.getName().equals("c"))
  .followedBy("middle").where(_.getName().equals("a"))
                       .oneOrMore().consecutive()
  .followedBy("end1").where(_.getName().equals("b"))
```

为输入序列生成以下匹配项：C D A1 A2 A3 D A4 B.

* 严格连续应用：{C A1 B}，{C A1 A2 B}，{C A1 A2 A3 B}
* 没有严格连续应用：{C A1 B}，{C A1 A2 B}，{C A1 A2 A3 B}，{C A1 A2 A3 A4 B}

**allowCombinations()**     

与oneOrMore()和times()一起使用，并在匹配事件之间强加非确定性的松散连续性（像followAyAny()）。

如果不应用，则使用宽松的连续性（像followBy()）。

```scala
Pattern.begin("start").where(_.getName().equals("c"))
  .followedBy("middle").where(_.getName().equals("a"))
                       .oneOrMore().allowCombinations()
  .followedBy("end1").where(_.getName().equals("b"))
```

将为输入序列生成以下匹配项：C D A1 A2 A3 D A4 B.

* 启用combinations：{C A1 B}，{C A1 A2 B}，{C A1 A3 B}，{C A1 A4 B}，{C A1 A2 A3 B}，{C A1 A2 A4 B}，{C A1 A3 A4 B}，{C A1 A2 A3 A4 B}
* 未启用combinations：{C A1 B}，{C A1 A2 B}，{C A1 A2 A3 B}，{C A1 A2 A3 A4 B}

### 2.3、模式的检测

指定要查找的模式序列后，就可以将其应用于输入流以检测潜在匹配。调用CEP.pattern()，给定输入流和模式，就能得到一个PatternStream。

```scala
val input:DataStream[Event] = …
val pattern:Pattern[Event,_] = …
val patternStream:PatternStream[Event]=CEP.pattern(input,pattern)
```

### 2.4、匹配事件的提取

创建PatternStream之后，就可以应用select或者flatSelect方法，从检测到的事件序列中提取事件了。

select()方法需要输入一个select function作为参数，每个成功匹配的事件序列都会调用它。

select()以一个Map[String,Iterable[IN]]来接收匹配到的事件序列，其中key就是每个模式的名称，而value就是所有接收到的事件的Iterable类型。

```scala
def selectFn(pattern : Map[String,Iterable[IN]]):OUT={
  val startEvent = pattern.get("start").get.next
  val endEvent = pattern.get("end").get.next
  OUT(startEvent, endEvent)
}
```

### 2.5、处理超时部分模式

每当模式具有通过within关键字附加的窗口长度时，部分事件序列可能因为超过窗口长度而被丢弃。 要对超时的部分匹配进行操作，可以使用TimedOutPartialMatchHandler接口。

```java
class MyPatternProcessFunction<IN, OUT> extends PatternProcessFunction<IN, OUT> implements TimedOutPartialMatchHandler<IN> {
    @Override
    public void processMatch(Map<String, List<IN>> match, Context ctx, Collector<OUT> out) throws Exception;
        ...
    }
 
    @Override
    public void processTimedOutMatch(Map<String, List<IN>> match, Context ctx) throws Exception;
        IN startEvent = match.get("start").get(0);
        ctx.output(outputTag, T(startEvent));
    }
}
```

## 三、demo

这是来自尚硅谷的一个例子：检测一个用户在3秒内连续登陆失败。

首先要导入依赖：

```xml
<dependency>
    <groupId>org.apache.flink</groupId>
    <artifactId>flink-cep-scala_2.11</artifactId>
    <version>${flink.version}</version>
</dependency>
```

```scala
import java.util

import org.apache.flink.cep.PatternSelectFunction
import org.apache.flink.cep.scala.CEP
import org.apache.flink.cep.scala.pattern.Pattern
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time

// 输入的登录事件样例类
case class LoginEvent(userId: Long, ip: String, eventType: String, eventTime: Long)

// 输出的异常报警信息样例类
case class Warning(userId: Long, firstFailTime: Long, lastFailTime: Long, warningMsg: String)

/**
  * @author w1992wishes 2020/7/28 16:29
  */
object LoginFailWithCep {
  def main(args: Array[String]): Unit = {

    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(1)

    // 1. 读取事件数据，创建简单事件流
    val loginEventStream = env.readTextFile("E:\\project\\my_project\\daily-summary\\flink\\flink-details\\src\\main\\resources\\LoginLog.csv")
      .map(data => {
        val dataArray = data.split(",")
        LoginEvent(dataArray(0).trim.toLong, dataArray(1).trim, dataArray(2).trim, dataArray(3).trim.toLong)
      })
      .assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor[LoginEvent](Time.seconds(5)) {
        override def extractTimestamp(element: LoginEvent): Long = element.eventTime * 1000L
      })
      .keyBy(_.userId)

    // 2. 定义匹配模式
    val loginFailPattern = Pattern.begin[LoginEvent]("begin").where(_.eventType == "fail")
      .next("next").where(_.eventType == "fail")
      .within(Time.seconds(3))

    // 3. 在事件流上应用模式，得到一个pattern stream
    val patternStream = CEP.pattern(loginEventStream, loginFailPattern)

    // 4. 从pattern stream上应用select function，检出匹配事件序列
    val LoginFailDataStream = patternStream.select(new LoginFailMatch())

    LoginFailDataStream.print()

    env.execute("login fail with cep job")
  }
}

class LoginFailMatch() extends PatternSelectFunction[LoginEvent, Warning] {
  override def select(map: util.Map[String, util.List[LoginEvent]]): Warning = {
    val firstFail = map.get("begin").iterator().next()
    val lastFail = map.get("next").iterator().next()
    Warning(firstFail.userId, firstFail.eventTime, lastFail.eventTime, "login fail!")
  }
}
```

LoginLog.csv 内容如下：

```csv
5402,83.149.11.115,success,1558430815
23064,66.249.3.15,fail,1558430826
5692,80.149.25.29,fail,1558430833
7233,86.226.15.75,success,1558430832
5692,80.149.25.29,success,1558430840
29607,66.249.73.135,success,1558430841
1035,83.149.9.216,fail,1558430842
1035,83.149.9.216,fail,1558430846
1035,83.149.9.216,fail,1558430843
1035,83.149.24.26,fail,1558430844
7328,193.114.45.13,success,1558430848
29607,66.249.73.135,success,1558430847
2133,50.16.19.13,success,1558430857
6745,66.249.73.185,success,1558430859
76456,110.136.166.128,success,1558430853
8345,46.105.14.53,success,1558430855
76456,110.136.166.128,success,1558430857
76456,110.136.166.128,success,1558430854
76456,110.136.166.128,fail,1558430859
76456,110.136.166.128,success,1558430861
3464,123.125.71.35,success,1558430860
76456,110.136.166.128,success,1558430865
65322,50.150.204.184,success,1558430866
23565,207.241.237.225,fail,1558430862
8455,200.49.190.101,success,1558430867
8455,200.49.190.100,success,1558430865
8455,200.49.190.101,success,1558430869
8455,200.49.190.101,success,1558430872
32031,66.249.73.185,success,1558430875
12018,66.249.73.135,success,1558430874
12018,66.249.73.135,success,1558430879
12018,66.249.73.135,success,1558430881
21419,67.214.178.190,success,1558430882
21419,67.214.178.190,success,1558430880
23565,207.241.237.220,success,1558430881
2386,46.105.14.53,success,1558430883
23565,207.241.237.227,success,1558430884
83419,91.177.205.119,success,1558430881
83419,91.177.205.119,fail,1558430882
83419,91.177.205.119,success,1558430885
83419,91.177.205.119,fail,1558430886
83419,91.177.205.119,success,1558430884
83419,91.177.205.119,success,1558430886
4325,26.249.73.15,success,1558430888
2123,207.241.237.228,success,1558430887
21083,207.241.237.101,success,1558430889
13490,87.169.99.232,success,1558430886
93765,209.85.238.199,success,1558430890
93765,209.85.238.199,success,1558430892
```

也可看我 github：

## 四、来源

[Flink CEP详解](https://blog.csdn.net/huahuaxiaoshao/article/details/107520646)
[Flink难点：彻底明白CEP4，组合模式、循环模式介绍](https://www.aboutyun.com/forum.php?mod=viewthread&tid=27308)

