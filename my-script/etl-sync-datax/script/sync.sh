#!/bin/bash
### every exit != 0 fails the script
set -e

if [ ! -n "$1" ];then
    echo "Parm Doesn't exist, don't run this shell"
    exit 1
fi

# 第一个参数为项目前缀，用于适配不同的项目同步
PROJECT_PREFIX=$1

# 用sed s/[[:space:]]//g 去除 多余的空格符
# python 路径
PYTHON_PATH=`cat system.properties | grep "python.path" | awk -F'=' '{ print $2 }' | sed s/[[:space:]]//g`
DATAX_PATH=`cat system.properties | grep "datax.path" | awk -F'=' '{ print $2 }' | sed s/[[:space:]]//g`

TIME_JOSN=`cat system.properties | grep $PROJECT_PREFIX".maxTimeJson" | awk -F'=' '{ print $2 }' | sed s/[[:space:]]//g`
TIME_JSON_FILE="lib/"$TIME_JOSN".json"
TIME_TMP_FILE="lib/"$TIME_JOSN"_tmp.json"

SYNC_JOSN=`cat system.properties | grep $PROJECT_PREFIX".incrementSync" | awk -F'=' '{ print $2 }' | sed s/[[:space:]]//g`
SYNC_JSON_FILE="lib/"$SYNC_JOSN".json"
SYNC_TMP_FILE="lib/"$SYNC_JOSN"_tmp.json"

# 数据库连接
SOURCE_JDBC=`cat system.properties | grep $PROJECT_PREFIX".sourceJdbc" | awk -F'=' '{ print $2 }' | sed s/[[:space:]]//g`
SOURCE_USER=`cat system.properties | grep $PROJECT_PREFIX".sourceUser" | awk -F'=' '{ print $2 }' | sed s/[[:space:]]//g`
SOURCE_PWD=`cat system.properties | grep $PROJECT_PREFIX".sourcePwd" | awk -F'=' '{ print $2 }' | sed s/[[:space:]]//g`
SOURCE_TABLE=`cat system.properties | grep $PROJECT_PREFIX".sourceTable" | awk -F'=' '{ print $2 }' | sed s/[[:space:]]//g`

SINK_JDBC=`cat system.properties | grep $PROJECT_PREFIX".sinkJdbc" | awk -F'=' '{ print $2 }' | sed s/[[:space:]]//g`
SINK_USER=`cat system.properties | grep $PROJECT_PREFIX".sinkUser" | awk -F'=' '{ print $2 }' | sed s/[[:space:]]//g`
SINK_PWD=`cat system.properties | grep $PROJECT_PREFIX".sinkPwd" | awk -F'=' '{ print $2 }' | sed s/[[:space:]]//g`
SINK_TABLE=`cat system.properties | grep $PROJECT_PREFIX".sinkTable" | awk -F'=' '{ print $2 }' | sed s/[[:space:]]//g`
SINK_FILE=$SINK_TABLE"_"

#先修改 $TIME_JSON_FILE 中的数据连接
sed "s%sinkJdbc%$SINK_JDBC%g" $TIME_JSON_FILE > $TIME_TMP_FILE
sed -i "s/sinkTable/$SINK_TABLE/g" $TIME_TMP_FILE
sed -i "s/sinkUser/$SINK_USER/g" $TIME_TMP_FILE
sed -i "s/sinkPwd/$SINK_PWD/g" $TIME_TMP_FILE

# 获取目标数据库最大数据时间，并写入一个 csv 文件
$PYTHON_PATH/python $DATAX_PATH/datax.py $TIME_TMP_FILE

# 删除临时文件
rm $TIME_TMP_FILE

if [ $? -ne 0 ]; then
  echo "sync.sh error, can not get max_time from target db!"
  exit 1
fi

#先修改 $SYNC_JSON_FILE 中的数据连接
sed "s%sinkJdbc%$SINK_JDBC%g" $SYNC_JSON_FILE > $SYNC_TMP_FILE
sed -i "s/sinkTable/$SINK_TABLE/g" $SYNC_TMP_FILE
sed -i "s/sinkUser/$SINK_USER/g" $SYNC_TMP_FILE
sed -i "s/sinkPwd/$SINK_PWD/g" $SYNC_TMP_FILE

sed -i "s/sourceTable/$SOURCE_TABLE/g" $SYNC_TMP_FILE
sed -i "s%sourceJdbc%$SOURCE_JDBC%g" $SYNC_TMP_FILE
sed -i "s/sourceUser/$SOURCE_USER/g" $SYNC_TMP_FILE
sed -i "s/sourcePwd/$SOURCE_PWD/g" $SYNC_TMP_FILE

# 找到 DataX 写入最大时间的文本文件，并将内容读取到一个变量中
RESULT_FILE=`ls $SINK_FILE*`
MAX_TIME=`cat $RESULT_FILE`
# 如果最大时间不为 null 的话， 修改全部同步的配置，进行增量更新；
if [ "$MAX_TIME" != "null" ]; then
  # 设置增量更新过滤条件
  WHERE="create_time > '$MAX_TIME'"
  sed -i "s/1=1/$WHERE/g" $SYNC_TMP_FILE
  # 增量更新
  $PYTHON_PATH/python $DATAX_PATH/datax.py $SYNC_TMP_FILE
  # 删除临时文件
  rm $SYNC_TMP_FILE
else
  # 全部更新
  $PYTHON_PATH/python $DATAX_PATH/datax.py $SYNC_TMP_FILE
  rm $SYNC_TMP_FILE
fi