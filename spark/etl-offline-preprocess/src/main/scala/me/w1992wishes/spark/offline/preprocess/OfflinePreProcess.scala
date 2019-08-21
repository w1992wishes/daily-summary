package me.w1992wishes.spark.offline.preprocess

import me.w1992wishes.spark.offline.preprocess.config.CommandLineArgs
import me.w1992wishes.spark.offline.preprocess.core.{PreProcess, PreProcessOnGpPartitions}

/**
  * @author w1992wishes 2019/2/26 13:52
  */
object OfflinePreProcess {

  def main(args: Array[String]): Unit = {
    val commandLineArgs: CommandLineArgs = new CommandLineArgs(args)
    val preProcessJob: PreProcess = new PreProcessOnGpPartitions(commandLineArgs)
    println(s"======> ${preProcessJob.getClass}")
    preProcessJob.wholePreProcess()
  }

}
