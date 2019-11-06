package me.w1992wishes.hbase.coprocessor.endpoint;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.google.protobuf.Service;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CoprocessorEnvironment;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.coprocessor.CoprocessorException;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessor;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.filter.PrefixFilter;
import org.apache.hadoop.hbase.regionserver.InternalScanner;
import org.apache.hadoop.hbase.shaded.protobuf.ResponseConverter;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static me.w1992wishes.hbase.common.dao.RelationsDAO.FROM;
import static me.w1992wishes.hbase.common.dao.RelationsDAO.RELATION_FAM;

/**
 * 说明：hbase 协处理器 Endpooint 的服务端代码
 * 功能：继承通过 protocol buffer 生成的 rpc 接口，在服务端获取指定列的数据后进行求和操作，最后将结果返回客户端
 *
 * @author w1992wishes 2019/8/1 16:58
 */
public class RelationCountEndpoint extends CountCoprocessor.CountService implements RegionCoprocessor {

    private static final Logger LOG = LoggerFactory.getLogger(RelationCountEndpoint.class);

    private RegionCoprocessorEnvironment env;

    @Override
    public Iterable<Service> getServices() {
        return Collections.singleton(this);
    }

    @Override
    public void start(CoprocessorEnvironment env) throws IOException {
        if (env instanceof RegionCoprocessorEnvironment) {
            this.env = (RegionCoprocessorEnvironment) env;
            LOG.info("****** {} start. ******", this.getClass().getName());
        } else {
            LOG.warn("****** Must be loaded on a table region .******");
            throw new CoprocessorException("Must be loaded on a table region!");
        }
    }

    @Override
    public void stop(CoprocessorEnvironment env) throws IOException {
        LOG.info("****** {} stop. ******", this.getClass().getName());
    }

    @Override
    public void followedByCount(RpcController controller, CountCoprocessor.CountRequest request, RpcCallback<CountCoprocessor.CountResponse> done) {
        Scan scan = new Scan();
        byte[] startKey = Bytes.toBytes(request.getStartKey());
        LOG.info("****** startKey {}. ******", request.getStartKey());
        scan.withStartRow(startKey);
        scan.setFilter(new PrefixFilter(startKey));
        scan.addColumn(RELATION_FAM, FROM);
        scan.readVersions(1);

        CountCoprocessor.CountResponse response = null;

        try (InternalScanner scanner = env.getRegion().getScanner(scan)) {
            List<Cell> results = new ArrayList<>();
            boolean hasMore;
            long sum = 0L;

            do {
                // 获取下一批结果，放入 result 中
                hasMore = scanner.next(results);
                sum += results.size();
                // 两次循环之间清空本地结果缓存
                results.clear();

                // 累加
                /*hasMore = scanner.next(results);
                for (Cell cell : results) {
                    sum = sum + Bytes.toLong(CellUtil.cloneValue(cell));
                }
                results.clear();*/
            } while (hasMore);

            // 设置返回结果
            response = CountCoprocessor.CountResponse.newBuilder().setCount(sum).build();
        } catch (IOException ioe) {
            ResponseConverter.setControllerException(controller, ioe);
        }

        // 将rpc结果返回给客户端
        done.run(response);
    }
}
