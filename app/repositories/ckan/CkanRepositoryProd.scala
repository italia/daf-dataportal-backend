package repositories.ckan

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.mongodb.{DBObject, WriteResult}
import com.mongodb.casbah.MongoClient
import ftd_api.yaml.{Dataset, DistributionLabel, Organization, ResourceSize}
import play.api.libs.json._
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
  private val CKAN_ERROR = "CKAN service is not working correctly"

  private val mongoHost: String = ConfigReader.getDbHost
  private val mongoPort = ConfigReader.getDbPort

  private def writeMongo(json: JsValue, collectionName: String): Boolean = {

    println("write mongo: "+json.toString())
    val mongoClient = MongoClient(mongoHost, mongoPort)
    val db = mongoClient("monitor_mdb")
    val coll = db(collectionName)
    val obj = com.mongodb.util.JSON.parse(json.toString()).asInstanceOf[DBObject]
    val inserted = coll.insert(obj)
    mongoClient.close()
    inserted.getN > 0

  }



  def createDataset(jsonDataset: JsValue): Future[String] = {

    val wsClient = AhcWSClient()
    val url =  LOCALURL + "/ckan/createDataset"
    wsClient.url(url).post(jsonDataset).map({ response =>
      (response.json \ "success").getOrElse(JsString(CKAN_ERROR)).toString()
    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

  }

  def createOrganization(jsonOrg: JsValue): Future[String] = {

    val wsClient = AhcWSClient()
    val url =  LOCALURL + "/ckan/createOrganization"
    wsClient.url(url).post(jsonOrg).map({ response =>
      (response.json \ "success").getOrElse(JsString(CKAN_ERROR)).toString()
    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

  }

  def updateOrganization(orgId: String, jsonOrg: JsValue): Future[String] = {

    val wsClient = AhcWSClient()
    val url =  LOCALURL + "/ckan/updateOrganization/" + orgId
    wsClient.url(url).put(jsonOrg).map({ response =>
      (response.json \ "success").getOrElse(JsString(CKAN_ERROR)).toString()
    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

  }

  def createUser(jsonUser: JsValue): Future[String] = {

    val wsClient = AhcWSClient()
    val url =  LOCALURL + "/ckan/createUser"
    wsClient.url(url).post(jsonUser).map({ response =>
      (response.json \ "success").toOption match {
                case Some(x) => if(x.toString()=="true")writeMongo(jsonUser,"users"); x.toString()
                case _ => JsString(CKAN_ERROR).toString()
            }
    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

  }

  def dataset(datasetId: String): JsValue = JsString("TODO")


  def getOrganization(orgId :String) : Future[JsResult[Organization]] = {

    val wsClient = AhcWSClient()
    val url =  LOCALURL + "/ckan/organization/" + orgId
    wsClient.url(url).get().map ({ response =>
      val orgJson: JsValue = (response.json \ "result")
        .getOrElse(Json.obj("error" -> "No organization"))
      val orgValidate = orgJson.validate[Organization]
      orgValidate
    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

  }

  def getOrganizations() : Future[JsValue] = {

    val wsClient = AhcWSClient()
    val url =  LOCALURL + "/ckan/organizations"
    wsClient.url(url).get().map ({ response =>
      val orgsListJson: JsValue = (response.json \ "result")
        .getOrElse(JsString(CKAN_ERROR))
      orgsListJson
    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }
  }


  def getDatasets() : Future[JsValue] = {

    val wsClient = AhcWSClient()
    val url =  LOCALURL + "/ckan/datasets"
    wsClient.url(url).get().map ({ response =>
      val dsListJson: JsValue = (response.json \ "result")
        .getOrElse(JsString(CKAN_ERROR))
      dsListJson
    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }
  }

  def searchDatasets( input: (DistributionLabel, DistributionLabel, ResourceSize) ) : Future[JsResult[Seq[Dataset]]]={

    val wsClient = AhcWSClient()

    val params = Map(("q",input._1),("sort",input._2),("rows",input._3))

    val queryString = WebServiceUtil.buildEncodedQueryString(params)

    val url =  LOCALURL + "/ckan/searchDataset"+queryString

    wsClient.url(url).get().map ({ response =>
      val datasetJson: JsValue =( (response.json \ "result") \ "results")
        .getOrElse(Json.obj("error" -> "No datasets"))

      val datasetsValidate = datasetJson.validate[Seq[Dataset]]
      println(datasetsValidate)
      datasetsValidate
    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

  }

  def getDatasetsWithRes( input: (ResourceSize, ResourceSize) ) : Future[JsResult[Seq[Dataset]]] = {

    val wsClient = AhcWSClient()

    val params = Map( ("limit",input._1),("offset",input._2) )

    val queryString = WebServiceUtil.buildEncodedQueryString(params)

    val url =  LOCALURL + "/ckan/datasetsWithResources"+queryString

    wsClient.url(url).get().map ({ response =>
      val datasetJson: JsValue =(response.json \ "result")
        .getOrElse(Json.obj("error" -> "No datasets"))

      val datasetsValidate = datasetJson.validate[Seq[Dataset]]
      println(datasetsValidate)
      datasetsValidate
    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

  }


  def testDataset(datasetId :String) : Future[JsResult[Dataset]] = {

    val wsClient = AhcWSClient()
    val url =  LOCALURL + "/ckan/dataset/" + datasetId

    wsClient.url(url).get().map ({ response =>
      val datasetJson: JsValue = (response.json \ "result")
        .getOrElse(Json.obj("error" -> "No dataset"))
      val datasetValidate = datasetJson.validate[Dataset]
      datasetValidate
    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

  }


}
