package me.w1992wishes.common.util

import java.security.MessageDigest

object Md5 {
  def hashMD5(content: String): String = {
    val md5 = MessageDigest.getInstance("MD5")
    val encoded = md5.digest(content.getBytes)
    encoded.map("%02x".format(_)).mkString
  }
}