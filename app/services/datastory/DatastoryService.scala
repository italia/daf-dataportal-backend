package services.datastory

import ftd_api.yaml.{Datastory, Error, Success}
import play.api.libs.ws.WSClient
import play.api.{Configuration, Environment}
import repositories.datastory.{DatastoryRepository, DatastoryRepositoryComponent}

import scala.concurrent.Future

trait DatastoryServiceComponent {
  this: DatastoryRepositoryComponent =>
  val datastoryService: DatastoryService

  class DatastoryService {

    def saveDatastory(user: String, datastory: Datastory, token: String, ws: WSClient): Future[Either[Error, Success]] = {
      datastoryRepository.saveDatastory(user, datastory, token, ws)
    }

    def deleteDatastory(id: String, user: String): Future[Either[Error, Success]] = {
      datastoryRepository.deleteDatastory(id, user)
    }

    def getAllDatastory(user: String, groups: List[String], limit: Option[Int], status: Option[Int]): Future[Either[Error, Seq[Datastory]]] = {
      datastoryRepository.getAllDatastory(user, groups, limit, status)
    }

    def getAllPublicDatastory(limit: Option[Int]): Future[Either[Error, Seq[Datastory]]] = {
      datastoryRepository.getAllPublicDatastory(limit)
    }

    def getDatastoryById(id: String, user: String, group: List[String]): Future[Either[Error, Datastory]] = {
      datastoryRepository.getDatastoryById(id, user, group)
    }

    def getPublicDatastoryById(id: String): Future[Either[Error, Datastory]] = {
      datastoryRepository.getPublicDatastoryById(id)
    }

  }

}


object DatastoryRegistry extends DatastoryServiceComponent with DatastoryRepositoryComponent {
  val conf = Configuration.load(Environment.simple())
  val app: String = conf.getString("app.type").getOrElse("dev")
  val datastoryRepository =  DatastoryRepository(app)
  val datastoryService = new DatastoryService
}
