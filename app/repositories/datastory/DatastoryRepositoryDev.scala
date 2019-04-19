package repositories.datastory
import ftd_api.yaml.{Datastory, Success, Error}

import scala.concurrent.Future

class DatastoryRepositoryDev extends DatastoryRepository {

  override def saveDatastory(user: String, datastory: Datastory): Future[Either[Error, Success]] = Future.successful(Right(Success(Some("save!"), None)))

  override def deleteDatastory(id: String, user: String): Future[Either[Error, Success]] = Future.successful(Right(Success(Some("delete!"), None)))

  override def getAllDatastory(user: String, groups: List[String], limit: Option[Int], status: Option[Int]): Future[Either[Error, Seq[Datastory]]] = Future.successful(Right(List[Datastory]()))

  override def getAllPublicDatastory(limit: Option[Int]): Future[Either[Error, Seq[Datastory]]] = Future.successful(Right(List[Datastory]()))

  override def getDatastoryById(id: String, user: String, group: List[String]): Future[Either[Error, Datastory]] = {
    Future.successful(Left(Error(Some(404), None, None)))
  }

  override def getPublicDatastoryById(id: String): Future[Either[Error, Datastory]] = {
    Future.successful(Left(Error(Some(404), None, None)))
  }
}
