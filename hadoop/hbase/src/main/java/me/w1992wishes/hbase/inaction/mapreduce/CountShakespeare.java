package me.w1992wishes.hbase.inaction.mapreduce;

import me.w1992wishes.hbase.inaction.hbase.TwitsDAO;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.NullOutputFormat;

import java.util.Random;

/**
 * 莎士比亚作品计数示例
 *
 * @author wanqinfeng
 * @date 2019/7/7 14:41.
 */
public class CountShakespeare {

    public static class Map
            extends TableMapper<Text, LongWritable> {

        public static enum Counters {ROWS, SHAKESPEAREAN};

        private Random rand;

        /**
         * Determines if the message pertains to Shakespeare.
         */
        private boolean containsShakespeare(String msg) {
            return rand.nextBoolean();
        }

        @Override
        protected void setup(Context context) {
            rand = new Random(System.currentTimeMillis());
        }

        @Override
        protected void map(
                ImmutableBytesWritable rowkey,
                Result result,
                Context context) {
            byte[] b = result.getColumnLatestCell(
                    TwitsDAO.TWITS_FAM,
                    TwitsDAO.TWIT_COL).getValueArray();
            if (b == null) {
                return;
            }

            String msg = Bytes.toString(b);
            if (msg.isEmpty()) {
                return;
            }

            context.getCounter(Counters.ROWS).increment(1);
            if (containsShakespeare(msg)) {
                context.getCounter(Counters.SHAKESPEAREAN).increment(1);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = HBaseConfiguration.create();
        Job job = Job.getInstance(conf, "TwitBase Shakespeare counter");
        job.setJarByClass(CountShakespeare.class);

        Scan scan = new Scan();
        scan.addColumn(TwitsDAO.TWITS_FAM, TwitsDAO.TWIT_COL);
        TableMapReduceUtil.initTableMapperJob(
                Bytes.toString(TwitsDAO.TABLE_NAME),
                scan,
                Map.class,
                ImmutableBytesWritable.class,
                Result.class,
                job);

        job.setOutputFormatClass(NullOutputFormat.class);
        job.setNumReduceTasks(0);
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }

}
