package dummy.monitoring_process.executor

import akka.actor.ActorSystem

/**
  * Created by roberto on 29/08/2016.
  */
object AkkaExecutionContext {

  val system = ActorSystem("monitoringSystem")
}
