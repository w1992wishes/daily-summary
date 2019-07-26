package me.w1992wishes.spark.offline.preprocess.core

import java.time.LocalDateTime

import me.w1992wishes.common.util.DateUtils
import me.w1992wishes.spark.offline.preprocess.config.CommandLineArgs
import me.w1992wishes.spark.offline.preprocess.util.Constants
import org.apache.commons.lang3.StringUtils
import org.apache.spark.sql.SaveMode

/**
  * 预处理任务，基于分区表实现
  *
  * @author w1992wishes 2019/3/22 15:08
  */
class PreProcessOnGpPartitions(commandLineArgs: CommandLineArgs) extends CommonPreProcess(commandLineArgs: CommandLineArgs) {

  private def partitionPostfix: String = commandLineArgs.partitionType match {
    case "d" => Constants.GP_PARTITION_STR.concat(DateUtils.dateTimeToStr(LocalDateTime.now().minusDays(1), DateUtils.DF_YMD_NO_LINE))
    case "h" => Constants.GP_PARTITION_STR.concat(DateUtils.dateTimeToStr(LocalDateTime.now().minusHours(1), DateUtils.DF_YMDH_NO_LINE))
  }

  // 预处理前的表
  override def getPreProcessTable: String =
    if (StringUtils.isNotEmpty(commandLineArgs.preProcessTable))
      commandLineArgs.preProcessTable
    else
      config.sourceTable.concat(partitionPostfix)

  // 预处理后的表
  override def getPreProcessedTable: String =
    if (StringUtils.isNotEmpty(commandLineArgs.preProcessedTable))
      commandLineArgs.preProcessedTable
    else
      config.sinkTable

  override def preProcessBefore(): Unit = {
    //clearDatas()
  }

  override def preProcessPost(): Unit = {
    // wait to do something
  }

  override def getSaveMode: SaveMode = SaveMode.Append

  /*  /**
      * 清除数据
      *
      */
    private def clearDatas(): Unit = {
      var conn: Connection = null
      var st: Statement = null
      try {
        conn = ConnectionUtils.getConnection(config.sinkUrl, config.sinkUser, config.sinkPasswd)
        val sql = s"TRUNCATE TABLE $preProcessedTable"
        println(s"======> TRUNCATE TABLE sql -- $sql")

        st = conn.createStatement()
        st.executeUpdate(sql)
      } finally {
        ConnectionUtils.closeConnection(conn)
        ConnectionUtils.closeStatement(st)
      }
    }*/
}
