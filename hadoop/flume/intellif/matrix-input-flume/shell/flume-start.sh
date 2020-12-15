#!/bin/bash

BIZCODE=$1
FLUME_LOG="$BIZCODE-input-flume.log"
FLUME_CONF="$BIZCODE-input-flume.conf"
FLUME_FLAG=$FLUME_CONF

function start(){
    PID=`ps -ef|grep -v grep|grep flume-ng|grep $FLUME_FLAG|awk '{print $2}'`
    NUM=`ps -ef|grep -v grep|grep flume-ng|grep $FLUME_FLAG|wc -l`
    if [ "$NUM" = "0" ] ; then
        cmd="nohup flume-ng agent -c /etc/flume-ng/conf -f $FLUME_CONF -n a1 -Dflume.root.logger=info,LOGFILE -Dflume.log.file=$FLUME_LOG >nohup.log 2>&1 &"
        echo $cmd
        eval $cmd
    else
        echo "$NUM process has started, please check..."
        exit 0
    fi
}

start