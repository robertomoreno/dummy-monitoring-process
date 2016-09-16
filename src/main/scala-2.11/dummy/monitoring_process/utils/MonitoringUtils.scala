package dummy.monitoring_process.utils

import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture}

import scala.concurrent.{Future, Promise}


object MonitoringUtils {

  implicit class ParseFuture[T](lf: ListenableFuture[T]) {
    def asAkkaFuture: Future[T] = {
      val p = Promise[T]()
      Futures.addCallback(lf, new FutureCallback[T] {
        def onFailure(t: Throwable): Unit = p failure t
        def onSuccess(result: T): Unit    = p success result
      })
      p.future
    }
  }
}
