package me.w1992wishes.spark.etl

import me.w1992wishes.spark.etl.config.CommandLineArgs
import me.w1992wishes.spark.etl.core.{PreProcess, PreProcessOnRDBMS}

/**
  * @author w1992wishes 2019/2/26 13:52
  */
object Bootstrap {

  def main(args: Array[String]): Unit = {
    val commandLineArgs: CommandLineArgs = new CommandLineArgs(args)
    val preProcessJob: PreProcess = new PreProcessOnRDBMS(commandLineArgs)
    preProcessJob.wholePreProcess()
  }

}
