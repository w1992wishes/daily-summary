package me.w1992wishes.my.common.util

import java.util.concurrent.Future

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord, RecordMetadata}

/**
  * 使用 懒加载 创建 Kafka Sender
 *
  * @param createProducer 构造 KafkaProducer
  * @tparam K key
  * @tparam V value
  */
class KafkaSender[K, V](createProducer: () => KafkaProducer[K, V]) extends Serializable {
  lazy val producer = createProducer()

  def send(topic: String, key: K, value: V): Future[RecordMetadata] = {
    producer.send(new ProducerRecord[K, V](topic, key, value))
  }

  def send(topic: String, value: V): Future[RecordMetadata] = {
    producer.send(new ProducerRecord[K, V](topic, value))
  }
}

object KafkaSender {

  import scala.collection.JavaConversions._

  def apply[K, V](config: Map[String, Object]): KafkaSender[K, V] = {
    val createProducerFunc = () => {
      val producer = new KafkaProducer[K, V](config)
      sys.addShutdownHook {
        producer.close()
      }
      producer
    }
    new KafkaSender(createProducerFunc)
  }

  def apply[K, V](config: java.util.Properties): KafkaSender[K, V] = apply(config.toMap)
}