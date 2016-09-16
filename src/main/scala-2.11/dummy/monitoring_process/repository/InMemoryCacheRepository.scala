package dummy.monitoring_process.repository

import akka.actor.{Actor, Props}
import akka.pattern.ask
import akka.util.Timeout
import dummy.monitoring_process.executor.AkkaExecutionContext
import dummy.monitoring_process.model.{Countable, RecognizableModel, SnapshotModel}
import dummy.monitoring_process.repository.InMemoryCacheRepository.{Add, AddList, All, Cacheable, Get}

import scala.concurrent.Future
import scala.concurrent.duration._

object InMemoryCacheRepository {

  type Cacheable = RecognizableModel with Countable

  trait CacheMessage
  case class Add(item: Cacheable) extends CacheMessage
  case class AddList(items: List[Cacheable]) extends CacheMessage
  case class Get(key: String) extends CacheMessage
  case object All extends CacheMessage

  implicit val timeout = Timeout(1 seconds)

  val cache = AkkaExecutionContext.system.actorOf(Props[InMemoryCacheRepository])

  def +(item: Cacheable) = cache ! Add(item)

  def ++(items: List[Cacheable]) = cache ! AddList(items)

  def get(key: String): Future[Option[Cacheable]] = (cache ? Get(key)).mapTo[Option[Cacheable]]

  def getAll(): Future[Map[String, Cacheable]] = (cache ? All).mapTo[Map[String, Cacheable]]

  def recoverCachePartition(cacheModel: SnapshotModel)= InMemoryCacheRepository ++ cacheModel.asCacheableList

}

class InMemoryCacheRepository extends Actor {

  var cache = Map.empty[String, Cacheable]

  def addToCache(item: Cacheable) =
    if (cache contains item.getId) {
      val cacheItem = cache(item.getId)
      cacheItem add item.getCount
      cache = cache + (item.getId -> cacheItem)
    }
    else
      cache = cache + (item.getId -> item)

  override def receive: Receive = {
    case Add(item) => addToCache(item)
    case AddList(items) => items foreach addToCache
    case Get(key) => sender ! cache.get(key)
    case All => sender ! cache
  }
}
