package repositories.ckan

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.libs.json.{JsError, JsString, JsSuccess, JsValue}
import play.api.libs.ws.WSResponse
import play.api.libs.ws.ahc.AhcWSClient
import utils.ConfigReader
import utils.it.gov.daf.catalogmanager.utilities.WebServiceUtil

import scala.concurrent.Future

/**
  * Created by ale on 01/07/17.
  */
class CkanRepositoryProd  extends CkanRepository{

  def createDataset(jsonDataset: JsValue): Unit = println("TODO")
  def dataset(datasetId: String): JsValue = JsString("TODO")

}
