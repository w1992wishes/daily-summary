package me.w1992wishes.spark.etl.job2

import java.io.FileInputStream
import java.util.Properties

import me.w1992wishes.spark.etl.util.IntParam

/**
  * 配置文件参数
  *
  * @author w1992wishes 2019/1/14 10:37
  */
private[core] class ConfigArgs {

  val filePath = "system.properties"
  val properties = new Properties()
  properties.load(new FileInputStream(filePath))

  // greenplum 配置属性
  val gpUrl: String = properties.getProperty("gp.url")
  val gpDriver: String = properties.getProperty("gp.driver")
  val gpUser: String = properties.getProperty("gp.user")
  val gpPasswd: String = properties.getProperty("gp.passwd")
  val gpFetchsize: Int = properties.getProperty("gp.fetchsize").toInt
  val gpBatchsize: Int = properties.getProperty("gp.batchsize").toInt

  // job 不同配置属性
  val sparkAppName: String = properties.getProperty("preprocess.name")
  val sourceTable: String = properties.getProperty("preprocess.sourceTable")
  val clusterTable: String = properties.getProperty("preprocess.clusterTable")
  val classTable: String = properties.getProperty("preprocess.classTable")
  val childTable: String = properties.getProperty("preprocess.childTable")
  val unlessTable: String = properties.getProperty("preprocess.unlessTable")
  // 质量属性好坏阈值
  val clusterQualityThreshold: Float = properties.getProperty("preprocess.clusterQualityThreshold").toFloat
  val classQualityThreshold: Float = properties.getProperty("preprocess.classQualityThreshold").toFloat
  val clusterPitchThreshold: Float = properties.getProperty("preprocess.clusterPitchThreshold").toFloat
  val clusterRollThreshold: Float = properties.getProperty("preprocess.clusterRollThreshold").toFloat
  val clusterYawThreshold: Float = properties.getProperty("preprocess.clusterYawThreshold").toFloat
  val classPitchThreshold: Float = properties.getProperty("preprocess.classPitchThreshold").toFloat
  val classRollThreshold: Float = properties.getProperty("preprocess.classRollThreshold").toFloat
  val classYawThreshold: Float = properties.getProperty("preprocess.classYawThreshold").toFloat
  // 年龄阈值
  val realAgeThreshold: Int = properties.getProperty("preprocess.realAgeThreshold").toInt
  val ageStageThreshold: Int = properties.getProperty("preprocess.ageStageThreshold").toInt

  // 是否过滤夜间
  val filterNightEnable: Boolean = properties.getProperty("preprocess.filterNightEnable").toBoolean
  // 过滤的起始时间
  val filterStartTime: AppTime = parseTime(properties.getProperty("preprocess.filterStartTime"))
  // 过滤的结束时间
  val filterEndTime: AppTime = parseTime(properties.getProperty("preprocess.filterEndTime"))

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
