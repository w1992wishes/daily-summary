package me.w1992wishes.common.util

/**
  * An extractor object for parsing strings into integers.
  */
object IntParam {
  def unapply(str: String): Option[Int] = {
    try {
      Some(str.toInt)
    } catch {
      case _: NumberFormatException => None
    }
  }
}
