#!/bin/sh

## add oracle jar
export HADOOP_CLASSPATH=$HADOOP_CLASSPATH:./lib/ojdbc6.jar
#source ./config/matrix-sqoop-jdbc.properties

opts=$@
getparams(){
arg=$1
echo ${opts} |xargs -n1 |cut -b 2- |awk -F'=' '{if($1=="'"$arg"'") print $2}'
#return 10
}

connect=`getparams "connect"`
userName=`getparams "userName"`
password=`getparams "password"`
bizCode=`getparams "bizCode"`
sourceTableName=`getparams "sourceTableName" | sed 's/#bizCode#/'${bizCode}'/g'`
##　默认昨天
dt=`date --date='1 days ago' +%Y%m%d`
targetDir=/user/sqoop/${bizCode}_odl/${sourceTableName}/${dt}
daysAgo=`getparams "daysAgo"`
startTime=`getparams "startTime"`
endTime=`getparams "endTime"`
today=`date +%Y/%m/%d`
incremental=`getparams "incremental"`
splitBy=`getparams "splitBy"`
mapNum=`getparams "mapNum"`

if  [[ -z ${daysAgo}  ]]
then
    daysAgo=1
fi
if  [[ -z ${endTime}  ]]
then
    endTime=${today}
fi
if  [[ -z ${startTime}  ]]
then
    startTime=`date -d "${endTime} -${daysAgo} days "  "+%Y/%m/%d"`
fi

echo "bizCode:${bizCode}"
echo "sourceTableName:${sourceTableName}"
echo "targetDir:${targetDir}"
echo "today:${today}"
echo "daysAgo:${daysAgo}"
echo "startTime:${startTime}"
echo "endTime:${endTime}"
echo "incremental:${incremental}"
echo "splitBy:${splitBy}"
echo "mapNum:${mapNum}"

sqoop import \
--connect ${connect} \
--username ${userName} \
--password ${password} \
--target-dir ${targetDir} \
--delete-target-dir \
--query "select * from ${sourceTableName} where \$CONDITIONS and ${incremental} >= '${startTime}' and ${incremental} < '${endTime}'" \
--split-by ${splitBy} \
-m ${mapNum} \
--fields-terminated-by '\t'