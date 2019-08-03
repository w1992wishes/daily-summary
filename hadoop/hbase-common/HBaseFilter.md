# 【HBase】HBase 过滤器

[TOC]

## 一、Filter 介绍

一般来说调整表设计就可以优化访问模式。但是有时已经把表设计调整得尽可能好了，为不同访问模式优化得尽可能好了。当仍然需要减少返回客户端的数据时，这就是考虑使用过滤器的时候了。

过滤器也被称为下推判断器（push-down predicates），支持把数据过滤标准从客户端下推到服务器，带有 Filter 条件的 RPC 查询请求会把 Filter 分发到各个 RegionServer，所有的**过滤器都在服务端生效**，使被过滤掉的数据不会被传送到客户端，这些过滤逻辑在读操作时使用，对返回客户端的数据有影响。这样可以降低网络传输的压力。

Filter 可以根据 簇、列、版本等更多的条件来对数据进行过滤，基于 HBase 本身提供的三维有序（主键有序、列有序、版本有序），这些 Filter 可以高效的完成查询过滤的任务。

HBase Filter 具有以下特点：

* Filter 是在 HBase 服务器端上执行判断操作；
* Filter 可以应用到行键（RowFilter），列限定符（QualifierFilter）或者数据值（ValueFilter）；
* Filter 允许对数据分页处理(PageFilter)，限制扫描器返回行数；
* FilterList 可以组合使用多个Filter。

## 二、Filter 方法执行顺序及含义

在 Filter 的源码中定义了 Filter 各方法的含义和执行顺序：

 A filter can expect the following call sequence:

* reset() : reset the filter state before filtering a new row.
* filterAllRemaining(): true means row scan is over; false means keep going.
* filterRowKey(Cell): true means drop this row; false means include.（常用）
* filterCell(Cell): decides whether to include or exclude this Cell.（常用）
* transformCell(Cell): if the Cell is included, let the filter transform the Cell.（基本不用）
* filterRowCells(List):allows direct modification of the final list to be submitted.提交前，修改要被过滤的 Cell 的机会。
* filterRow():last chance to drop entire row based on the sequence of    filter calls. Eg: filter a row if it doesn't contain a specified column.丢掉整行的最后机会。

过滤器执行流程大致如下：

![](https://ws1.sinaimg.cn/large/90c2c6e5gy1g5eh32v5xbj20bp0kxdgq.jpg)

1. 调用 filterRowKey 方法，进行行健级别的过滤，如果该行需要过滤，返回 true，不需要过滤返回 false；
2. 如果该行没有被 filterRowKey 过滤掉，接着调用 filterCell 方法筛选该行的每一个 Cell，这个方法返回一个 ReturnCode，返回的 ReturnCode 用于判断该 Cell 将要发生什么；
3.  在第 2 步过滤 Cell 对象后，filterRowCells 方法将处理被过滤的 Cell 列表，可以在该方法中对被过滤的 Cell 做一些转换或运算；
4. 再完成前面的动作后，filterRow 提供了最后的机会用于选择是否过滤该行；
5. filterAllRemaining 可以构造逻辑来提早停止一次扫描，例如在很多行、列中找到需要的值后，不再关心剩余的行，此方法将很有益处。

![](https://ws1.sinaimg.cn/large/90c2c6e5gy1g5ej1cenfij20sl08vaap.jpg)

## 三、自定义 Filter 

自定义一个查找密码长度小于指定值的过滤器，如下：

```java
public class PasswordStrengthFilter extends FilterBase {

    private int len;
    private boolean filterRow = false;

    // This flag is used to speed up seeking cells when matched column is found, such that following
    // columns in the same row can be skipped faster by NEXT_ROW instead of NEXT_COL.
    private boolean columnFound = false;
    
    public PasswordStrengthFilter(int len) {
        this.len = len;
    }

    @Override
    public ReturnCode filterCell(final Cell c) throws IOException {
        if (!CellUtil.matchingColumn(c, UsersDAO.INFO_FAM, UsersDAO.PASS_COL)) {
            return columnFound ? ReturnCode.NEXT_ROW : ReturnCode.NEXT_COL;
        }
        // Column found
        columnFound = true;
        if(c.getValueLength() >= len) {
            this.filterRow = true;
            return ReturnCode.SKIP;
        }
        return ReturnCode.INCLUDE;
    }

    @Override
    public boolean filterRow() {
        return this.filterRow;
    }

    @Override
    public void reset() {
        this.filterRow = false;
        columnFound = false;
    }

}
```

## 四、Filter 的实现

Filter 和 FilterList 作为一个通用的数据过滤框架，提供了一系列的接口，供用户来实现自定义的 Filter。当然，HBase 本身也提供了一系列的内置 Filter，例如：PrefixFilter、RowFilter、FamilyFilter、QualifierFilter、ValueFilter、ColumnPrefixFilter 等。

事实上，很多 Filter 都没有必要在服务端从 Scan 的 startRow 一直扫描到 endRow，中间有很多数据是可以根据 Filter 具体的语义直接跳过，通过减少磁盘 IO 和比较次数来实现更高的性能的。以 PrefixFilter(“333”) 为例，需要返回的是RowKey 以 “333” 为前缀的数据。

![](https://ws1.sinaimg.cn/large/90c2c6e5gy1g5ejoub203j21h10ljacq.jpg)

实际的扫描流程如图所示：

1) 碰到 RowKey=111 的行时，发现 111 比前缀 333 小，因此直接跳过 111 这一行去扫下一行，返回状态码 NEXT_ROW；
2) 下一个 Cell 的 RowKey =222，仍然比前缀 333 小，因此继续扫下一行，返回状态 NEXT_ROW；
3) 下一个 Cell 的 RowKey=333，前缀和 333 匹配，返回 column=f:ddd 这个 Cell 给用户，返回状态码为 INCLUDE；
4) 下一个 Cell 的 RowKey 仍为 333，前缀和 333 匹配，返回 column=f:eee 这个 Cell 给用户，返回状态码为 INCLUDE；
5) 下一个 Cell 的 RowKey 为 444，前缀比 333 大，不再继续扫描数据。

这个流程中，每碰到一个 Cell，返回的状态码 NEXT_ROW、INCLUDE 等，就告诉了RegionServer 扫描框架下一个 Cell 的位置。例如在第 2 步中，返回状态码 NEXT_ROW，那么下一个 Cell 的 RowKey 必须是比 222 大的，于是就跳过了 column=f:ccc 这个 Cell，直接定位到了 RowKey=333 的行，继续扫描数据。

在 Filter 中引入了 7 种状态码，见如下：

```java
  public enum ReturnCode {
    /**
     * Include the Cell
     */
    INCLUDE,
    /**
     * Include the Cell and seek to the next column skipping older versions.
     */
    INCLUDE_AND_NEXT_COL,
    /**
     * Skip this Cell
     */
    SKIP,
    /**
     * Skip this column. Go to the next column in this row.
     */
    NEXT_COL,
    /**
     * Seek to next row in current family. It may still pass a cell whose family is different but
     * row is the same as previous cell to {@link #filterCell(Cell)} , even if we get a NEXT_ROW
     * returned for previous cell. For more details see HBASE-18368. <br>
     * Once reset() method was invoked, then we switch to the next row for all family, and you can
     * catch the event by invoking CellUtils.matchingRows(previousCell, currentCell). <br>
     * Note that filterRow() will still be called. <br>
     */
    NEXT_ROW,
    /**
     * Seek to next key which is given as hint by the filter.
     */
    SEEK_NEXT_USING_HINT,
    /**
     * Include KeyValue and done with row, seek to next. See NEXT_ROW.
     */
    INCLUDE_AND_SEEK_NEXT_ROW,
}
```

当 ReturnCode 为 SEEK_NEXT_USING_HINT 时，需要在 Filter 的 getNextCellHint() 方法中告诉具体的 Cell。

## 五、HBase 内置 Filter 

要完成一个过滤的操作，需要两个参数。

**一个是抽象的操作符**，Hbase提供了枚举类型的变量来表示这些抽象的操作符：LESS/LESS_OR_EQUAL/EQUAL/NOT_EUQAL 等；

**另一个是具体的比较器（Comparator）**，代表具体的比较逻辑，比如字节级的比较、字符串级的比较等。

有了这两个参数，就可以清晰的定义筛选的条件，过滤数据。

**抽象操作符（比较运算符）**

```java
public enum CompareOperator {
  // Keeps same names as the enums over in filter's CompareOp intentionally.
  // The convertion of operator to protobuf representation is via a name comparison.
  /** less than */
  LESS,
  /** less than or equal to */
  LESS_OR_EQUAL,
  /** equals */
  EQUAL,
  /** not equal */
  NOT_EQUAL,
  /** greater than or equal to */
  GREATER_OR_EQUAL,
  /** greater than */
  GREATER,
  /** no operation */
  NO_OP,
}
```

**比较器（指定比较机制）**

* BinaryComparator 按字节索引顺序比较指定字节数组，采用 Bytes.compareTo(byte[])
* BinaryPrefixComparator 跟前面相同，只是比较左端的数据是否相同
* NullComparator 判断给定的是否为空
* BitComparator 按位比较
* RegexStringComparator 提供一个正则的比较器，仅支持 EQUAL 和非 EQUAL
* SubstringComparator 判断提供的子串是否出现在 value 中

HBase Filter 的可分为**比较过滤器和专用过滤器**。

### 5.1、比较过滤器

**行键过滤器 RowFilter**

```java
Filter rowFilter = new RowFilter(CompareOp.GREATER, new BinaryComparator("95007".getBytes()));
scan.setFilter(rowFilter);
```

筛选出匹配的所有的行，基于行键（RowKey）过滤数据，可以执行精确匹配，子字符串匹配或正则表达式匹配，过滤掉不匹配的数据。

一般来说，对 RowKey进行范围过滤，可以执行 Scan 的 startKey 和 endKey，RowFilter 可以更精确的过滤。

**列簇过滤器 FamilyFilter**

```java
Filter familyFilter = new FamilyFilter(CompareOp.EQUAL, new BinaryComparator("info".getBytes()));
scan.setFilter(familyFilter);
```

与 RowFilter 类似，区别是比较列族，而不是比较行键。当 HBase 表有多个列族时，可以用来筛选不同列族中的列。

**列过滤器 QualifierFilter**

```java
Filter qualifierFilter = new QualifierFilter(CompareOp.EQUAL, new BinaryComparator("name".getBytes()));
scan.setFilter(qualifierFilter);
```

根据列名进行筛选。

**值过滤器 ValueFilter**

```java
Filter valueFilter = new ValueFilter(CompareOp.EQUAL, new SubstringComparator("男"));
scan.setFilter(valueFilter);
```

筛选特定值的单元格，可以与 RegexStringComparator 搭配使用，完成复杂的筛选。

不同的比较器，只能与部分比较运算符搭配，例如 SubstringComparator 只能使用 `EQUAL` 或 `NOT_EQUAL`。

**时间戳过滤器 TimestampsFilter**

```java
List<Long> list = new ArrayList<>();
list.add(1522469029503l);
TimestampsFilter timestampsFilter = new TimestampsFilter(list);
scan.setFilter(timestampsFilter);
```

当需要在扫描结果中对版本进行细粒度控制时，可以使用这个 Filter 传入一个时间戳集合，对时间进行限制，只会返回与指定时间戳相同的版本数据，并且与设置时间戳范围共同使用。

### 5.2、专用过滤器

**单列值过滤器 SingleColumnValueFilter ----返回满足条件的整行**

```java
SingleColumnValueFilter singleColumnValueFilter = new SingleColumnValueFilter(
                "info".getBytes(), //列簇
                "name".getBytes(), //列
                CompareOp.EQUAL, 
                new SubstringComparator("刘晨"));
//如果不设置为 true，则那些不包含指定 column 的行也会返回
singleColumnValueFilter.setFilterIfMissing(true);
scan.setFilter(singleColumnValueFilter);
```

使用某一列的值，决定一行数据是否被过滤。

对于不包含指定列的行数据，通过 `setFilterIfMissing()` 决定是否返回。

**单列值排除器 SingleColumnValueExcludeFilter** 

```java
SingleColumnValueExcludeFilter singleColumnValueExcludeFilter = new SingleColumnValueExcludeFilter(
                "info".getBytes(), 
                "name".getBytes(), 
                CompareOp.EQUAL, 
                new SubstringComparator("刘晨"));
singleColumnValueExcludeFilter.setFilterIfMissing(true);
        
scan.setFilter(singleColumnValueExcludeFilter);
```

继承自 SingleColumnValueFilter，实现的与单列值过滤器相反的语义。

**前缀过滤器 PrefixFilter----针对行键**

```java
PrefixFilter prefixFilter = new PrefixFilter("9501".getBytes());
        
scan.setFilter(prefixFilter);
```

基于行键（Rowkey）的前缀过滤行数据。Scan 操作以字典序查找，当行键大于前缀时，Scan 结束。

**列前缀过滤器 ColumnPrefixFilter**

```java
ColumnPrefixFilter columnPrefixFilter = new ColumnPrefixFilter("name".getBytes());
        
scan.setFilter(columnPrefixFilter);
```

通过对列名称的前缀匹配过滤，返回的结果只包含满足过滤器的列。

**分页过滤器 PageFilter**

```java
byte[] lastRow = null; 
Filter filter = new PageFilter(10); 

while(true) { 
    int rowCount = 0; 
    Scan scan = new Scan(); 
    scan.setFilter(filter); 
    scan.setStartRow(lastRow); 

    ResultScanner resultScanner = table.getScanner(scan); 
    Iterator<Result> resultIterator = resultScanner.iterator(); 
    while (resultIterator.hasNext()) { 
        Result result = resultIterator.next(); 
        // ... 
        lastRow = result.getRow(); // 记录最后一行的rowkey 
        rowCount++; // 记录本页行数 
    } 

    if(rowCount <= 10) { 
        break; 
    } 
}
```

使用该过滤器，对结果进行按行分页，需要指定 pageSize 参数，控制每页返回的行数，并设置 startRow 多次调用 getScanner()，感觉功能只适用于一些特殊场景，性能也并不高。

### 5.3、多种过滤条件使用

通过 FilterList 实例可以提供多个过滤器共同使用的功能。并且可以指定对多个过滤器的过滤结果如何组合。

```java
Filter filter1 = new ...; 
Filter filter2 = new ...; 
Filter filter3 = new ...; 

FilterList filterList = new FilterList(Arrays.asList(filter1, filter2, filter3)); 

Scan scan = new Scan(); 
scan.setFilter(filterList);
```

## 六、参考博文

1.[漫谈HBase Filter](http://openinx.github.io/2019/07/02/blog-for-filter-list/)
2.[HBase之过滤器](https://www.bbsmax.com/A/rV57EKoRzP/)

