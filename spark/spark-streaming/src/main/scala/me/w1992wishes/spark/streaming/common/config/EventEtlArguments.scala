package me.w1992wishes.spark.streaming.common.config

/**
  * @author w1992wishes 2019/11/22 10:42
  */
class EventEtlArguments(args: List[String]) extends TaskArguments(args: List[String]) {
  override def parseCustomArguments(args: List[String]): Unit = args match {

    case Nil => // No-op

    case _ =>
      printUsageAndExit(1)
  }
}

object EventEtlArguments {
  def apply(args: List[String]): EventEtlArguments = new EventEtlArguments(args)
}
