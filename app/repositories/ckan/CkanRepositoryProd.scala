package repositories.ckan

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.google.inject.Inject
import ftd_api.yaml.Dataset
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.libs.ws.ahc.AhcWSClient
import utils.ConfigReader
import utils.it.gov.daf.catalogmanager.utilities.WebServiceUtil

import scala.concurrent.Future

/**
  * Created by ale on 01/07/17.
  */
class CkanRepositoryProd  extends CkanRepository{

  import ftd_api.yaml.BodyReads._
  import scala.concurrent.ExecutionContext.Implicits._

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  private val LOCALURL = "http://localhost:9000"

  def createDataset(jsonDataset: JsValue): Unit = println("TODO")

  def dataset(datasetId: String): JsValue = JsString("TODO")

  def testDataset(datasetId :String) : Future[JsResult[Dataset]] = {

    val wsClient = AhcWSClient()
    val url =  LOCALURL + "/ckan/dataset/" + datasetId

    wsClient.url(url).get().map ({ response =>
      val json = response.json
      val datasetJson: JsValue = (response.json \ "result")
        .getOrElse(Json.obj("error" -> "No dataset"))
      val datasetValidate = datasetJson.validate[Dataset]
      datasetValidate
    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

  }


}
