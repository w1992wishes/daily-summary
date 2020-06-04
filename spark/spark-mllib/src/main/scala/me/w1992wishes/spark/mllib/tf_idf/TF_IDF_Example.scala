package me.w1992wishes.spark.mllib.tf_idf

import org.apache.spark.ml.feature.{CountVectorizer, CountVectorizerModel, IDFModel, RegexTokenizer}
import org.apache.spark.ml.linalg.SparseVector
import org.apache.spark.sql.{DataFrame, SparkSession}

/**
  * @author w1992wishes 2020/6/4 15:49
  */
object TF_IDF_Example extends App {
  val spark = SparkSession.builder().master("local[8]").appName(getClass.getSimpleName).getOrCreate()

  import org.apache.spark.ml.feature.{HashingTF, IDF, Tokenizer}

  val sentenceData = spark.createDataFrame(Seq(
    (0.0, "Hi I heard about Spark,I love spark"),
    (0.0, "I wish Java could use case classes"),
    (1.0, "Logistic regression models are neat,I like it")
  )).toDF("label", "sentence")

  // 分词
  val tokenizer = new RegexTokenizer().setInputCol("sentence").setOutputCol("words")
    .setPattern("\\W").setToLowercase(true)
  val wordsData = tokenizer.transform(sentenceData)
  wordsData.show(false)

  // hash 向量化，hash 桶的数量少容易出现 hash 碰撞
  val hashingTF = new HashingTF().setInputCol("words").setOutputCol("rowFeatures").setNumFeatures(2000)
  val featurizedData: DataFrame = hashingTF.transform(wordsData)
  // alternatively, CountVectorizer can also be used to get term frequency vectors
  featurizedData.show(false)

  /*  // 使用CountVectorizer生成特征向量
    val cvModel: CountVectorizerModel =new CountVectorizer()
      .setInputCol("words")
      .setOutputCol("rowFeatures")
      .setMinTF(2)
      .fit(wordsData)
    val featurizedData = cvModel.transform(wordsData)
    //输出词表
    val vocabulary = cvModel.vocabulary
    // i,spark,it,hi,about,neat,wish,could,regression,classes,like,java,case,models,are,love,heard,logistic,use
    println(vocabulary.mkString(","))
    //(0,i)
    //(5,heard)
    //(10,case)
    //(14,could)
    //(1,spark)
    //(6,wish)
    //(9,like)
    //(13,use)
    //(2,it)
    //(17,java)
    //(12,logistic)
    //(7,are)
    //(3,hi)
    //(18,models)
    //(16,neat)
    //(11,classes)
    //(8,love)
    //(4,about)
    //(15,regression)
    vocabulary.zipWithIndex.map(row => {(row._2, row._1)}).toMap.foreach(println(_))*/

  // IDF 是一个评估器，需要使用 fit 进行转换，生成模型
  val idf = new IDF().setInputCol("rowFeatures").setOutputCol("features")
  val model: IDFModel = idf.fit(featurizedData)

  // IDF 减少那些在语料库中出现频率较高的词的权重
  // 通过 TF-IDF 转换后得到带权重的向量值
  val rescaleData = model.transform(featurizedData)
  rescaleData.select("words", "rowFeatures", "features").show(false)

  /*  rescaleData.rdd.map(row => {
      val vec = row.getAs[SparseVector]("features").toSparse
      val values = vec.values
      // 286
      //240
      //213
      //1105
      //495
      //1357
      //342
      //1777
      //695
      //1138
      //1193
      //1330
      //1960
      //489
      //1604
      //495
      //1809
      //1967
      vec.indices.foreach(println(_))
      //L1 范数做归一化
      val powsum = values.sum
      //.map(v => Math.pow(v, 2)).sum
      val newValues = values.map(v => {
        println(v, v / powsum)
        (v, v / powsum)
      })
    }).count()*/

  spark.stop()
}
