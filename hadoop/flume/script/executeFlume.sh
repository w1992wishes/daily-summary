#!/bin/bash

#参数：start|stop|restart

#功能：flume 启动停止重启

#使用方法:

#./execflume.sh start flume_cmbc.conf(配置文件，自己修改) Cobub（代理名称，自己修改）

#./execflume.sh stop#./execflume.sh restart flume_cmbc.conf(配置文件，自己修改) a1（代理名称，自己修改）
DIR=$(cd `dirname $0`; pwd)
echo $DIR
AGENT_CONF=$2
AGENT=$3
LOG="$4.log"
JAR="flume"

function start(){
    echo "begin start process ...."
    num=`ps -ef|grep java|grep $JAR|grep $AGENT_CONF|wc -l`
    if [ "$num" = "0" ] ; then
        cmd='nohup flume-ng agent -c /etc/flume-ng/conf -f $AGENT_CONF -n $AGENT -Dflume.root.logger=info,LOGFILE -Dflume.log.file=$LOG >nohup.log 2>&1 &'
        echo ${cmd}
        ${cmd}
        echo "启动成功 ..."
    else
        echo "进程已经存在，启动失败，请检查 ..."
        exit 0
    fi
}

function stop(){
    echo "begin stop process ......"
    num=`ps -ef|grep -v grep|grep flume-ng|grep $AGENT_CONF|wc -l`
    if [ "$num" != "0" ] ; then
        # 正常停止flume
        PID=`ps -ef|grep -v grep|grep flume-ng|grep AGENT_CONF|awk '{print $2}'`
        kill -15 $PID
        echo "进程已经关闭 ..."
    else
        echo "服务未启动，无需停止 ..."
    fi
}

function restart(){
    echo "begin restart process ......"
    stop
    # 判断程序是否彻底停止
    num=`ps -ef|grep java|grep $JAR|grep $AGENT_CONF|wc -l`
    while [ $num -gt 0 ]; do
        sleep 1
        num=`ps -ef|grep java|grep $JAR|grep $AGENT_CONF|wc -l`
    done
    start
    echo "started ..."
}

case "$1" in
    "start")
      start $@
      exit 0
    ;;
    "stop")
      stop
      exit 0
     ;;
    "restart")
       restart $@
       exit 0
     ;;
    *)
       echo "用法： $0 {start|stop|restart}"
       exit 1
    ;;
esac