package dummy.monitoring_process.repository

import akka.actor.{Actor, Props}
import dummy.monitoring_process.executor.AkkaExecutionContext
import dummy.monitoring_process.model.{Countable, RecognizableModel}
import dummy.monitoring_process.repository.InMemoryCacheRepository.{Add, AddList, Cacheable, Get}
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._

import scala.concurrent.Future

object InMemoryCacheRepository {

  implicit val timeout = Timeout(1 seconds)

  type Cacheable = RecognizableModel with Countable

  trait CacheMessage

  case class Add(item: Cacheable) extends CacheMessage

  case class AddList(items: List[Cacheable]) extends CacheMessage

  case class Get(key: String) extends CacheMessage

  val cache = AkkaExecutionContext.system.actorOf(Props[InMemoryCacheRepository])

  def +(item: Cacheable) = cache ! Add(item)

  def ++(items: List[Cacheable]) = cache ! AddList(items)

  def get(key: String): Future[Option[Cacheable]] = (cache ? Get(key)).mapTo[Option[Cacheable]]
}

class InMemoryCacheRepository extends Actor {

  var cache = scala.collection.mutable.Map.empty[String, Cacheable]

  def add(item: Cacheable) =
    if (cache contains item.getId) {
      val cacheItem = cache(item.getId)
      cacheItem add item.getCount
      cache.put(item.getId, cacheItem)
    }
    else
      cache.put(item.getId, item)

  override def receive: Receive = {
    case Add(item) => add(item)
    case AddList(items) => items foreach add
    case Get(key) => sender ! cache.get(key)
  }
}
