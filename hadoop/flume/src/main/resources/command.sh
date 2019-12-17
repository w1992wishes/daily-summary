flume-ng agent -c conf -f ./flume_mysql.conf -n a1 -Dflume.root.logger=INFO,console

flume-ng agent --conf conf --conf-file /home/hadoop/flume/conf/flume-client.conf --name a1 -Dflume.root.logger=INFO,console
flume-ng agent --conf conf --conf-file /home/hadoop/flume/conf/flume-client.conf --name a1 -Dflume.root.logger=INFO,console
flume-ng agent --conf conf --conf-file /home/hadoop/flume/conf/flume-server.conf --name a1 -Dflume.root.logger=INFO,console
flume-ng agent --conf conf --conf-file /home/hadoop/flume/conf/flume-server.conf --name a1 -Dflume.root.logger=INFO,console