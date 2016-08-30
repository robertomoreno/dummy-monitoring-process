package dummy.monitoring_process.model

trait RequestEventMessage

case class RequestEventModel(entityId: String, userId: String, apiId: String, responseCode: String) extends RequestEventMessage

case class RequestEventRepited(event: RequestEventModel, times: Int) extends  RequestEventMessage

case class SnapshotModel(
                          entities: List[EntityModel],
                          entitiesUsers: List[EntityUserModel],
                          entitiesUsersApis: List[EntityUserApiModel],
                          entitiesUsersApisResponses: List[EntityUserApiResponseModel]
                        )