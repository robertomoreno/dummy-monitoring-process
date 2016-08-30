package dummy.monitoring_process.service

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import dummy.monitoring_process.executor.AkkaExecutionContext
import dummy.monitoring_process.model.RequestEventModel
import dummy.monitoring_process.repository.{CassandraRepository, InMemoryCacheRepository}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.read


/**
  * Created by roberto on 26/08/2016.
  */
class KafkaStreamService(properties: KafkaProperties, cassandra: CassandraRepository) {

  import dummy.monitoring_process.model.ModelTransformationImplicits._

  implicit val system = AkkaExecutionContext.system
  implicit val materializer = ActorMaterializer()
  implicit val formats = DefaultFormats

  def runStream() = {
    val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
      .withBootstrapServers(properties.host)
      .withGroupId(properties.group)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

    val subscription = Subscriptions.assignmentWithOffset(new TopicPartition(properties.topic, properties.partitionId) -> properties.offset)

    Consumer.plainSource(consumerSettings, subscription)
      .map{ message => read[RequestEventModel](message.value()) }
      .runForeach { event =>
        InMemoryCacheRepository ++ event.getCacheEvents
        cassandra.saveEvent(event)
      }
  }
}

case class KafkaProperties(host: String, group: String, topic: String, partitionId: Int, offset : Long)
