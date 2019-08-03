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

            countService.followedByCount(controller, request, rpcCallback);

            CountCoprocessor.CountResponse response = rpcCallback.get();
            if (controller.failedOnException()) {
                throw controller.getFailedOn();
            }
            return (response != null && response.getCount() != 0) ?
                    response.getCount() : 0;
        };

        Map<byte[], Long> results =
                followed.coprocessorService(
                        CountCoprocessor.CountService.class,
                        startKey,
                        endKey,
                        callable);

        long sum = 0;
        for (Map.Entry<byte[], Long> e : results.entrySet()) {
            sum += e.getValue();
        }

        return sum;
    }

}
