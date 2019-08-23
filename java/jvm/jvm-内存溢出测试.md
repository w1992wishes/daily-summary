# 【Jvm】内存溢出异常测试

## 一、JAVA堆异常溢出

### 1.1、制造堆溢出异常

```java
/**
 * Vm args:-Xms20M -Xmx20M -XX:+HeapDumpOnOutOfMemoryError
 * 堆的最小值参数-Xms，堆的最大值参数-Xmx
 * -XX:+HeapDumpOnOutOfMemoryError表示让虚拟机在出现内存异常时Dump出当前的内存堆转储快照
 * -XX:HeapDumpPath:为快照文件位置
 * Java 堆内存溢出测试，深入理解java虚拟机 p51
 */
//-Xms20m -Xmx20m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=E:/
public class HeapOOM {
    static class OOMObject {

    }

    public static void main(String[] args) {
        List<OOMObject> list = new ArrayList<OOMObject>();
        while (true) {
            list.add(new OOMObject());
        }
    }
}
```

如下在 Idea 中设置 VM 参数，运行。

![](https://ws1.sinaimg.cn/large/90c2c6e5gy1g3yq1mqvu1j20ky0do3z4.jpg)

出现如下错误：

```
java.lang.OutOfMemoryError: Java heap space
Dumping heap to E:/\java_pid10564.hprof ...
Heap dump file created [28298817 bytes in 0.078 secs]
Disconnected from the target VM, address: '127.0.0.1:60792', transport: 'socket'
Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
	at java.util.Arrays.copyOf(Arrays.java:3210)
	at java.util.Arrays.copyOf(Arrays.java:3181)
	at java.util.ArrayList.grow(ArrayList.java:265)
	at java.util.ArrayList.ensureExplicitCapacity(ArrayList.java:239)
	at java.util.ArrayList.ensureCapacityInternal(ArrayList.java:231)
	at java.util.ArrayList.add(ArrayList.java:462)
	at me.w1992wishes.jvm.HeapOOM.main(HeapOOM.java:22)
```

### 1.2、堆溢出异常分析

使用 **Eclipse Memory Analyzer**（Eclipse Memory Analyze 是 Java 堆转储文件分析工具，可以帮助发现内存漏洞和减少内存消耗）。

**第一步**，先打开 dump 文件：

![](https://ws1.sinaimg.cn/large/90c2c6e5gy1g3yqcmnbrwj20rz0l2tbb.jpg)

**第二步**，查看内存泄漏分析报表，可以非常直观看到内存泄露的可疑点：

![](https://ws1.sinaimg.cn/large/90c2c6e5gy1g3yqha88y1j20ny0ejjrt.jpg)

**第三步**，开始寻找导致内存泄漏的代码点。这时往往需要打开对象依赖关系树形视图，右键 Path To GC Roots->exclude weak reference(过滤到弱引用,弱引用不会阻止GC回收)：

```
几个概念说明:

Shallow Heap:对象本身占用的内存大小，不包含对其他对象的引用，也就是对象头加成员变量（不是成员变量的值）的总和。在32位系统上，对象头占用8字节，int占用4字节，不管成员变量（对象或数组）是否引用了其他对象（实例）或者赋值为null，它始终占用4字节。故此，对于String对象实例来说，它有三个int成员（3*4=12字节）、一个char[]成员（1*4=4字节）以及一个对象头（8字节），总共3*4 +1*4+8=24字节。

Retained Heap:对象本身以及它持有的所有对象的内存总和，加上从该对象能直接或间接访问到对象的shallow size之和。换句话说，retained size是该对象被GC之后所能回收到内存的总和。

System Class标签:由系统管理的对象,不会导致OOM,不用理会
```

![](https://ws1.sinaimg.cn/large/90c2c6e5gy1g3yr49knibj20ym0kitc9.jpg)

![1560351183049](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\1560351183049.png)

**第四步**，前面的分析还不是特别清晰，可以看看下面：

在垃圾回收机制中有一组元素被称为根元素集合，它们是一组被虚拟机直接引用的对象，比如，正在运行的线程对象，系统调用栈里面的对象以及被 system class loader 所加载的那些对象。堆空间中的每个对象都是由一个根元素为起点被层层调用的。因此，一个对象还被某一个存活的根元素所引用，就会被认为是存活对象，不能被回收，进行内存释放。因此，我们可以通过分析一个对象到根元素的引用路径来分析为什么该对象不能被顺利回收。如果说一个对象已经不被任何程序逻辑所需要但是还存在被根元素引用的情况，我们可以说这里存在内存泄露。

可以在下面的图示可以清楚的看到，这个对象集合中保存了大量 OOMObject 对象的引用，就是它导致的内存泄露。

![](https://ws1.sinaimg.cn/large/90c2c6e5gy1g3yrjk8hu8j20r70ct3z7.jpg)

## 二、JAVA虚拟机栈和本地方法栈溢出

在 Hotspot 中，不区分 JAVA 虚拟机栈和本地方法栈。在 JAVA 虚拟机规范中描述了两种与之相关的异常。

如果线程请求的栈深度超出了虚拟机允许的最大深度，将抛出 StackOverflowError异常：

```java
/**
 * VM args:-Xss128k
 * -Xss128k 减小栈内存至128k
 */
public class JavaVMStackSOF {
    private int stackLength = 1;

    public void stackLeak() {
        stackLength++;
        stackLeak();
    }

    public static void main(String[] args) {
        JavaVMStackSOF oom = new JavaVMStackSOF();
        try {
            oom.stackLeak();
        } catch (Throwable e) {
            System.out.println("stack length:" + oom.stackLength);
            throw e;
        }
    }
}
```

如果虚拟机在扩展栈时无法申请到足够的内存空间，则抛出OutOfMemoryError：

## 三、方法区和运行时常量池溢出

String.intern() 是一个 native 方法，它的作用是：如果字符串常量池中已经包含了一个等于此 String 对象的字符串，则返回代表池中这个字符串的 String 对象；否则，将此 String 对象包含的字符串添加到常量池中，并返回此 String 对象的引用。

```java
/**
 * VM args:-XX:PermSize=10M -XX:MaxPermSize=10M
 * PermSize 方法区大小
 */
public class RuntimeContantPoolOOM {
    public static void main(String[] args) {
        //使用List保存对常量池字符串的应用，避免Full GC回收常量池的行为
        List<String> list = new ArrayList<String>();
        //10M的PermSize在int的范围足够产生OutOfMemoryError
        int i = 0;
        while (true) {
            list.add(String.valueOf(i++).intern());
        }
    }
}
```

运行结果（JDK1.6 及之前的运行结果），在 jdk6 及之前，常量池分配在永久代内，通过-XX:PermSize10M -XX:MaxPermSize=10M限制其大小，而：

```
Exception in thread “main” java.lang.OutOfMemoryError: PermGen space
```

但是使用 JDK1.7运行这段程序不会得到相同的结果，这是因为这两个参数已经不在JDK1.7中使用了。在 JDK1.7 中常量池存储的不再是对象，而是对象引用，真正的对象是存储在堆中的。因此 while 循环将一直运行下去，但是，while 循环并不是始终运行下去，直到系统中堆内存用完为止，一般需要过好长时间才会出现。

借助 CGLib 使方法区出现内存溢出异常：

```java
/**
 * VM Args： -XX:PermSize=10M -XX:MaxPermSize=10M
 *
 * JDK 1.8 不再由持久代，类信息改为放在元空间 -XX:MetaspaceSize=10m -XX:MaxMetaspaceSize=10m
 */
public class JavaMethodAreaOOM {

    public static void main(String[] args) {
        while (true) {
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(OOMObject.class);
            enhancer.setUseCache(false);
            enhancer.setCallback((MethodInterceptor) (obj, method, arg, proxy) -> proxy.invokeSuper(obj, arg));
            enhancer.create();
        }
    }

    static class OOMObject {

    }
}
```

```
Exception in thread "main" java.lang.OutOfMemoryError: Metaspace
	at java.lang.Class.forName0(Native Method)
	at java.lang.Class.forName(Class.java:348)
	at net.sf.cglib.core.ReflectUtils.defineClass(ReflectUtils.java:467)
	at net.sf.cglib.core.AbstractClassGenerator.generate(AbstractClassGenerator.java:336)
	at net.sf.cglib.proxy.Enhancer.generate(Enhancer.java:492)
	at net.sf.cglib.core.AbstractClassGenerator$ClassLoaderData.get(AbstractClassGenerator.java:114)
	at net.sf.cglib.core.AbstractClassGenerator.create(AbstractClassGenerator.java:291)
	at net.sf.cglib.proxy.Enhancer.createHelper(Enhancer.java:480)
	at net.sf.cglib.proxy.Enhancer.create(Enhancer.java:305)
	at me.w1992wishes.jvm.JavaMethodAreaOOM.main(JavaMethodAreaOOM.java:19)
```

方法区溢出也是一种常见的内存溢出异常，一个类要被垃圾收集器回收掉，判定条件是比较苛刻的。在经常动态生成大量 Class 的应用中，需要特别注意类的回收状况。这类场景除了上面提到的程序使用了 CGLib 字节码增强和动态语言之外，常见的还有：大量 JSP 或动态产生 JSP 文件的应用（ JSP 第一次运行时需要编译为 Java类）、基于 OSGi 的应用（即使是同一个类文件，被不同的加载器加载也会视为不同的类）等。

## 四、本机直接内存溢出

虽然使用 DerictByteBuffer 分配内存也会抛出内存溢出异常，但它抛出异常时并没有真正向操作系统申请分配，而是通过计算得知内存无法分配，于是手动抛出异常，真正申请分配内存的方法是 unsafe.allocateMemory()。

```java
/**
 * VM Args：-Xmx20M -XX:MaxDirectMemorySize=10M
 *
 */
public class DirectMemoryOOM {
    private static final int _1MB = 1024 * 1024;

    public static void main(String[] args) throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredFields()[0];
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);
        while (true) {
            unsafe.allocateMemory(_1MB);
        }
    }
}

```