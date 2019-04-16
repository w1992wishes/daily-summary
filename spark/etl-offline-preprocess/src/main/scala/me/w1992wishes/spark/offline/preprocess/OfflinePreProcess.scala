package me.w1992wishes.spark.offline.preprocess

import me.w1992wishes.spark.offline.preprocess.config.CommandLineArgs
import me.w1992wishes.spark.offline.preprocess.core.{PreProcess, PreProcessOnGpPartitions, PreProcessOnRDBMS}

/**
  * @author w1992wishes 2019/2/26 13:52
  */
object OfflinePreProcess {

  def main(args: Array[String]): Unit = {
    val commandLineArgs: CommandLineArgs = new CommandLineArgs(args)
    val preProcessJob: PreProcess =
      commandLineArgs.preProcessImpl match {
        case 1 => new PreProcessOnRDBMS(commandLineArgs)
        case 2 => new PreProcessOnGpPartitions(commandLineArgs)
        case _ => new PreProcessOnRDBMS(commandLineArgs)
      }
    println(s"======> ${preProcessJob.getClass}")
    preProcessJob.wholePreProcess()
  }

}
