package me.w1992wishes.spark.mllib

import java.util
import java.util.Date

import com.beust.jcommander.JCommander
import me.w1992wishes.spark.mllib.dbscan.{DBSCANClustering, DataPoint}
import me.w1992wishes.spark.mllib.rough.RoughModel
import org.apache.spark.mllib.linalg.{Vector, Vectors}
import org.apache.spark.rdd.RDD
import org.apache.spark.{Partitioner, SparkConf, SparkContext}

import scala.collection.JavaConverters
import scala.collection.mutable.{ArrayBuffer, HashSet}
import scala.util.Random

object SardineProcessApp {
  val random = new Random(System.currentTimeMillis())

  def parseCommandArgs(args: Array[String]): ClusteringConfig = {
     var clusteringConfig = new ClusteringConfig
     val config = JCommander.newBuilder().acceptUnknownOptions(true).addObject(clusteringConfig).build()
     config.parse(args: _*)
     clusteringConfig
  }
  
  def randomArray(size: Int): Array[Double] = {
    var ra = new Array[Double](size)
    for(i <- 1 to ra.length - 1){
      ra(i) = random.nextDouble()
    }
    ra
  }

  def randomVector(dimension: Int):Vector = {
    var array = randomArray(dimension)
    var v = Vectors.dense(array)
    v
  }

  def arrayToVector(array: Array[Double]):Vector = {
    var v = Vectors.dense(array)
    v
  }

  def randomVector(rows: Int, dimension:Int):ArrayBuffer[(Int, Vector)] = {
    var set = new HashSet[Int]()
    for(i <- 0 to rows -1){
      set.add(i)
    }
    var seq = new ArrayBuffer[(Int, Vector)]()
    for(id <- set){
      var vector = randomVector(dimension)
      seq.append((id, vector))
    }
    seq
  }
  
  def randomRDD(seq: Seq[(Int, Vector)], sparkContext: SparkContext, partitions: Int):RDD[(Int, Vector)] = {
    var rdd = sparkContext.parallelize(seq, partitions)
    rdd
  }

    /***
   *  dbscan聚类
   */
  def dbscan(partId: Int, partData: Iterator[(Int, Vector)]):Iterator[(Int, Int, Vector)] = {
      val data = partData.toSeq
      val iddata: Seq[Int] = data.map(f => f._1)
      val vdata: Seq[DataPoint] = data.map(f => new DataPoint(f._2.toArray, String.valueOf(partId), false))
      val points: util.List[DataPoint] = JavaConverters.seqAsJavaListConverter(vdata).asJava

      val minPts = 1 
      val ePs = 0.03 
      val javaDBscan = new DBSCANClustering(ePs, minPts)
      javaDBscan.cluster(points)
      
      val presult = JavaConverters.asScalaIteratorConverter(points.iterator()).asScala
      val idpresult = iddata.zip(presult.toSeq)
      val result = idpresult.map(dp => (dp._2.getClusterId(), dp._1, arrayToVector(dp._2.getDimensioin()))).toIterator
      result
  }

  def main(args: Array[String]): Unit = {
    val clusteringConfig = parseCommandArgs(args)
    var dataNum = clusteringConfig.dataNum
    var dataDim = clusteringConfig.dataDim
    var dataPartitionCount = clusteringConfig.dataPartitionCount
    var kmeansk = clusteringConfig.kmeansk
    
    val conf = new SparkConf()
      .setAppName("SardineProcessApp")
      .setMaster("local[8]")
    val sc = new SparkContext(conf)

    // 1. prepare data sources
    var data = randomVector(dataNum, dataDim)
    val rdd: RDD[(Int, Vector)] = randomRDD(data, sc, dataPartitionCount)
    // RDD(vector)
    val vrdd: RDD[Vector] = rdd.map(fv => fv._2)

    // 2.prepare sample centers
    val centers : Array[Vector] = new RandomSampleStrategy().takeSamples(data.map(v => v._2), kmeansk)

    val start = System.currentTimeMillis()
    // 3. 将所有的点分到相应的样本中心组，得到 [index, Vector]，index 为样本点下标， Vector 为point
    val prdd : RDD[(Int, Vector)] = new RoughModel(centers).predict(vrdd).cache()
    prdd.count()
    val endPredict = System.currentTimeMillis()
    println("predict speed time " + (endPredict - start)/1000 + "s")

    // 4. 先得到所有粗分簇的id 数组(eg (1,4,6,9...))，不一定所有的样本点最后都会成分粗分簇的中心，可能部分样本点不是所有点的最近点
    // 使用 zipWithIndex 将可能不连续的 簇id 映射为 连续的 id
    var gidarray : Array[Int] = prdd.map(s => s._1).distinct().collect()
    var giddata: Map[Int, Int] = gidarray.zipWithIndex.toMap

    // 4. 重新分区，将 RDD[(Int, Vector)] index 相同的 Vector 分到一个区
    var pdata: RDD[(Int, Vector)] = prdd.partitionBy(new ZhaoMengPartitioner(giddata))
    println("partitions size:" + pdata.partitions.size)
    
    // 5. 分区后 clustering in partition
    var subclustereddata = pdata.mapPartitionsWithIndex(
        (partitionID, iterator) => {  
          val s = dbscan(partitionID, iterator)
          s
        } , false)
    var count = subclustereddata.count()
    println("partition dbscan result data count:" + count)
    println("partition and dbscan speed " + (System.currentTimeMillis() - endPredict)/1000 + "s")
    println("partition dbscan finish " + new Date())
     
    //suspend for view
    Thread.sleep(Long.MaxValue)
  }
  
  class ZhaoMengPartitioner(groupDic: Map[Int, Int]) extends Partitioner {

    override def numPartitions: Int = {
      groupDic.size
    }
  
    override def getPartition(key: Any): Int = {
      groupDic.get(key.asInstanceOf[Int]).get
    }
    
  }

}


