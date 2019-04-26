package me.w1992wishes.my.common.ability

import org.slf4j.{Logger, LoggerFactory}

/**
  * @author w1992wishes 2019/4/25 19:15
  */
trait Log {

  val log: Logger = LoggerFactory.getLogger(getClass)

  def logInfo(message: String): Unit = {
    log.info(message)
  }

  def logInfo(messageFormat: String, obj: Object): Unit = {
    log.info(messageFormat, obj)
  }

  def logError(message: String): Unit = {
    log.error(message)
  }

  def logWarn(messageFormat: String, obj: Object): Unit = {
    log.info(messageFormat, obj)
  }
}
