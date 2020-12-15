#!/bin/bash

BIZCODE=$1
FLUME_CONF="$BIZCODE-input-flume.conf"
FLUME_FLAG=$FLUME_CONF

function stop15(){
    PID=`ps -ef|grep -v grep|grep flume-ng|grep $FLUME_FLAG|awk '{print $2}'`
    NUM=`ps -ef|grep -v grep|grep flume-ng|grep $FLUME_FLAG|wc -l`
    if [ "$NUM" != "0" ] ; then
        echo "pid is $PID"
        ps -ef|grep -v grep|grep flume-ng|grep $FLUME_FLAG|awk '{print "kill -15 "$2}'
        ps -ef|grep -v grep|grep flume-ng|grep $FLUME_FLAG|awk '{print "kill -15 "$2}'|sh
        echo "killed gracefully"
    else
        echo "process not started, do not need stop..."
    fi
}

function stop9(){
    PID=`ps -ef|grep -v grep|grep flume-ng|grep $FLUME_FLAG|awk '{print $2}'`
    NUM=`ps -ef|grep -v grep|grep flume-ng|grep $FLUME_FLAG|wc -l`
    if [ "$NUM" != "0" ] ; then
        echo "pid is $PID"
        ps -ef|grep -v grep|grep flume-ng|grep $FLUME_FLAG|awk '{print "kill -9 "$2}'
        ps -ef|grep -v grep|grep flume-ng|grep $FLUME_FLAG|awk '{print "kill -9 "$2}'|sh
        echo "killed violently"
    else
        echo "do not need stop violently..."
    fi
}

stop15
stop9