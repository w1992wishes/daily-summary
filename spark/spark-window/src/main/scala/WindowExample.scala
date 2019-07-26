import java.time.LocalDateTime

import com.arangodb.ArangoDB
import me.w1992wishes.common.util.DateUtils
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions.{collect_set, unix_timestamp}

import scala.collection.mutable.ListBuffer
import scala.math.min

/**
  * @author w1992wishes 2019/7/10 20:13
  */
object WindowExample {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .master("local[*]")
      .appName("example")
      .getOrCreate()

    val events = spark.read
      .format("jdbc")
      .option("partitionColumn", "aid")
      .option("lowerBound", 0L)
      .option("upperBound", 1000000L)
      .option("numPartitions", 100)
      .option("fetchsize", 5000)
      .option("driver", "")
      .option("url", "")
      .option("dbtable", "table")
      .option("user", "")
      .option("password", "")
      .load()

    import spark.implicits._

    val win = Window.partitionBy("source_id").orderBy(unix_timestamp($"time")).rangeBetween(0, 10)
    val df = events.withColumn("peers", collect_set('aid) over win)

    val df1 = df.flatMap(row => {
      val list = new ListBuffer[(String, Int)]()
      val sAid = row.getString(0)
      val date = row.getTimestamp(1).toString.substring(0,10).replace("-", "")
      val aids = row.getSeq[String](3).foreach(aid =>{
        if(!aid.equals(sAid)){
          val key: String  = if(sAid < aid)  sAid + "_" + aid + "_" + date else aid + "_" + sAid + "_" + date
          list.append((key, 1))
        }
      })
      list
    })

    val df2 = df1.groupByKey(_._1).count()
      .map(row => {
        val splits = row._1.split("_")
        (splits(0) + "_" + splits(1), "t" + splits(2) + ":" + row._2)
      })

    val df3 = df2.groupByKey(_._1).mapValues(row => row._2).mapGroups((k, i) => (k, i.toSeq))


    df3.foreachPartition(rows => {
      val now = DateUtils.dateTimeToStr(LocalDateTime.now())
      val arangoDB = new ArangoDB.Builder()
        .host("host", 11)
        .user("")
        .password("")
        .build


      val list = rows.map(row => {
        val splits = row._1.split("_")
        val peers = row._2.foldLeft("{")((s, e) => s + e +  ",").dropRight(1) + "}"
        s"{from:'${splits(0)}',to:'${splits(0)}',key: '${row._1}', peers:${peers},direction:'${"ANY"}'" +
          s",weight:'${0}',currentTime:'${now}',label:'${"peer"}'}"
      }).toList

      for(i <- 0 to list.length / 10000){
        val elements =  list.slice(i*10000, min((i+1)*10000, list.length)).mkString(",")

        val aql = s"FOR e IN [$elements]\n " +
          s"LET kys = ATTRIBUTES(e.peers)\n " +
          s"LET sum = SUM(VALUES(e.peers))\n " +
          s"LET sortedDate = sorted(kys) \n" +
          s"LET startdate = substring(sortedDate[0] ,1, 9) \n" +
          s"LET enddate = substring(sortedDate[-1] ,1, 9) \n" +
          s"LET oldPeers = Document(${"relation_assemble_core"}, e.key).peers \n " +
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
          s"   endTime: enddate > OLD.endTime ? enddate : OLD.endTime, \n"  +
          s"   total: OLD.total - OLD.peers[kys[0]] - OLD.peers[kys[1]] + sum,\n " +
          s"   peers: e.peers\n " +
          s"  }    \n " +
          s"INTO ${"relation_assemble_core"} \n" +
          s"OPTIONS { waitForSync:true,ignoreRevs:false,ignoreErrors:true,exclusive:true }"
        //        println(aql)
        arangoDB.db("db").query(aql,null, null, null)
      }
      arangoDB.shutdown()

    })
    spark.stop()
  }

}
