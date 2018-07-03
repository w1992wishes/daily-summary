package me.w1992wishes.study.spark.wordcount;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import scala.Tuple2;

import java.util.Arrays;
import java.util.List;

/**
 * @Author: w1992wishes
 * @Date: 2018/4/17 16:30
 */
public class WordCountSpark {
    public static void main(String[] args) {
        WordCountSpark wordCountSpark = new WordCountSpark();
        wordCountSpark.count();
    }

    private void count(){
        //1.本地模式，创建spark配置及上下文
        SparkConf sparkConf = new SparkConf().setMaster("local").setAppName("WordCount");
        JavaSparkContext sc = new JavaSparkContext(sparkConf);

        //2.读取本地文件,并创建RDD
        JavaRDD<String> linesRDD = sc.textFile("e:/words.txt");

        //3.每个单词由空格隔开,将每行的linesRDD拆分为每个单词的RDD
        JavaRDD<String> wordsRDD = linesRDD.flatMap(s  -> Arrays.asList(s.split("\\s")).iterator());

        //4.将每个单词转为key-value的RDD，并给每个单词计数为1
        JavaPairRDD<String,Integer> wordsPairRDD = wordsRDD.mapToPair(s -> new Tuple2<String,Integer>(s, 1));

        //5.计算每个单词出现的次数
        JavaPairRDD<String,Integer> wordsCountRDD = wordsPairRDD.reduceByKey((a,b) -> a+b);

        //6.因为只能对key进行排序，所以需要将wordsCountRDD进行key-value倒置，返回新的RDD
        JavaPairRDD<Integer,String> wordsCountRDD2 = wordsCountRDD.mapToPair(s -> new Tuple2<Integer,String>(s._2, s._1));

        //7.对wordsCountRDD2进行排序,降序desc
        JavaPairRDD<Integer,String> wordsCountRDD3 = wordsCountRDD2.sortByKey(false);

        //8.只取前10个
        List<Tuple2<Integer, String>> result = wordsCountRDD3.take(10);

        //9.打印
        result.forEach(t -> System.out.println(t._2 + "   " + t._1));

        sc.close();
    }
}
