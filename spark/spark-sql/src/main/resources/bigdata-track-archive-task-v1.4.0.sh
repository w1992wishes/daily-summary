#!/bin/bash

EVENT_TYPE=$1
MASTER="spark://master:7077"
DEPLOY_MODE="client"
APP_MAINCLASS="com.intellif.dataplatform.batch.task.ArchiveBatchEtlTask"
EXECUTOR_MEMORY="8G"
EXECUTOR_CORES=4
TOTAL_EXECUTOR_CORES=32
PARTITIONS=`expr 2 \* $TOTAL_EXECUTOR_CORES`
CONF_FILE="bigdata-track-archive-task-v1.4.0.properties"
APP_JAR="etl-batch-task.jar"
APP_ARG="--partitions $PARTITIONS --confName $CONF_FILE --eventType $EVENT_TYPE --shufflePartitions 400"

COMMAND="spark-submit \
--master $MASTER \
--deploy-mode $DEPLOY_MODE \
--class $APP_MAINCLASS \
--executor-memory $EXECUTOR_MEMORY \
--total-executor-cores $TOTAL_EXECUTOR_CORES \
--executor-cores $EXECUTOR_CORES \
--files $CONF_FILE \
$APP_JAR $APP_ARG"

echo $COMMAND
eval $COMMAND