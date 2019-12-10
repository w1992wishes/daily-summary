import org.apache.spark.rdd.RDD
import org.apache.spark.sql.expressions.{MutableAggregationBuffer, UserDefinedAggregateFunction, Window}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.{Row, SparkSession}


object ComputePeerNum extends UserDefinedAggregateFunction {

  def inputSchema = StructType(StructField("aid", StringType) :: StructField("time", StringType) :: Nil)

  def bufferSchema: StructType = StructType(StructField("aid", StringType) :: StructField("time", StringType) :: StructField("merge", ArrayType(StringType, containsNull = true), nullable = true) :: Nil)

  override def dataType: DataType = ArrayType(StringType, containsNull = true)

  def deterministic = true

  def initialize(buffer: MutableAggregationBuffer): Unit = {
    buffer(0) = null
    buffer(1) = null
    buffer(2) = Seq[String]()
  }

//  def update(buffer: MutableAggregationBuffer, input: Row) = {
//    if (!input.isNullAt(0)) {
//      if (buffer(0) == null) {
//        buffer.update(0, input.getString(0))
//        buffer.update(1, input.getString(1))
//
//        buffer(2) = input.getString(0) + "_" + input.getString(1) :: Nil
//      } else {
//        val name = input.getString(0)
//        val time = input.getString(1)
//        var e = name + "_" + time
//
//        val arr: Seq[String] = buffer.getAs[Seq[String]](2)
//        val arr1 = for(elem <- arr if !elem.startsWith(name)) yield elem
//        buffer(2) = arr1 :+ e
//      }
//    }
//  }


  def update(buffer: MutableAggregationBuffer, input: Row): Unit = {
    if (!input.isNullAt(0)) {
      val name = input.getString(0)
      val time = input.getString(1)
      var e = name + "_" + time
      val arr: Seq[String] = buffer.getAs[Seq[String]](2)
      buffer(2) = arr :+ e
    }
  }

  def merge(buffer1: MutableAggregationBuffer, buffer2: Row): Unit = {
    buffer1(2) = buffer1.getAs[Seq[String]](2) ++ buffer2.getAs[Seq[String]](2)
  }

  def evaluate(buffer: Row): Seq[String] = {
    buffer.getAs[Seq[String]](2)
  }

  def analyze[T](r: RDD[T]): Unit ={
    val partitions = r.glom()
    println(partitions.count() + " partitions")

    // use zipWithIndex() to see the index of each partition
    // we need to loop sequentially so we can see them in order: use collect()
    partitions.zipWithIndex().collect().foreach {
      case (a, i) =>
        println("Partition " + i + " contents:" +  a.foldLeft("")((e, s) => e + " " + s))
    }

  }

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .master("local")
      .appName("YR")
      .config("spark.default.parallelism", 12)
      .getOrCreate()

    import spark.implicits._




    val df = spark.sparkContext.parallelize(
      Seq(("4612375779706142782", "2019-03-23 15:05:32", 1),
        ("4612375779706142782", "2019-03-23 15:05:33", 1),
        ("4612375780511449144", "2019-03-23 15:05:33", 1),
        ("4612375780511449144", "2019-03-23 15:05:34", 1),

        ("4612375778632400915", "2019-03-23 15:05:35", 1),
        ("4612375778632400915", "2019-03-23 15:05:36", 1),

        ("4612375779169271823", "2019-03-23 15:05:37", 1),
        ("4612375779169271823", "2019-03-23 15:05:38", 1))).toDF("aid", "time", "source_id")

    val win = Window.partitionBy("source_id").orderBy(unix_timestamp($"time")).rangeBetween(0,100)

//    val df1 = df.select($"aid", unix_timestamp($"time").as("timestamp"), $"source_id")

//    val z = ComputePeerNum($"aid", $"time").over(win).as("peers")

    val df1 = df.withColumn("Peers", ComputePeerNum($"aid", $"time").over(win))

    analyze(df1.rdd)



  }
}