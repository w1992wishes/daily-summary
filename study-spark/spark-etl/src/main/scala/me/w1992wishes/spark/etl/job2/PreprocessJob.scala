package me.w1992wishes.spark.etl.job2

import java.sql.{Connection, PreparedStatement, ResultSet, Timestamp}
import java.time.LocalDateTime

import com.alibaba.fastjson.JSON
import com.typesafe.scalalogging.Logger
import me.w1992wishes.spark.etl.util._
import org.apache.commons.lang3.StringUtils
import org.apache.spark.TaskContext
import org.apache.spark.sql.{Row, SparkSession}

import scala.collection.mutable.ArrayBuffer

/**
  * 对增量数据做一些清洗、过滤、转化等预处理
  *
  * @author w1992wishes 2018/10/16 11:21
  */
object PreprocessJob {

  private val LOG = Logger(this.getClass)

  private val configArgs = new ConfigArgs

  def main(args: Array[String]): Unit = {

    // parse args
    val appArgs: AppArgs = new AppArgs(args)

    val beginTime = System.currentTimeMillis()
    // 计算开始结束 time
    val timeSequence: (String, String) = {
      // 当前日期的前一天的 0时0分0秒
      val startTime = LocalDateTime.now().minusDays(1).withHour(0).withMinute(0).withSecond(0)
      val startTimeStr = if (StringUtils.isEmpty(appArgs.preprocessStartTime)) DateUtils.dateTimeToStr(startTime) else appArgs.preprocessStartTime
      // 当前日期的 0时0分0秒
      val endTime = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0)
      val endTimeStr = if (StringUtils.isEmpty(appArgs.preprocessEndTime)) DateUtils.dateTimeToStr(endTime) else appArgs.preprocessEndTime
      (startTimeStr, endTimeStr)
    }
    // 计算开始结束 sequence
    val sequence = calculateSequenceRange(timeSequence._1, timeSequence._2)

    if (sequence._2 > sequence._1) {
      // 先清除数据，防止重复插入
      clearDatas(sequence)

      // spark
      val spark = SparkSession
        .builder()
        //.master("local")
        .appName(configArgs.sparkAppName)
        .getOrCreate()

      // 为了不丢失数据，向上取整，将数据分成 numPartitions 份
      val stride = (sequence._2 - sequence._1) / appArgs.partitions + 1
      println("======> total partitions: " + appArgs.partitions + ", stride is: " + stride)

      Range(0, appArgs.partitions)
        .map(index => {
          spark
            .read
            .format("jdbc")
            .option("driver", configArgs.gpDriver)
            .option("url", configArgs.gpUrl)
            .option("dbtable", s"(SELECT * FROM ${configArgs.sourceTable} WHERE id >= ${stride * index + sequence._1} AND id < ${Math.min(stride * (index + 1) + sequence._1, sequence._2)}) AS t_tmp_$index")
            .option("user", configArgs.gpUser)
            .option("password", configArgs.gpPasswd)
            .option("fetchsize", configArgs.gpFetchsize)
            .load()
        })
        .reduce((rdd1, rdd2) => rdd1.union(rdd2))
        .foreachPartition(partitionIter => partitionFunc(partitionIter))

      LOG.info("======> PreprocessJob speed time {}s", (System.currentTimeMillis() - beginTime) / 1000)

      spark.stop()
    } else {
      LOG.info("======> the sequence range is (0, 0), not need to continue.")
    }
  }

  /**
    * 计算加载数据的自增 id 区间，默认加载前一天的最小 id 到最大 id
    *
    * @return
    */
  private def calculateSequenceRange(startTimeStr: String, endTimeStr: String): (Long, Long) = {
    var conn: Connection = null
    var pstmt: PreparedStatement = null
    var rs: ResultSet = null
    try {
      conn = ConnectionUtils.getConnection(configArgs.gpUrl, configArgs.gpUser, configArgs.gpPasswd)

      val sql = s"select min(id) startSequence, max(id) endSequence from ${configArgs.sourceTable} where create_time >= '$startTimeStr' and create_time < '$endTimeStr'"

      println("======> query sequence range sql -- " + sql)

      pstmt = conn.prepareStatement(sql)
      rs = pstmt.executeQuery()

      var startSequence: Long = 0L
      var endSequence: Long = 0L
      if (rs.next()) {
        startSequence = rs.getLong("startSequence")
        endSequence = rs.getLong("endSequence")
      }
      println("======> start  sequence : " + startSequence + ", end sequence : " + endSequence)
      (startSequence, endSequence)
    } finally {
      ConnectionUtils.closeResource(conn, pstmt, rs)
    }
  }

  /**
    * 预处理后的实体
    *
    * @param id             唯一标识 id
    * @param faceId         小图唯一标识
    * @param imageData      小图 url
    * @param faceFeature    特征值
    * @param fromImageId    大图唯一标识
    * @param gender         性别
    * @param accessories    穿戴
    * @param age            年龄
    * @param isRealAge      是否是真实年龄
    * @param json           josn
    * @param quality        质量
    * @param race           族别
    * @param sourceId       采集源id
    * @param sourceType     采集源类型
    * @param locus          地点
    * @param time           抓拍时间
    * @param version        特征值版本
    * @param pose           角度
    * @param featureQuality 特征值质量
    * @param tableSequence  源主键 id
    */
  case class PreprocessData(id: Long, faceId: Long, imageData: String, faceFeature: Array[Byte], fromImageId: Long, gender: Int,
                            accessories: Int, isRealAge: Boolean, age: Int, json: String, quality: Float, race: Int, sourceId: Long, sourceType: Int,
                            locus: String, time: Timestamp, version: Int, pose: String, featureQuality: Float, tableSequence: Long)

  /**
    * 分区执行批量插入
    *
    * @param iterator 分区迭代器
    */
  private def partitionFunc(iterator: Iterator[Row]): Unit = {

    val tuple = structurePreprocessList(iterator)
    LOG.info(s"======> partition ${TaskContext.getPartitionId()} has " +
      s"${tuple._1.length} cluster data, " +
      s"${tuple._2.length} class data, " +
      s"${tuple._3.length} unless data, " +
      s"${tuple._4.length} child data")

    var conn: Connection = null
    try {
      conn = ConnectionUtils.getConnection(configArgs.gpUrl, configArgs.gpUser, configArgs.gpPasswd)
      conn.setAutoCommit(false)
      batchInsertFunc(tuple._1, conn, configArgs.clusterTable)
      batchInsertFunc(tuple._2, conn, configArgs.classTable)
      batchInsertFunc(tuple._3, conn, configArgs.unlessTable)
      batchInsertFunc(tuple._4, conn, configArgs.childTable)
      conn.commit()
    } catch {
      case e: Exception =>
        LOG.error("======> insert data failure", e)
        if (conn != null) {
          try {
            conn.rollback()
          } catch {
            case e: Exception => LOG.error("======> rollback failure", e)
          }
        }
        throw e
    } finally {
      ConnectionUtils.closeConnection(conn)
    }
  }

  /**
    * 构建归档数据集合，建档数据集合
    *
    * @param iterator 迭代器
    * @return
    */
  private def structurePreprocessList(iterator: Iterator[Row]): (ArrayBuffer[PreprocessData], ArrayBuffer[PreprocessData], ArrayBuffer[PreprocessData], ArrayBuffer[PreprocessData]) = {
    // 用于生产唯一 id
    val idWorker = new SnowflakeIdWorker(TaskContext.getPartitionId / SnowflakeIdWorker.MAX_FLAG,
      TaskContext.getPartitionId % SnowflakeIdWorker.MAX_FLAG)
    // 可建档
    val clusterPreprocessDatas = new ArrayBuffer[PreprocessData]()
    // 可归档不可建档
    val classPreprocessDatas = new ArrayBuffer[PreprocessData]()
    // 不可归档不可建档
    val unlessPreprocessDatas = new ArrayBuffer[PreprocessData]()
    // 小孩
    val childPreprocessDatas = new ArrayBuffer[PreprocessData]()
    while (iterator.hasNext) {
      val row = iterator.next()
      val id = idWorker.nextId()
      val faceId = row.getAs[Long]("face_id")
      val imageData = row.getAs[String]("image_data")
      val faceFeature = row.getAs[Array[Byte]]("face_feature")
      val fromImageId = row.getAs[Long]("from_image_id")
      val gender = row.getAs[Int]("gender")
      val accessories = row.getAs[Int]("accessories")
      val (isRealAge, age) = AgeUtils.parseRealAge(row.getAs[Int]("age"))
      val json = row.getAs[String]("json")
      val quality: Float = row.getAs[Double]("quality").toFloat
      val race = row.getAs[Int]("race")
      val sourceId = row.getAs[Long]("source_id")
      val sourceType = row.getAs[Int]("source_type")
      val locus = row.getAs[String]("locus")
      val time = row.getAs[Timestamp]("time")
      val version = row.getAs[Int]("version")
      val pose = row.getAs[String]("pose")
      val featureQuality = getFeatureQuality(faceFeature, pose, quality, time)
      val tableSequence = row.getAs[Long]("id")
      // 构建 PreprocessData 实体
      val preprocessData = PreprocessData(id, faceId, imageData, faceFeature, fromImageId, gender, accessories, isRealAge, age, json,
        quality, race, sourceId, sourceType, locus, time, version, pose, featureQuality, tableSequence)

      // 年龄过滤
      val ageFilter = (preprocessData.isRealAge && preprocessData.age > configArgs.realAgeThreshold) ||
        (!preprocessData.isRealAge && preprocessData.age > configArgs.ageStageThreshold)
      // 建档特征值质量过滤
      val clusterFeatureFilter = preprocessData.featureQuality.compareTo(Predef.float2Float(0.0f)) > 0
      // 归档特征值质量过滤
      val classFeatureFilter = preprocessData.featureQuality.compareTo(Predef.float2Float(-1.0f)) >= 0
      // featureQuality 大于 0 表示可建档
      if (ageFilter) {
        if (clusterFeatureFilter) {
          clusterPreprocessDatas.append(preprocessData)
        } else if (classFeatureFilter) {
          classPreprocessDatas.append(preprocessData)
        } else {
          unlessPreprocessDatas.append(preprocessData)
        }
      } else {
        childPreprocessDatas.append(preprocessData)
      }
    }
    (clusterPreprocessDatas, classPreprocessDatas, unlessPreprocessDatas, childPreprocessDatas)
  }

  def batchInsertFunc(preprocessDatas: ArrayBuffer[PreprocessData], conn: Connection, table: String): Unit = {
    if (preprocessDatas.isEmpty) {
      return
    }
    var pstmt: PreparedStatement = null
    try {
      val sql = s"insert into $table(id, face_id, image_data, face_feature, from_image_id, " +
        s"gender, accessories, age, json, quality, race, source_id, source_type, locus, time, version, " +
        s"pose, feature_quality, table_sequence) " +
        s"values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
      var sum = 0
      var i = 0
      pstmt = conn.prepareStatement(sql)
      for (index <- preprocessDatas.indices) {
        i += 1
        sum += 1
        if (i > configArgs.gpBatchsize) {
          i = 0
          pstmt.executeBatch()
          pstmt.clearBatch()
        }
        val preprocessData = preprocessDatas(index)
        pstmt.setLong(1, preprocessData.id)
        pstmt.setLong(2, preprocessData.faceId)
        pstmt.setString(3, preprocessData.imageData)
        pstmt.setBytes(4, preprocessData.faceFeature)
        pstmt.setLong(5, preprocessData.fromImageId)
        pstmt.setInt(6, preprocessData.gender)
        pstmt.setInt(7, preprocessData.accessories)
        pstmt.setInt(8, preprocessData.age)
        pstmt.setString(9, preprocessData.json)
        pstmt.setFloat(10, preprocessData.quality)
        pstmt.setInt(11, preprocessData.race)
        pstmt.setLong(12, preprocessData.sourceId)
        pstmt.setInt(13, preprocessData.sourceType)
        pstmt.setString(14, preprocessData.locus)
        pstmt.setTimestamp(15, preprocessData.time)
        pstmt.setInt(16, preprocessData.version)
        pstmt.setString(17, preprocessData.pose)
        pstmt.setFloat(18, preprocessData.featureQuality)
        pstmt.setLong(19, preprocessData.tableSequence)
        pstmt.addBatch()
      }
      pstmt.executeBatch()
    } finally {
      ConnectionUtils.closeStatement(pstmt)
    }
  }

  private def getFeatureQuality(faceFeature: Array[Byte], pose: String, quality: Float, time: Timestamp): Float = {

    /**
      * 计算可建档图片角度权重
      *
      * @param facePose 角度
      * @return
      */
    def calculatePoseWeight(facePose: XPose): Float = {
      1 - (Math.abs(facePose.getPitch) / configArgs.clusterPitchThreshold
        + Math.abs(facePose.getRoll) / configArgs.clusterRollThreshold
        + Math.abs(facePose.getYaw) / configArgs.clusterYawThreshold) / 3
    }

    /**
      * 计算特征值质量
      *
      * @param xPose   角度
      * @param quality 质量分值
      * @param time    抓拍时间
      * @return 特征值质量
      */
    def calculateFeatureQuality(xPose: XPose)(quality: Float)(time: Timestamp): Float = {
      var featureQuality = .0f
      if (clusterQualityFilter(quality) && clusterPoseFilter(xPose) && timeFilter(time)) {
        featureQuality = quality * calculatePoseWeight(xPose)
      } else if (classQualityFilter(quality) && classPoseFilter(xPose)) {
        featureQuality = -1.0f
      } else {
        featureQuality = -2.0f
      }
      featureQuality
    }

    def clusterPoseFilter(xPose: XPose): Boolean = XPoseUtils.inAngle(xPose, configArgs.clusterPitchThreshold, configArgs.clusterRollThreshold, configArgs.clusterYawThreshold)

    def classPoseFilter(xPose: XPose): Boolean = XPoseUtils.inAngle(xPose, configArgs.classPitchThreshold, configArgs.classRollThreshold, configArgs.classYawThreshold)

    def clusterQualityFilter(quality: Float): Boolean = quality >= configArgs.clusterQualityThreshold

    def classQualityFilter(quality: Float): Boolean = quality >= configArgs.classQualityThreshold

    // 夜间照片过滤，不能用作聚档
    def timeFilter(time: Timestamp): Boolean = {
      val startTime = configArgs.filterStartTime
      val endTime = configArgs.filterEndTime
      if (configArgs.filterNightEnable && startTime != null && endTime != null) {
        val dateTime = time.toLocalDateTime
        val boo = (dateTime.getHour >= startTime.hour && dateTime.getMinute >= startTime.min && dateTime.getSecond >= startTime.sec) ||
          (dateTime.getHour <= endTime.hour && dateTime.getMinute <= endTime.min && dateTime.getSecond <= endTime.sec)
        !boo
      } else {
        true
      }
    }

    var xPose: XPose = null
    var featureQuality = 0.0f
    if (!StringUtils.isEmpty(pose) && !faceFeature.isEmpty) {
      try {
        xPose = JSON.parseObject(pose, classOf[XPose])
        featureQuality = calculateFeatureQuality(xPose)(quality)(time)
      } catch {
        case _: Throwable =>
          featureQuality = -2.0f
          LOG.error("======> json parse to XPose failure, json is {}", pose)
      }
    } else {
      featureQuality = -2.0f
    }
    featureQuality.formatted("%.2f").toFloat
  }

  /**
    * 清除数据
    *
    * @param sequence 起始 sequence Tupple
    */
  private def clearDatas(sequence: (Long, Long)): Unit = {
    var conn: Connection = null
    try {
      conn = ConnectionUtils.getConnection(configArgs.gpUrl, configArgs.gpUser, configArgs.gpPasswd)
      clearDataBySequence(sequence, configArgs.clusterTable, conn)
      clearDataBySequence(sequence, configArgs.classTable, conn)
      clearDataBySequence(sequence, configArgs.unlessTable, conn)
      clearDataBySequence(sequence, configArgs.childTable, conn)
    } finally {
      ConnectionUtils.closeConnection(conn)
    }
  }


  /**
    * 根据 sequence 清除预处理表中的数据
    *
    * @param sequence 起始 sequence Tupple
    * @param table    表名
    * @param conn     数据库连接
    * @return
    */
  private def clearDataBySequence(sequence: (Long, Long), table: String, conn: Connection): Unit = {
    var pstmt: PreparedStatement = null
    try {
      val sql = s"delete from $table WHERE table_sequence >= ${sequence._1} and table_sequence < ${sequence._2}"
      println(s"======> clear sql -- $sql")

      pstmt = conn.prepareStatement(sql)
      val result = pstmt.executeUpdate()
      println(s"======> clear $result row data from $table")
    } finally {
      ConnectionUtils.closeStatement(pstmt)
    }
  }
}
