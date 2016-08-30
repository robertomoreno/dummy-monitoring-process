package dummy.monitoring_process.model

trait RecognizableModel {
  def getId: String
}

trait Countable {
  var count: Long

  def add(i: Long): Unit = count += i

  def getCount = count
}

case class EntityModel(entityId: String, var count: Long = 1) extends RecognizableModel with Countable {

  override def getId: String = entityId
}

case class EntityUserModel(entityId: String, userId: String, var count: Long = 1) extends RecognizableModel with Countable {

  override def getId: String = entityId + "_" + userId
}

case class EntityUserApiModel(entityId: String, userId: String, apiId: String, var count: Long = 1) extends RecognizableModel with Countable {

  override def getId: String = entityId + "_" + userId + "_" + apiId
}

case class EntityUserApiResponseModel(entityId: String, userId: String, apiId: String, responseCode: String, var count: Long = 1) extends RecognizableModel with Countable {

  override def getId: String = entityId + "_" + userId + "_" + apiId + "_" + responseCode
}
