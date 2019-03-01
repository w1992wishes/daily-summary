#!/bin/bash
### every exit != 0 fails the script
set -e

# 获取目标数据库最大数据时间，并写入一个 csv 文件
python $DATAX_HOME/bin/datax.py greenplumToTxt.json

if [[ $? -ne 0 ]]; then
  echo "sync.sh error, can not get max_time from target db!"
  exit 1
fi
# 找到 DataX 写入的文本文件，并将内容读取到一个变量中
RESULT_FILE=`ls face_event_20_5024_max_time_result_*`
MAX_TIME=`cat $RESULT_FILE`
# 如果最大时间不为 null 的话， 修改全部同步的配置，进行增量更新；
if [[ "$MAX_TIME" != "null" ]]; then
  # 设置增量更新过滤条件
  WHERE="create_time > '$MAX_TIME'"
  sed "s/1=1/$WHERE/g" mysqlToGreenplum.json > mysqlToGreenplum_tmp.json
  # 将第 45 行的 truncate 语句删除；
  sed '63d' mysqlToGreenplum_tmp.json > mysqlToGreenplum_inc.json
  # 增量更新
  python $DATAX_HOME/bin/datax.py mysqlToGreenplum_inc.json
  # 删除临时文件
  rm mysqlToGreenplum_tmp.json mysqlToGreenplum_inc.json
else
  # 全部更新
  python $DATAX_HOME/bin/datax.py mysqlToGreenplum.json
fi