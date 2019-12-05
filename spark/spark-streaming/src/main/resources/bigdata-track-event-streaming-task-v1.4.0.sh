#!/bin/bash

MASTER="spark://master:7077"
DEPLOY_MODE="client"
APP_MAINCLASS="com.intellif.dataplatform.streaming.task.StreamingEventEtlTask"
EXECUTOR_MEMORY="4G"
TOTAL_EXECUTOR_CORES=4
EXECUTOR_CORES=2
CONF_FILE="bigdata-track-event-streaming-task-v1.4.0.properties"
APP_JAR="etl-streaming-task.jar"
APP_ARG="--partitions 12 --confName $CONF_FILE"

spark-submit \
--master $MASTER \
--deploy-mode $DEPLOY_MODE \
--class $APP_MAINCLASS \
--executor-memory $EXECUTOR_MEMORY \
--total-executor-cores $TOTAL_EXECUTOR_CORES \
--executor-cores $EXECUTOR_CORES \
--files $CONF_FILE \
$APP_JAR $APP_ARG