package repositories.ckan

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.{DBObject, WriteResult}
import com.mongodb.casbah.{MongoClient, MongoCollection}
import ftd_api.yaml.{Credentials, Dataset, DistributionLabel, Organization, ResourceSize, User}
import play.api.libs.json._
import play.api.libs.ws.ahc.AhcWSClient
import utils.{ConfigReader, SecurePasswordHashing}
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

  private def readMongo(collectionName: String, filterAttName: String, filterValue: String): JsValue = {

    val mongoClient = MongoClient(mongoHost, mongoPort)
    val db = mongoClient("monitor_mdb")
    //val collection = db.getCollection(collectionName)
    val coll = db(collectionName)
    //val result2 = collection.findOne(equal(filterAttName, filterValue))

    val query = MongoDBObject(filterAttName -> filterValue)
    val result = coll.findOne(query)
    mongoClient.close

    val out: JsValue = result match {
      case Some(x) => {
        val jsonString = com.mongodb.util.JSON.serialize(x)
        Json.parse(jsonString)
      }
      case None => JsString("Not found")
    }

    out

  }

  def getMongoUser(name:String): JsResult[User] = {

    val jsUser = readMongo("users","name", name )
    val userValidate = jsUser.validate[User]
    userValidate

  }

  def verifyCredentials(credentials: Credentials):Boolean = {

    val result = readMongo("users", "name", credentials.username.get.toString)
    val hpasswd = (result \ "password").get.as[String]

    //println("pwd: "+credentials.password.get.toString)
    //println("hpasswd: "+hpasswd)

    return SecurePasswordHashing.validatePassword(credentials.password.get.toString, hpasswd )
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

    val password = (jsonUser \ "password").get.as[String]
    println("PWD -->"+password+"<")
    val hashedPassword = SecurePasswordHashing.hashPassword( password )
    val updatedJsonUser = jsonUser.as[JsObject] + ("password" -> JsString(hashedPassword) )

    val wsClient = AhcWSClient()
    val url =  LOCALURL + "/ckan/createUser"
    wsClient.url(url).post(jsonUser).map({ response =>
      (response.json \ "success").toOption match {
                case Some(x) => if(x.toString()=="true")writeMongo(updatedJsonUser,"users"); x.toString()
                case _ => JsString(CKAN_ERROR).toString()
            }
    }).andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

  }

  def getUserOrganizations(userName :String) : Future[JsResult[Seq[Organization]]] = {

    val wsClient = AhcWSClient()
    val url =  LOCALURL + "/ckan/userOrganizations/" + userName
    wsClient.url(url).get().map ({ response =>
      val orgsListJson: JsValue = (response.json \ "result")
        .getOrElse(JsString(CKAN_ERROR))

      val orgsListValidate = orgsListJson.validate[Seq[Organization]]
      println(orgsListValidate)
      orgsListValidate

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
