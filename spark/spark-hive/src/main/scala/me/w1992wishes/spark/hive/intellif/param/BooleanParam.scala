package me.w1992wishes.spark.hive.intellif.param

/**
 * An extractor object for parsing strings into booleans.
 */
object BooleanParam {
  def unapply(str: String): Option[Boolean] = {
    try {
      Some(str.toBoolean)
    } catch {
      case _: ClassCastException => None
    }
  }
}