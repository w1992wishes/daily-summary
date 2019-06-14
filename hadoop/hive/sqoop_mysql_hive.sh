#!/bin/bash

# 第一步，先创建 hive 表，见 hive.sql

# 执行sqoop直接导入hive
sqoop import \
--connect jdbc:postgresql://192.168.13.82:5432/bigdata_dwd \
--username gpadmin \
--password gpadmin \
--table dwd_bigdata_event_face_5029_copy \
--fields-terminated-by '\t' \
--delete-target-dir \
--split-by 'create_time' \
--num-mappers 10 \
--hive-import \
--hive-database default \
--hive-table dwd_bigdata_event_face_5029

# hive 不支持 gp 中的 bytea 类型

sqoop import \
--connect jdbc:postgresql://192.168.13.82:5432/bigdata_dwd \
--username gpadmin \
--password gpadmin \
--table dwd_bigdata_event_face_5029_copy \
--fields-terminated-by '\t' \
--delete-target-dir \
--split-by 'create_time' \
--num-mappers 10 \
--hive-import \
--hive-database default \
--hive-table dwd_bigdata_event_face_5029
--map-column-hive feature_info=BINARY

# 运行发现 ERROR tool.ImportTool:Import failed: java.io.IOException: java.lang.ClassNotFoundException: org.apache.hadoop.hive.conf.HiveConf

# 将 hive-common-2.3.5.jar 放入 sqoop/lib 下