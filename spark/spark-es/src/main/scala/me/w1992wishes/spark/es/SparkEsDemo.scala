package me.w1992wishes.spark.es

import org.apache.spark.SparkConf
import org.apache.spark.sql.{SaveMode, SparkSession}
import org.elasticsearch.hadoop.cfg.ConfigurationOptions

/**
  * @author w1992wishes 2020/6/5 16:56
  */
object SparkEsDemo {

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf()
      .setIfMissing("spark.master", "local[5]")
      // es.nodes/es.prot: 这里比较简单，就是es的节点列表和端口号
      .set(ConfigurationOptions.ES_NODES, "192.168.13.82")
      .set(ConfigurationOptions.ES_PORT, "9200")
      // es.nodes.wan.only: 这里是表示使用的 es 节点 ip 是否是一个云环境中的 ip，不允许使用节点嗅探探查真实的节点 ip。适用于类似于腾讯云或AWS的ES云服务。
      .set(ConfigurationOptions.ES_NODES_WAN_ONLY, "true")
      // true or false，是否自动创建index
      .set(ConfigurationOptions.ES_INDEX_AUTO_CREATE, "true")
      .set(ConfigurationOptions.ES_NODES_DISCOVERY, "false")
      // 存在则更新，不存在则进行插入
      .set(ConfigurationOptions.ES_WRITE_OPERATION, "upsert")
      //.set(ConfigurationOptions.ES_NET_HTTP_AUTH_USER, esUser)
      //.set(ConfigurationOptions.ES_NET_HTTP_AUTH_PASS, esPwd)
      //.set("es.write.rest.error.handlers", "ignoreConflict")
      //.set("es.write.rest.error.handler.ignoreConflict", "com.jointsky.bigdata.handler.IgnoreConflictsHandler")
      // es会为每个文档分配一个全局id。如果不指定此参数将随机生成；如果指定的话按指定的来
      .set("es.mapping.id", "camera_id")
      // 这两个参数可以控制单次批量写入的数据量大小和条数，数据积累量先达到哪个参数设置，都会触发一次批量写入。
      // 增大单次批量写入的数据，可以提高写入ES的整体吞吐。
      // 因为ES的写入一般是顺序写入，在一次批量写入中，很多数据的写入处理逻辑可以合并，大量的IO操作也可以合并。
      // 默认值设置的比较小，可以适当根据集群的规模调大这两个值，建议为20MB和2w条。
      // 当然，bulk size不能无限的增大，会造成写入任务的积压。
      //.set("es.batch.size.bytes", "30MB")
      //.set("es.batch.size.entries", "20000")
      //es.batch.write.refresh: ES是一个准实时的搜索引擎，意味着当写入数据之后，只有当触发refresh操作后，写入的数据才能被搜索到。
      // 这里的参数是控制，是否每次bulk操作后都进行refresh。
      // 每次refresh后，ES会将当前内存中的数据生成一个新的segment。
      // 如果refresh速度过快，会产生大量的小segment，大量segment在进行合并时，会消耗磁盘的IO。
      // 默认值为开启，我们这里建议设置为false。在索引的settings中通过refresh_interval配置项进行控制，可以根据业务的需求设置为30s或更长。
      .set("es.batch.write.refresh", "false")
      // es.batch.write.retry.count/es.batch.write.retry.wait: 这两个参数会控制单次批量写入请求的重试次数，以及重试间隔。
      // 当超过重试次数后，Yarn任务管理会将该任务标记为failed，造成整个写数据任务的失败。
      // 默认值为3，为了防止集群偶发的网络抖动或压力过大造成的集群短暂熔断，建议将这个值调大，设置为50。
      .set("es.batch.write.retry.count", "50")
      .set("es.batch.write.retry.wait", "500s")
      // es.http.timeout/es.http.retries: 这两个参数是控制http接口层面的超时及重试，覆盖读请求和写请求，和上面一样，默认值比较小。
      // 默认超时时间为1分钟，重试次数为3，建议调整为超时时间5分钟，重试次数50次。
      .set("es.http.timeout", "5m")
      .set("es.http.retries", "50")
      // es.action.heart.beat.lead: 这个参数是控制任务超时之后多久才通知Yarn调度模块重试整个任务。大部分的写入场景，我们都不会设置写入任务的重试。
      .set("es.action.heart.beat.lead", "50s")

    val spark = SparkSession.builder()
      .appName(getClass.getSimpleName)
      .config(conf)
      .enableHiveSupport()
      .getOrCreate()

    import spark.sql

    sql("SELECT * FROM bigdata_dim.dim_bigdata_event_camera")
      .write
      .format("org.elasticsearch.spark.sql")
      .mode(SaveMode.Append)
      .save("event/camera")

/*    PUT /event
    {
      "mappings": {
        "camera": {
        "properties": {
        "camera_id": {
        "type": "keyword"
      },
        "name": {
        "type": "keyword"
      },
        "ip": {
        "type": "ip"
      },
        "lat": {
        "type": "double",
        "index": false
      },
        "lon": {
        "type": "double",
        "index": false
      }
      }
      }
      }
    }*/
  }

}
