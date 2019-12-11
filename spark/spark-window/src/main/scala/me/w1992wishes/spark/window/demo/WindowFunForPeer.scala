package me.w1992wishes.spark.window.demo

import java.time.LocalDateTime

import com.arangodb.ArangoDB
import me.w1992wishes.common.util.DateUtil
import me.w1992wishes.spark.udf.ComputePeerNum
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions.unix_timestamp

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.math.min

/**
  * @author w1992wishes 2019/7/10 20:13
  */
object WindowFunForPeer {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .master("local[*]")
      .appName("example")
      .getOrCreate()


    val partitions = 200
    // 并行加载数据
    val archives = Range(0, partitions)
      .map(index => {
        val sql = s"SELECT aid,time,source_id FROM t_archive WHERE cast(aid as BIGINT) % $partitions = $index"
        println(s"======> query archive sql -- $sql")
        spark
          .read
          .format("jdbc")
          .option("driver", "")
          .option("url", "")
          .option("dbtable", s"($sql) AS t_tmp_$index")
          .option("user", "")
          .option("password", "")
          .load()
      }).reduce((ds1, ds2) => ds1.union(ds2))

    import spark.implicits._
    val win = Window.partitionBy("source_id").orderBy(unix_timestamp($"time")).rangeBetween(-10, 10)

    val df = archives.withColumn("peers", ComputePeerNum($"aid", $"time").over(win))

    // aid,time,source_id,peers(Array[aid_time])
    val df1 = df.flatMap(row => {
      // aid_aid_time   eg 103984_993895_20190813 23:23:00
      val result = new ArrayBuffer[String]()

      val aid = row.getString(0)
      val list = new ListBuffer[(String, String)]()

      row.getSeq[String](3).foreach(peer => {
        val splits = peer.split("_")
        list.append((splits(0), splits(1)))
      })

      val tuples = list.sortBy(_._1)

      // aid -> time
      val map = new mutable.HashMap[String, String]
      tuples.foreach(row => map += row)

      for ((k, v) <- map) {
        val key: String = if (aid < k) aid + "_" + k + "_" + v else k + "_" + aid + "_" + v
        result.append(key)
      }

      result
    }).distinct()


    val df2 = df1.map(row => {
      val splits = row.split(" ")
      // (103984_993895_20190813, 23:23:00)
      (splits(0), 1)
    })
      .groupByKey(_._1).count()
      .map(row => {
        // (103984_993895_20190813)
        val splits = row._1.split("_")
        // (103984_993895) t20190813:3)
        (splits(0) + "_" + splits(1), "t" + splits(2).replace("-", "") + ":" + row._2)
      })

    val sinkArangoEdge = "archive_edge_core"
    val sinkArangoDB = "bigdata_archive_graph"
    val df3 = df2.groupByKey(_._1).mapValues(row => row._2).mapGroups((k, i) => (k, i.toSeq))
    df3.foreachPartition(rows => {
      val now = DateUtil.dateTimeToStr(LocalDateTime.now())
      val arangoDB = new ArangoDB.Builder()
        .host("", 999)
        .user("")
        .password("")
        .build

      val list = rows.map(row => {
        val splits = row._1.split("_")
        val peers = row._2.foldLeft("{")((s, e) => s + e + ",").dropRight(1) + "}"
        s"{from:'${splits(0)}',to:'${splits(1)}',key: '${row._1}', peers:$peers,direction:'${Direction.ANY}'" +
          s",weight:'$WEIGHT_0',currentTime:'$now',label:'${Label.PEER}'}"
      }).toList

      for (i <- 0 to list.length / 2000) {
        val elements = list.slice(i * 2000, min((i + 1) * 2000, list.length)).mkString(",")

        val aql = s"FOR e IN [$elements]\n " +
          s"LET kys = ATTRIBUTES(e.peers)\n " +
          s"LET sum = SUM(VALUES(e.peers))\n " +
          s"LET sortedDate = sorted(kys) \n" +
          s"LET startdate = substring(sortedDate[0] ,1, 9) \n" +
          s"LET enddate = substring(sortedDate[-1] ,1, 9) \n" +
          s"LET oldPeers = Document($sinkArangoEdge, e.key).peers \n " +
          s"LEt oldSum = SUM(for ky in kys return oldPeers[ky]) \n " +
          s"UPSERT {_key : e.key}\n " +
          s"INSERT {\n  " +
          s"      _key: e.key,\n  " +
          s"      _from: e.from, \n  " +
          s"      fromId: e.from, \n  " +
          s"      _to: e.to, \n  " +
          s"      targetId: e.to, \n  " +
          s"      direction: e.direction,\n  " +
          s"      weight: e.weight, \n  " +
          s"      total: sum,\n  " +
          s"      createTime: e.currentTime, \n  " +
          s"      modifyTime: e.currentTime, \n  " +
          s"      label: e.label, \n  " +
          s"      startTime: startdate, \n  " +
          s"      endTime: enddate, \n  " +
          s"      peers: e.peers \n  " +
          s"  } \n " +
          s"UPDATE { \n  " +
          s"   modifyTime: e.currentTime , \n" +
          s"   startTime: startdate > OLD.startTime ? OLD.startTime : startdate,  \n" +
          s"   endTime: enddate > OLD.endTime ? enddate : OLD.endTime, \n" +
          s"   total: OLD.total - OLD.peers[kys[0]] - OLD.peers[kys[1]] + sum,\n " +
          s"   peers: e.peers\n " +
          s"  }    \n " +
          s"IN $sinkArangoEdge \n" +
          s"OPTIONS { waitForSync:true,ignoreRevs:false,ignoreErrors:true,exclusive:true }"

        println(aql)
        arangoDB.db(sinkArangoDB).query(aql, null, null, null)
      }
      arangoDB.shutdown()

    })
    spark.stop()
  }

  /**
    * 方向
    */
  object Direction {
    val SINGLE = "SINGLE"
    val BOTH = "BOTH"
    val ANY = "ANY"
  }

  val WEIGHT_0 = "0"

  /**
    * 标签
    */
  object Label {
    val PEER = "PEER"
    val PERSON = "PERSON"
  }

}

