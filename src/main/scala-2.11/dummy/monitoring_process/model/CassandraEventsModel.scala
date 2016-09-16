package dummy.monitoring_process.model

import dummy.monitoring_process.repository.InMemoryCacheRepository.Cacheable

trait RequestEventMessage

case class RequestEventModel(entityId: String, userId: String, apiId: String, responseCode: String) extends RequestEventMessage

case class RequestEventRepited(event: RequestEventModel, times: Int) extends RequestEventMessage

case class SnapshotModel(
                          entities: List[EntityModel] = List.empty,
                          entitiesUsers: List[EntityUserModel] = List.empty,
                          entitiesUsersApis: List[EntityUserApiModel] = List.empty,
                          entitiesUsersApisResponses: List[EntityUserApiResponseModel] = List.empty
                        ) {

  def +(cacheModel: Cacheable) =
    cacheModel match {
      case model: EntityModel =>
        SnapshotModel(model :: entities, entitiesUsers, entitiesUsersApis, entitiesUsersApisResponses)
      case model: EntityUserModel =>
        SnapshotModel(entities, model :: entitiesUsers, entitiesUsersApis, entitiesUsersApisResponses)
      case model: EntityUserApiModel =>
        SnapshotModel(entities, entitiesUsers, model :: entitiesUsersApis, entitiesUsersApisResponses)
      case model: EntityUserApiResponseModel =>
        SnapshotModel(entities, entitiesUsers, entitiesUsersApis, model :: entitiesUsersApisResponses)
    }

  def asCacheableList = entities ::: entitiesUsers ::: entitiesUsersApis ::: entitiesUsersApisResponses

}