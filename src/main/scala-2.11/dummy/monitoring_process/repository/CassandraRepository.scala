package dummy.monitoring_process.repository

import akka.Done
import com.datastax.driver.core.{Cluster, ResultSetFuture}
import dummy.monitoring_process.executor.AkkaExecutionContext
import dummy.monitoring_process.model.RequestEventModel
import dummy.monitoring_process.repository.InMemoryCacheRepository.Cacheable
import dummy.monitoring_process.model.ModelTransformationImplicits._
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization._

import scala.concurrent.Future

class CassandraRepository(host: String) {

  val cluster = Cluster.builder().addContactPoint(host).build()
  val session = cluster.connect()
  implicit val akkaExec = AkkaExecutionContext.system.dispatcher
  implicit val formats = DefaultFormats

  val preparedInsert = session.prepare("insert into statistics.request ( entityid, userid,apiid,responsecode) values (?, ?, ?, ?)")

  def getSnapshot(partitionId: String) = {

    val rs = session.execute(s"select data, partitionoffset from statistics.entity_snapshot where partitionid = $partitionId")

    val row = rs.one()

    if(row != null)
      Some( StatisticsSnapshot(
        row.getString("data"),
        row.getLong("partitionoffset")))
    else
      None
  }

  def saveEvent(event: RequestEventModel) = {
    val bounded = preparedInsert.bind(event.entityId,event.userId,event.apiId,event.responseCode)
    session.executeAsync(bounded)
  }

  def saveSnapshot(cache : Map[String, Cacheable]) : ResultSetFuture = {
    val snapshotModel = cache.toSnapshotModel
    val snapshotModelJson : String = write(snapshotModel)
    session.executeAsync(preparedInsert.bind())
  }

  def terminateSession = if (cluster != null) cluster.close
}

case class StatisticsSnapshot(val data: String, val partitionOffset: Long)
