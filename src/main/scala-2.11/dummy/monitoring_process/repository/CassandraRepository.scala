package dummy.monitoring_process.repository

import akka.Done
import com.datastax.driver.core.Cluster
import dummy.monitoring_process.executor.AkkaExecutionContext
import dummy.monitoring_process.model.RequestEventModel

import scala.concurrent.Future

class CassandraRepository(host: String) {

  val cluster = Cluster.builder().addContactPoint(host).build()
  val session = cluster.connect()
  implicit val akkaExec = AkkaExecutionContext.system.dispatcher

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

  def saveEvent(requestEventModel: RequestEventModel) = Future(Done)

  def terminateSession = if (cluster != null) cluster.close
}

case class StatisticsSnapshot(val data: String, val partitionOffset: Long)
