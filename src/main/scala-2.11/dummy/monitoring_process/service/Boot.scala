package dummy.monitoring_process.service

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream._
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Partition, Source}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import GraphDSL.Implicits._

import scala.concurrent.Future

object Boot {

  implicit val system = ActorSystem("monitoringSystem")
  implicit val executor = system.dispatcher
  val decider: Supervision.Decider = {
    case error: Exception => error.printStackTrace(); Supervision.resume
  }
  implicit val materialize = ActorMaterializer(ActorMaterializerSettings(system).withSupervisionStrategy(decider))

  val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
    .withBootstrapServers("localhost:9092")
    .withGroupId("group1")
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  val subscription = Subscriptions.assignmentWithOffset(new TopicPartition("test", 0) -> 0L)

  val saveGraph = Flow.fromGraph(GraphDSL.create() { implicit b =>

    // 1 error flow, 0 ok flow
    val broadcast = b.add(Partition[Message](2, message => if (message.isInstanceOf[ErrorRetry]) 1 else 0))
    val merge = b.add(Merge[Message](2))

    merge.out ~> Flow[Message].mapAsync(1)(errorFuture) ~> broadcast.in
    merge.in(1) <~ broadcast.out(1)

    // expose ports
    FlowShape(merge.in(0), broadcast.out(0))
  })

  def main(args: Array[String]): Unit = {
    Consumer.plainSource(consumerSettings, subscription)
      .map(m => new Message(m.value()))
    //Source(List("1", "2", "dds", "32", "111", "22", "3"))
      // .map(new Message(_))
      .via(saveGraph)
      .runForeach(println)
  }

  def errorFuture(message: Message): Future[Message] = Future(new Message(message.value.toInt.toString)).recover {
    case error =>
      message match {
        case ErrorRetry(message, times) if times > 2 => throw new Exception("To many retries for " + message.value)
        case ErrorRetry(message, times) => println(s"$message, times: $times"); ErrorRetry(message, times + 1)
        case m: Message => ErrorRetry(m, 1)
      }
  }
}

class Message(val value: String) {

  override def toString = s"Message($value)"
}

case class ErrorRetry(message: Message, times: Int) extends Message(message.value)
