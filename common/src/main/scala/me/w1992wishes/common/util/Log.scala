package me.w1992wishes.common.util

import org.apache.spark.internal.Logging

trait Log extends Logging{

  def logInfos(messages : String*): Unit ={
    messages.foreach(message => {
      logInfo(message)
    })
  }

  def logErrors(messages : String*): Unit ={
    messages.foreach(message => {
      logError(message)
    })
  }

  def logDebugs(messages : String*): Unit ={
    messages.foreach(message => {
      logDebug(message)
    })
  }

  def logWarnings(messages : String*): Unit ={
    messages.foreach(message => {
      logWarning(message)
    })
  }


}
