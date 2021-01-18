#!/bin/bash
opts=$@
getparams(){
arg=$1
echo $opts |xargs -n1 |cut -b 2- |awk -F'=' '{if($1=="'"$arg"'") print $2}'
#return 10
}

connect=`getparams "connect"`
userName=`getparams "userName"`
password=`getparams "password"`
bizCode=`getparams "bizCode"`
sourceTableName=`getparams "sourceTableName" | sed 's/#bizCode#/'$bizCode'/g'`
targetTableName=`getparams "targetTableName" | sed 's/#bizCode#/'$bizCode'/g'`
targetDir=/sqoopTest/$targetTableName
daysAgo=`getparams "daysAgo"`
startTime=`getparams "startTime"`
endTime=`getparams "endTime"`
today=`date +%Y%m%d`
yesterday=`date --date='1 days ago' +%Y%m%d`

if  [ -z $daysAgo  ]
then
    daysAgo=0
fi

if  [ -z $endTime  ]
then
    endTime=$today
fi

if  [ -z $startTime  ]
then
    startTime=`date -d "${endTime} -${daysAgo} days "  "+%Y%m%d"`
fi

echo bizCode:${bizCode}
echo sourceTableName:${sourceTableName}
echo targetTableName:${targetTableName}
echo daysAgo:${daysAgo}
echo today:${today}
echo yesterday:${yesterday}
echo startTime:${startTime}
echo endTime:${endTime}

sqoop import \
--connect ${connect} \
--username ${userName} \
--password ${password} \
--query 'select * from ${sourceTableName} where id>3 and $CONDITIONS' \
--target-dir $targetDir \
--fields-terminated-by "\t" \
--delete-target-dir \
--hive-import \
--hive-overwrite \
--hive-table $targetTableName \
--num-mappers 1 \
--split-by id
--null-string '\\N' \
--null-non-string '\\N'

#sqoop import \
#--connect jdbc:mysql://hadoop102:3306/test \
#--username root \
#--password 123 \
#--query 'select * from ${sourceTableName} where id>3 and $CONDITIONS' \
#--target-dir /sqoopTest \
#--fields-terminated-by "\t" \
#--delete-target-dir \
#--hive-import \
#--hive-overwrite \
#--hive-table $targetTableName \
#--num-mappers 1 \
#--split-by id \
#--null-string '\\N' \
#--null-non-string '\\N'

