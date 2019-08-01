package me.w1992wishes.hbase.inaction.coprocessors;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.google.protobuf.Service;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.coprocessor.CoprocessorException;
import org.apache.hadoop.hbase.coprocessor.CoprocessorService;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.filter.PrefixFilter;
import org.apache.hadoop.hbase.regionserver.InternalScanner;
import org.apache.hadoop.hbase.shaded.protobuf.ResponseConverter;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static me.w1992wishes.hbase.inaction.hbase.RelationsDAO.FROM;
import static me.w1992wishes.hbase.inaction.hbase.RelationsDAO.RELATION_FAM;

/**
 * 说明：hbase 协处理器 Endpooint 的服务端代码
 * 功能：继承通过 protocol buffer 生成的 rpc 接口，在服务端获取指定列的数据后进行求和操作，最后将结果返回客户端
 *
 * @author w1992wishes 2019/8/1 16:58
 */
public class RelationCountEndpoint extends CountCoprocessor.CountService implements Coprocessor, CoprocessorService {

    private RegionCoprocessorEnvironment env;

    @Override
    public Service getService() {
        return this;
    }

    @Override
    public void start(CoprocessorEnvironment env) throws IOException {
        if (env instanceof RegionCoprocessorEnvironment) {
            this.env = (RegionCoprocessorEnvironment) env;
        } else {
            throw new CoprocessorException("Must be loaded on a table region!");
        }
    }

    @Override
    public void stop(CoprocessorEnvironment env) throws IOException {
        // do nothing
    }

    @Override
    public void followedByCount(RpcController controller, CountCoprocessor.CountRequest request, RpcCallback<CountCoprocessor.CountResponse> done) {
        Scan scan = new Scan();
        byte[] startKey = Bytes.toBytes(request.getStartKey());
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
                // count 个数
                hasMore = scanner.next(results);
                sum += results.size();
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
