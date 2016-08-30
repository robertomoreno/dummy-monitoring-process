package dummy.monitoring_process


import dummy.monitoring_process.model.SnapshotModel
import dummy.monitoring_process.repository.{CassandraRepository, InMemoryCacheRepository, StatisticsSnapshot}
import dummy.monitoring_process.service.{KafkaProperties, KafkaStreamService}
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.read

object MonitoringProcessMain {

  val cache : InMemoryCacheRepository = new InMemoryCacheRepository

  def main(args: Array[String]): Unit = {

    val kafkaHost = "localhost:9092"
    val kafkaGroup = "group1"
    val kafkaTopic = "test"
    val kafkaPartitionId = "1"

    val cassandraRepository = new CassandraRepository("localhost")
    val kafkaOffset =
      cassandraRepository.getSnapshot(kafkaPartitionId) match {
        case None => 0
        case Some(StatisticsSnapshot(data, offset)) =>
          restoreCache(data)
          offset
      }

    val kafkaProperties = KafkaProperties(kafkaHost, kafkaGroup, kafkaTopic, kafkaPartitionId.toInt, kafkaOffset)
    val kafkaStreamService: KafkaStreamService = new KafkaStreamService(kafkaProperties,cassandraRepository)
    kafkaStreamService.runStream()
  }

  def restoreCache(json: String) = {
    implicit val formats = DefaultFormats
    val snapshot: SnapshotModel = read(json)

    InMemoryCacheRepository ++ snapshot.entities
    InMemoryCacheRepository ++ snapshot.entitiesUsers
    InMemoryCacheRepository ++ snapshot.entitiesUsersApis
    InMemoryCacheRepository ++ snapshot.entitiesUsersApisResponses
  }


}
