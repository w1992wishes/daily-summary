# 利用 DataX 进行数据增量同步

用法：

* 首先到 system.properties 中配置属性：
```json
## datax 计算最大时间 json 文件名
dataxSync.faceEvent.maxTimeJson=maxTimeJson
## datax 增量同步 json 文件名
dataxSync.faceEvent.incrementSync=incrementSync

## datax 数据源 jdbc 连接
dataxSync.faceEvent.sourceJdbc=jdbc:mysql://116.7.23.147:9026/intellif_mining
dataxSync.faceEvent.sourceTable=t_capture_event_20_5024
dataxSync.faceEvent.sinkJdbc=jdbc:postgresql://192.168.11.72:5432/bigdata_ods
dataxSync.faceEvent.sinkTable=public.face_event_20_5024
```

* 然后在安装了 datax 并且将 datax 安装目录设置为环境变量 DATAX_HOME 的 linux 中运行 

```bash
sync.sh dataxSync.faceEvent
```
dataxSync.faceEvent 为配置的前缀
