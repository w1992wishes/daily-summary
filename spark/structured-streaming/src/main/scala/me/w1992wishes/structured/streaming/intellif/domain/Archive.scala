package me.w1992wishes.structured.streaming.intellif.domain

import java.sql.Timestamp

/**
  * @author w1992wishes 2020/11/4 16:28
  */
case class Archive(biz_code: String, aid: String, data_code: String, status: Int, create_time: Timestamp,
                   modify_time: Timestamp, sys_code: String, props: String, data_type: String)

case class SimplifiedArchive(biz_code: String, aid: String, data_type: String, data_code: String)