package me.w1992wishes.spark.streaming.example.intellif.config

/**
  * @author w1992wishes 2019/5/15 14:54
  */
class PreProcessTaskArguments(args: List[String]) extends TaskArguments(args: List[String]) {

  override def parseCustomArguments(args: List[String]): Unit = args match {

    case Nil => // No-op

    case _ =>
      printUsageAndExit(1)
  }

  override def printUsageAndExit(exitCode: Int): Unit = {
    System.err.println(
      commonUsageInfo()
    )
  }
}

object PreProcessTaskArguments {
  def apply(args: List[String]): PreProcessTaskArguments = new PreProcessTaskArguments(args)
}


