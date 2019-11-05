package me.w1992wishes.hbase.coprocessor.observer;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessor;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.coprocessor.RegionObserver;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.wal.WALEdit;

import java.util.List;
import java.util.Optional;

/**
 * @author w1992wishes 2019/11/5 14:37
 */
public class HelloWordObserver implements RegionObserver, RegionCoprocessor {

    private static final String JACK = "jack";

    /**
     * 必须实现这个方法，否则不会生效
     *
     * @return Optional<RegionObserver>
     */
    @Override
    public Optional<RegionObserver> getRegionObserver() {
        return Optional.of(this);
    }

    @Override
    public void prePut(ObserverContext<RegionCoprocessorEnvironment> c, Put put, WALEdit edit,
                       Durability durability) {
        // 获取 mycf:name 单元格
        List<Cell> name = put.get(Bytes.toBytes("mycf"), Bytes.toBytes("name"));
        // 如果该 put 不存在 mycf:name 这个单元格则直接返回
        if (name.isEmpty()) {
            return;
        }
        // 比较 mycf:name 是否为 JACK
        if (JACK.equals(Bytes.toString(CellUtil.cloneValue(name.get(0))))) {
            // 在 mycf:message 中添加一句话
            put.addColumn(Bytes.toBytes("mycf"), Bytes.toBytes("message"), Bytes.toBytes("Hello word! Welcome back!"));
        }
    }

}
