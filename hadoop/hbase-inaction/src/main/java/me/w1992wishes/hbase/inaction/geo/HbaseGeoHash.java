package me.w1992wishes.hbase.inaction.geo;

import ch.hsr.geohash.GeoHash;
import com.google.common.collect.MinMaxPriorityQueue;
import me.w1992wishes.hbase.common.util.HBaseUtils;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.PrefixFilter;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;

/**
 * 最近邻位置
 *
 * @author w1992wishes 2019/11/20 10:09
 */
public class HbaseGeoHash {

    private static final byte[] FAMILY = "FAMILY".getBytes();
    private static final byte[] ID = "id".getBytes();
    private static final byte[] X_COL = "x_col".getBytes();
    private static final byte[] Y_COL = "y_col".getBytes();

    public Collection<QueryMatch> query(double lat, double lon, int n) throws IOException {
        DistanceComparator comp = new DistanceComparator(lon ,lat);
        Collection<QueryMatch> ret = MinMaxPriorityQueue.orderedBy(comp).maximumSize(n).create();

        GeoHash target = GeoHash.withCharacterPrecision(lat, lon,7);
        ret.addAll(takeN(comp, target.toBase32(), n));
        // 所有邻居同样处理
        for (GeoHash h : target.getAdjacent()) {
            ret.addAll(takeN(comp, h.toBase32(), n));
        }
        return ret;
    }

    Collection<QueryMatch> takeN(Comparator<QueryMatch> comp, String prefix, int n) throws IOException {
        Collection<QueryMatch> candidates = MinMaxPriorityQueue.orderedBy(comp).maximumSize(n).create();

        Scan scan = new Scan().withStartRow(prefix.getBytes());
        scan.setFilter(new PrefixFilter(prefix.getBytes()));

        scan.addFamily(FAMILY);
        scan.readVersions(1);
        // 设置缓存，减少 RPC 调用
        scan.setCaching(50);

        Connection conn = HBaseUtils.getCon();
        Table table = conn.getTable(TableName.valueOf("wifi".getBytes()));

        ResultScanner scanner = table.getScanner(scan);
        for (Result r : scanner) {
            String hash = new String(r.getRow());
            String id = new String(r.getValue(FAMILY, ID));
            double lon = Bytes.toDouble(r.getValue(FAMILY, X_COL));
            double lan = Bytes.toDouble(r.getValue(FAMILY, Y_COL));
            // 收集候选位置
            candidates.add(new QueryMatch(id, hash, lon, lan));
        }
        table.close();
        return candidates;
    }

}
