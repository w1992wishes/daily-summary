package me.w1992wishes.common.conf

object ParamOptions {

  // PROJECT
  val BIZ_CODE = "--bizCode"

  // TASK
  val DT = "--dt"
  val START_TIME = "--startTime"
  val END_TIME = "--endTime"
  val DAYS_AGO = "--daysAgo"
  val DATA_TYPE = "--dataType"
  val EXPAND_SOURCE_ID = "--expandSourceId"
  val SCORE_THRESHOLD = "--scoreThreshold"
  val CNT_THRESHOLD = "--cntThreshold"
  val PARTITIONS_NUM = "--partitionsNum"
  val SPARK_IS_LOCAL = "--sparkIsLocal"
  val SOURCE_TYPE = "--sourceType"
  val TARGET_TYPE = "--targetType"
  val CLEAR = "--clear"
  val UPSERT = "--upsert"
  val WEIGHT = "--weight"
  val WEIGHT_INDIV = "--weightIndiv"
  val WEIGHT_ASSOC_DEGREE = "--weightAssocDegree"
  val WEIGHT_CONTI_DAY = "--weightContiDay"
  val DISTABCE = "--distance"
  val INTERVAL = "--interval"
  val GEO_LENGTH = "--geoLength"
  val TASK_ID = "--taskId"

  // ARANGODB
  val ARANGODB_KEY = "--arangodbKey"
  val ARANGODB_SOURCE_TABLE = "--arangodbSourceTable"
  val ARANGODB_RESULT_TYPE = "--arangodbResultType"
  val ARANGODB_RESULT_KEY_FIELD = "--arangodbResultKeyField"
  val ARANGODB_RESULT_GRAPH_NAME = "--arangodbResultGraphName"
  val ARANGODB_RESULT_COLLECTION_NAME = "--arangodbResultCollectionName"
  val ARANGODB_RESULT_EDGE_DEFINITIONS = "--arangodbResultEdgeDefinitions"

  // ES
  val ES_KEY = "--esKey"
  val ES_WRITE_OPERATION = "--esWriteOperation"
  val ES_MAPPING_ID = "--esMappingId"
  val ES_SAVE_MODE = "--esSaveMode"

  // SPARK
  val SHUFFLE_PARTITION = "--shufflePartition"

  // GP
  val PARTITION_FIELD_NAME = "--partitionFieldName"
  val PARALLEL_PARTITION = "--parallelPartition"
}
