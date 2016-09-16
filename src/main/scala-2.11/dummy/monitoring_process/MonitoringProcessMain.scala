package dummy.monitoring_process


import dummy.monitoring_process.model.ModelTransformationImplicits._
import dummy.monitoring_process.repository.{CassandraRepository, InMemoryCacheRepository, StatisticsSnapshot}
import dummy.monitoring_process.service.{KafkaProperties, KafkaStreamService}

object MonitoringProcessMain {

  def main(args: Array[String]): Unit = {

    val kafkaHost = "localhost:9092"
    val kafkaGroup = "group1"
    val kafkaTopic = "test"
    val kafkaPartitionId = "1"

    implicit val cassandraRepository = new CassandraRepository("localhost")

    val kafkaOffset = cassandraRepository.getSnapshot(kafkaPartitionId) match {
      case None => 0L
      case Some(snapshot: StatisticsSnapshot) =>
        InMemoryCacheRepository recoverCachePartition snapshot.toCacheModel
        snapshot.partitionOffset
    }
    val kafkaProperties = KafkaProperties(kafkaHost, kafkaGroup, kafkaTopic, kafkaPartitionId.toInt, kafkaOffset)
    val kafkaStreamService = new KafkaStreamService(kafkaProperties, cassandraRepository)

    kafkaStreamService.runStream()
  }
}
