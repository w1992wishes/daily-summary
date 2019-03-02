package me.w1992wishes.spark.etl.config

import java.io.FileInputStream
import java.util.Properties

import me.w1992wishes.spark.etl.util.IntParam

/**
  * 配置文件参数
  *
  * @author w1992wishes 2019/1/14 10:37
  */
class ConfigArgs extends Serializable{

  val filePath = "system.properties"
  val properties = new Properties()
  properties.load(new FileInputStream(filePath))

  // preProcess 数据源配置
  val dbUrl: String = properties.getProperty("preProcess.db.url")
  val dbDriver: String = properties.getProperty("preProcess.db.driver")
  val dbUser: String = properties.getProperty("preProcess.db.user")
  val dbPasswd: String = properties.getProperty("preProcess.db.passwd")
  val dbFetchsize: String = properties.getProperty("preProcess.db.fetchsize")
  val dbBatchsize: Int = properties.getProperty("preProcess.db.batchsize").toInt
  val sourceTable: String = properties.getProperty("preProcess.source.table")
  val sinkTable: String = properties.getProperty("preProcess.sink.table")

  // job 不同配置属性
  val sparkAppName: String = properties.getProperty("preProcess.name")

  val clusterQualityThreshold: Float = properties.getProperty("preProcess.clusterQualityThreshold").toFloat
  val classQualityThreshold: Float = properties.getProperty("preProcess.classQualityThreshold").toFloat

  val clusterPitchThreshold: Float = properties.getProperty("preProcess.clusterPitchThreshold").toFloat
  val clusterRollThreshold: Float = properties.getProperty("preProcess.clusterRollThreshold").toFloat
  val clusterYawThreshold: Float = properties.getProperty("preProcess.clusterYawThreshold").toFloat
  val classPitchThreshold: Float = properties.getProperty("preProcess.classPitchThreshold").toFloat
  val classRollThreshold: Float = properties.getProperty("preProcess.classRollThreshold").toFloat
  val classYawThreshold: Float = properties.getProperty("preProcess.classYawThreshold").toFloat

  // 年龄过滤
  val filterAgeEnable: Boolean = properties.getProperty("preProcess.filterAgeEnable").toBoolean
  val realAgeThreshold: Int = properties.getProperty("preProcess.realAgeThreshold").toInt

  // 夜间过滤
  val filterNightEnable: Boolean = properties.getProperty("preProcess.filterNightEnable").toBoolean
  // 过滤的起始时间
  val filterStartTime: AppTime = parseTime(properties.getProperty("preProcess.filterStartTime"))
  // 过滤的结束时间
  val filterEndTime: AppTime = parseTime(properties.getProperty("preProcess.filterEndTime"))

  private def parseTime(timeStr: String): AppTime = {
    val timeArray = timeStr.split(":").toList
    timeArray match {
      case IntParam(value1) :: IntParam(value2) :: IntParam(value3) :: _ =>
        AppTime(value1, value2, value3)
      case IntParam(value1) :: IntParam(value2) :: _ =>
        AppTime(value1, value2, 0)
      case IntParam(value1) :: _ =>
        AppTime(value1, 0, 0)
    }
  }
  // 用于过滤夜间抓拍照片的 case 类
  case class AppTime(hour: Int, min: Int, sec: Int)
}
