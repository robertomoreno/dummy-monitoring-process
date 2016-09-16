package dummy.monitoring_process.service

import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import com.datastax.driver.core.ResultSet
import dummy.monitoring_process.executor.AkkaExecutionContext
import dummy.monitoring_process.model.RequestEventModel
import dummy.monitoring_process.repository.{CassandraRepository, InMemoryCacheRepository}
import dummy.monitoring_process.utils.MonitoringUtils._
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.read
import scala.concurrent.duration._
import scala.concurrent.Future
import dummy.monitoring_process.model.ModelTransformationImplicits._

class KafkaStreamService(properties: KafkaProperties, cassandra: CassandraRepository) {

  implicit val system = AkkaExecutionContext.system
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()
  implicit val formats = DefaultFormats

  def runStream() = {

    val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
      .withBootstrapServers(properties.host)
      .withGroupId(properties.group)

    val subscription = Subscriptions.assignmentWithOffset(new TopicPartition(properties.topic, properties.partitionId) -> properties.offset)

    Consumer.plainSource(consumerSettings, subscription)
      .map { message => (message, read[RequestEventModel](message.value)) }
      .mapAsync(1) { event => persistEvent(event._2).map(rs => event) }
      .runForeach { event => InMemoryCacheRepository ++ event._2.getCacheEvents }

    createSchedulerForCachePersistence()
  }

  /**
    * Makes 2 retries before fail
    *
    * @param event
    * @param retries
    * @return
    */
  def persistEvent(event: RequestEventModel, retries: Int = 0): Future[ResultSet] =
  cassandra.saveEvent(event).asAkkaFuture
    .recoverWith {
      case _ if retries < 3 => persistEvent(event, retries + 1)
      case error => throw error
    }

  def createSchedulerForCachePersistence() =
    system.scheduler.schedule(20 second, 20 second)(InMemoryCacheRepository.getAll().foreach(cassandra saveSnapshot _))

}

case class KafkaProperties(host: String, group: String, topic: String, partitionId: Int, offset: Long)



