package dummy.monitoring_process.model

/**
  * Created by roberto on 29/08/2016.
  */
object ModelTransformationImplicits {

  implicit class CassandraToCache(event : RequestEventModel) {

    def getCacheEvents = List(
      EntityModel(event.entityId),
      EntityUserModel(event.entityId, event.userId),
      EntityUserApiModel(event.entityId, event.userId, event.apiId),
      EntityUserApiResponseModel(event.entityId, event.userId, event.apiId, event.responseCode)
    )
  }

}
