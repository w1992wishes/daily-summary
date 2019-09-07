package me.w1992wishes.hbase.inaction.bulkload;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.RegionLocator;
import org.apache.hadoop.hbase.tool.LoadIncrementalHFiles;

public class CompleteBulkLoad {
    public static void main(String[] args) throws Exception {
        Configuration conf = HBaseConfiguration.create();
        Connection connection = ConnectionFactory.createConnection(conf);
        String hFilePath = "hdfs://master:9000/test/hfiles";
        HBaseConfiguration.addHbaseResources(conf);
        LoadIncrementalHFiles loadFiles = new LoadIncrementalHFiles(conf);
        RegionLocator regionLocator = connection.getRegionLocator(TableName.valueOf("users"));
        loadFiles.doBulkLoad(new Path(hFilePath), connection.getAdmin(), connection.getTable(TableName.valueOf("users")), regionLocator);
    }
}