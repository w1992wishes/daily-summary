spark-submit \
--master yarn \
--deploy-mode client \
--class me.w1992wishes.spark.sql.`case`.config.PersonEventBatchEtlTask \
--num-executors 16 \
--executor-cores 2 \
--executor-memory 4G \
--conf spark.sql.autoBroadcastJoinThreshold=104857600 \
--files PersonEventEtlTask.properties \
etl-batch-task.jar --partitions 64