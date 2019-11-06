package me.w1992wishes.hbase.coprocessor.endpoint;

import me.w1992wishes.hbase.common.util.Md5Utils;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.client.coprocessor.Batch;
import org.apache.hadoop.hbase.ipc.CoprocessorRpcUtils.BlockingRpcCallback;
import org.apache.hadoop.hbase.ipc.ServerRpcController;
import org.apache.hadoop.hbase.util.Bytes;

import java.util.Arrays;
import java.util.Map;

import static me.w1992wishes.hbase.common.dao.RelationsDAO.FOLLOWED_TABLE_NAME;

/**
 * @author w1992wishes 2019/8/3 11:07
 */
public class RelationCountEndpointClient {

    public long followedByCount(Connection conn, final String userId) throws Throwable {

        Table followed = conn.getTable(TableName.valueOf(FOLLOWED_TABLE_NAME));

        final byte[] startKey = Md5Utils.md5sum(userId);
        final byte[] endKey = Arrays.copyOf(startKey, startKey.length);
        endKey[endKey.length - 1]++;

        final CountCoprocessor.CountRequest request = CountCoprocessor.CountRequest.newBuilder()
                .setStartKey(Bytes.toString(startKey))
                .setEndKey(Bytes.toString(endKey))
                .build();

        Batch.Call<CountCoprocessor.CountService, Long> callable = countService -> {
            ServerRpcController controller = new ServerRpcController();
            BlockingRpcCallback<CountCoprocessor.CountResponse> rpcCallback = new BlockingRpcCallback<>();

            // 调用终端程序方法
            countService.followedByCount(controller, request, rpcCallback);

            // 取出 response
            CountCoprocessor.CountResponse response = rpcCallback.get();
            if (controller.failedOnException()) {
                throw controller.getFailedOn();
            }

            // 由于自定义的变量为 count，所以 protobuf 会自动生成一个 hasCount 方法来判断是否变量有值
            return response.hasCount() ? response.getCount() : 0;
        };

        // 这里触发调用终端程序
        Map<byte[], Long> results =
                followed.coprocessorService(
                        CountCoprocessor.CountService.class,
                        startKey,
                        endKey,
                        callable);

        // 对各个 region 的终端结果求和
        long sum = 0;
        for (Map.Entry<byte[], Long> e : results.entrySet()) {
            sum += e.getValue();
        }

        return sum;
    }

}
