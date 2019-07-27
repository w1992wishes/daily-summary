## 相关参数

--archiveStartTime
"2019-05-21 17:12:59"
--archiveEndTime
"2019-05-21 17:14:26"
--archiveEventStartTime
"2019-06-21 11:36:05"
--archiveEventEndTime
"2019-06-21 22:05:08"
--gpIp
192.168.13.82
--archiveExceptTable
odl_bigdata_import_personfile_5029_wqf
--isLocal
true
--isSaveTempData
true



spark-submit \
--master spark://master:7077 \
--class me.w1992wishes.spark.sql.DifferenceSetCalculate \
--executor-memory 16G \
--total-executor-cores 112 \
--executor-cores 4 \
--conf spark.default.parallelism=336 \
except-set.jar --partitions 336 --archiveTable odl_bigdata_import_personfile_5029_copy --archiveStartTime "2019-05-21 17:12:59" --archiveEndTime "2019-05-21 17:14:26" --archiveEventStartTime "2019-06-21 11:36:05" --archiveEventEndTime "2019-06-21 22:05:08" --gpIp 192.168.13.82 --archiveExceptTable odl_bigdata_import_personfile_5029_wqf
