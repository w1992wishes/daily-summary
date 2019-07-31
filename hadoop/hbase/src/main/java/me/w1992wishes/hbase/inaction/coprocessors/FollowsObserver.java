package me.w1992wishes.hbase.inaction.coprocessors;

import me.w1992wishes.hbase.inaction.hbase.RelationsDAO;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CoprocessorEnvironment;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessor;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.coprocessor.RegionObserver;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.wal.WALEdit;

import java.io.IOException;

import static me.w1992wishes.hbase.inaction.hbase.RelationsDAO.*;

/**
 * 2.0 版本之前使用extends BaseRegionObserver 实现
 *
 * @author w1992wishes 2019/7/30 20:52
 */
public class FollowsObserver implements RegionObserver, RegionCoprocessor {


    private Connection conn = null;

    @Override
    public void start(CoprocessorEnvironment env) throws IOException {
        conn = ConnectionFactory.createConnection(env.getConfiguration());
    }

    @Override
    public void stop(CoprocessorEnvironment env) throws IOException {
        conn.close();
    }

    @Override
    public void postPut(ObserverContext<RegionCoprocessorEnvironment> c, Put put, WALEdit edit, Durability durability)
            throws IOException {

        byte[] table = c.getEnvironment().getRegion().getRegionInfo().getTable().getName();
        if (!Bytes.equals(table, FOLLOWS_TABLE_NAME)) {
            return;
        }

        Cell fCell = put.get(RELATION_FAM, FROM).get(0);
        String from = Bytes.toString(fCell.getValueArray());
        Cell tCell = put.get(RELATION_FAM, TO).get(0);
        String to = Bytes.toString(tCell.getValueArray());

        RelationsDAO relations = new RelationsDAO(conn);
        relations.addFollowedBy(to, from);
    }

}
