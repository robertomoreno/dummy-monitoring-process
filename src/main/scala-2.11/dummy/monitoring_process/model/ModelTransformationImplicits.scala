package dummy.monitoring_process.model

import dummy.monitoring_process.repository.InMemoryCacheRepository.Cacheable
import dummy.monitoring_process.repository.StatisticsSnapshot
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization._

object ModelTransformationImplicits {

  implicit class CassandraToCache(event: RequestEventModel) {

    def getCacheEvents = List(
      EntityModel(event.entityId),
      EntityUserModel(event.entityId, event.userId),
      EntityUserApiModel(event.entityId, event.userId, event.apiId),
      EntityUserApiResponseModel(event.entityId, event.userId, event.apiId, event.responseCode)
    )
  }

  implicit class SnapshotToCache(snapshot: StatisticsSnapshot) {

    def toCacheModel = {
      implicit val formats = DefaultFormats
      read[SnapshotModel](snapshot.data)
    }
  }

  implicit class CacheToSnapshot(cache: Map[String, Cacheable]) {

    def toSnapshotModel: StatisticsSnapshot = {
      val snapshotModel = cache.foldLeft(SnapshotModel()) { (acc, current) => acc + current._2 }
      StatisticsSnapshot("",0)
    }
  }

}
