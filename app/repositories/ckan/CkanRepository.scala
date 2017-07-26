package repositories.ckan

import ftd_api.yaml.{Credentials, Dataset, DistributionLabel, Organization, ResourceSize, User}
import play.api.libs.json.{JsResult, JsValue}

import scala.concurrent.Future

/**
  * Created by ale on 01/07/17.
  */
trait CkanRepository {

  def getMongoUser(name:String): JsResult[User]
  def verifyCredentials(credentials: Credentials):Boolean
  def createDataset(jsonDataset: JsValue): Future[String]
  def createOrganization(jsonDataset: JsValue): Future[String]
  def updateOrganization(orgId: String, jsonOrg: JsValue): Future[String]
  def createUser(jsonDataset: JsValue): Future[String]
  def dataset(datasetId: String): JsValue
  def getOrganization(orgId :String) : Future[JsResult[Organization]]
  def getOrganizations() : Future[JsValue]
  def getUserOrganizations(userName :String) : Future[JsResult[Seq[Organization]]]
  def getDatasets() : Future[JsValue]
  def searchDatasets( input: (DistributionLabel, DistributionLabel, ResourceSize) ) : Future[JsResult[Seq[Dataset]]]
  def getDatasetsWithRes( input: (ResourceSize, ResourceSize) ) : Future[JsResult[Seq[Dataset]]]
  def testDataset(datasetId :String) : Future[JsResult[Dataset]]

}

object CkanRepository {
  def apply(config: String): CkanRepository = config match {
    case "dev" => new CkanRepositoryDev
    case "prod" => new CkanRepositoryProd
  }
}

trait CkanRepositoryComponent {
  val ckanRepository :CkanRepository
}