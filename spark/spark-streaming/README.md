## PreprocessJob

APP_MAINCLASS="me.w1992wishes.spark.streaming.common.task.StreamingPreProcessTask"
CONF_FILE="preprocess.properties"
spark-submit \
--master spark://127.0.0.1:7077 \
--class $APP_MAINCLASS \
--executor-memory 3G \
--total-executor-cores 12 \
--executor-cores 3 \
--files $CONF_FILE \
etl-streaming-preprocess.jar --confName $CONF_FILE