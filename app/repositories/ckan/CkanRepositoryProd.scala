package repositories.ckan

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import ftd_api.yaml.Dataset
import play.api.libs.json.{JsError, JsString, JsSuccess, JsValue}
import play.api.libs.ws.ahc.AhcWSClient
import utils.ConfigReader
import utils.it.gov.daf.catalogmanager.utilities.WebServiceUtil

import scala.concurrent.Future

/**
  * Created by ale on 01/07/17.
  */
class CkanRepositoryProd  extends CkanRepository{

  import scala.concurrent.ExecutionContext.Implicits._
  import ftd_api.yaml.BodyReads._
  //import play.api.libs.concurrent.Execution.Implicits.defaultContext


  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  def createDataset(jsonDataset: JsValue): Unit = println("TODO")
  def dataset(datasetId: String): JsValue = JsString("TODO")


  def testDataset(datasetId :String) : Future[Dataset] = {
    val ahcConfig = WebServiceUtil.config
    val client  = AhcWSClient(ahcConfig)
    val responseWs = client.url("http://localhost:9000/ckan/dataset/" + datasetId ).get()
    responseWs.map { response =>

      val json = response.json
      val datasetJson: JsValue = (response.json \ "result").get
      val datasetValidate = datasetJson.validate[Dataset]
      val dataset: Dataset = datasetValidate match {
        case s: JsSuccess[Dataset] => println(s.get);s.get
        case e: JsError => println(e); Dataset(None,None,None,None,None,
          None,None,None,None,None,None,None,
          None,None,None,None,None,
          None,None)
      }
      client.close()
      dataset
    }
  }

}
