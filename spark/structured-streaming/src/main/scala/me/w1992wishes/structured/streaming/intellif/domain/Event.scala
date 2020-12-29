package me.w1992wishes.structured.streaming.intellif.domain

import java.sql.Timestamp

/**
  * @author w1992wishes 2020/11/3 15:37
  */
case class OriginalEvent(bizCode: String, id: String, dataType: String, dataCode: String, time: Timestamp,
                         location: String, guid: String, createTime: Timestamp, modifyTime: String, sysCode: String, props: String)

case class SimplifiedUnArchivedEvent(biz_code: String, data_code: String, data_type: String, sys_code: String, props: String)

case class SimplifiedMixedEvent(biz_code: String, data_code: String, data_type: String, sys_code: String, props: String,
                           aid: String, allocate_aid: Boolean) // allocate 表示是否需要分配 aid, true 表示需要

case class SimplifiedArchivedEvent(biz_code: String, data_code: String, data_type: String, sys_code: String, props: String,
                                   aid: String)

case class UnArchivedEvent(biz_code: String, id: String, data_code: String, time: Timestamp, coll_dt: String,
                           location: String, geo_hash: String, guid: String, create_time: Timestamp, modify_time: Timestamp,
                           sys_code: String, props: String, dt: String, data_type: String)

case class UnArchivedEsEvent(biz_code: String, id: String, data_code: String, time: Timestamp, coll_dt: Timestamp,

                             location: String, geo_hash: String, guid: String, create_time: Timestamp, modify_time: Timestamp,
                           sys_code: String, props: String, dt: String, data_type: String)

case class ArchivedEvent(biz_code: String, id: String, aid: String, data_code: String, time: Timestamp,
                 coll_dt: String, location: String, geo_hash: String, guid: String, create_time: Timestamp,
                 modify_time: Timestamp, sys_code: String, props: String, dt: String, data_type: String)