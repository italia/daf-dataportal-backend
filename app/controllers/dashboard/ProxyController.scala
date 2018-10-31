package controllers.dashboard

import java.net.URLEncoder

import javax.inject.Inject
import com.mongodb.casbah.Imports.{MongoCredential, MongoDBObject, ServerAddress}
import com.mongodb.casbah.MongoClient
import ftd_api.yaml.Success
import it.gov.daf.common.utils.RequestContext
import play.api.Logger
import play.api.cache.CacheApi
import play.api.inject.ConfigurationProvider
import play.api.libs.json.Json
import play.api.libs.ws.{WSAuthScheme, WSClient, WSResponse}
import play.api.mvc.{Action, Controller, Result}
import utils.ConfigReader

import scala.concurrent.{ExecutionContext, Future}

class ProxyController @Inject()(ws: WSClient,
                                cache: CacheApi,
                                config: ConfigurationProvider
                                )(implicit context: ExecutionContext) extends Controller {

  private val userName = ConfigReader.userName
  private val source = ConfigReader.database
  private val password = ConfigReader.password

  private val mongoHost: String = ConfigReader.getDbHost
  //private val mongoPort:Int = ConfigReader.getDbPort

  private val datasetManagerUrl = ConfigReader.getDatasetUrl
  private val opendataUserEmail = ConfigReader.getDatasetUserOpendataEmail
  private val opendataUserPwd = ConfigReader.getDatasetUserOpendataPwd



  val server = new ServerAddress(mongoHost, 27017)
  val credentials = MongoCredential.createCredential(userName, source, password.toCharArray)

  private val logger = Logger(this.getClass.getName)

  def medium(url: String) = Action.async { implicit request =>
    RequestContext.execInContext[Future[Result]] ("medium") { () =>
      val scheme = url.matches("^http[s]?://.*") match {
        case true => ""
        case false => "https://"
      }
      val responseWs: Future[WSResponse] = ws.url(scheme + url).get
      responseWs.map { response =>
        Ok(response.body).as("text/xml")
      }
    }
  }

  def ckanProxy(action: String) = Action.async { implicit request =>
    RequestContext.execInContext[Future[Result]] ("ckanProxy") { () =>
      val baseUrl = "http://dcatapit.geo-solutions.it/api/3/action/"
      val url = baseUrl + action
      val queryString: Map[String, String] = request.queryString.map { case (k, v) => k -> v.mkString }
      val responseWs = ws.url(url)
        .withQueryString(queryString.toList: _*)
        .get
      responseWs.map { response =>
        Ok(response.body).as("application/json")
      }
    }
  }

  def ckanGeoProxy(action: String) = Action.async { implicit request =>
    RequestContext.execInContext[Future[Result]] ("ckanGeoProxy") { () =>
      val baseUrl = "http://ckan-geo.default.svc.cluster.local:5000/api/3/action/"
      val url = baseUrl + action
      val queryString: Map[String, String] = request.queryString.map { case (k, v) => k -> v.mkString }
      val responseWs = ws.url(url)
        .withQueryString(queryString.toList: _*)
        .get
      responseWs.map { response =>
        Ok(response.body).as("application/json")
      }
    }
  }

  def externalOpendata(name :String) = Action {  implicit request =>
    RequestContext.execInContext[Result] ("externalOpendata") { () =>
      val mongoClient = MongoClient(server, List(credentials))
      val db = mongoClient(source)
      val coll = db("ext_opendata")
      val query = MongoDBObject("name" -> name)
      val dataset = coll.findOne(query)
      val json = dataset match {
        case None => Json.parse("{}")
        case Some(ser) => {
          val jsonString = com.mongodb.util.JSON.serialize(dataset)
          Json.parse(jsonString)
        }
      }
      Ok(json)
    }
  }

  def dafResourcesInExtOpendata(name :String) = Action {  implicit request =>
    RequestContext.execInContext[Result] ("dafResourcesInExtOpendata") { () =>
      val mongoClient = MongoClient(server, List(credentials))
      val db = mongoClient(source)
      val coll = db("catalog_test")
      val query = MongoDBObject("operational.ext_opendata.name" -> name)
      val datasets = coll.find(query).toList
      val jsonString = com.mongodb.util.JSON.serialize(datasets)
      val json = Json.parse(jsonString)
      Ok(json)
    }
  }


  def openDataPreview(logicalUri :String) = Action.async { implicit request =>

      val url = datasetManagerUrl +
        URLEncoder.encode(logicalUri,"UTF-8") // + "?limit=50"
      val limitQueryString  = request.getQueryString("limit") match {
        case None => ""
        case Some(_) => "limit=50"
      }
      val format = request.getQueryString("format").getOrElse("json")

      def buildQueryString = if (limitQueryString == "") {
        "?format=" + format
      } else {
        "?format=" + format + "&" + limitQueryString
      }

      val responseWs: Future[WSResponse] = ws.url(url + buildQueryString)
        .withAuth(opendataUserEmail, opendataUserPwd, WSAuthScheme.BASIC)
        .get

      responseWs.map { response =>
              if (format.equals("csv")) {
                Ok(response.body)
              } else {
                Ok(response.json)
              }
            }
  }


  def openDataDownload(physicalUriEncoded :String) = Action.async { implicit request =>


    val url = "https://api.daf.teamdigitale.it/dataset-manager/v1/dataset/" +
      URLEncoder.encode(physicalUriEncoded,"UTF-8")

    println("URL")
    println(url)
    val responseWs: Future[WSResponse] =
      ws.url(url)
        .withAuth(opendataUserEmail, opendataUserPwd, WSAuthScheme.BASIC)
        .withFollowRedirects(true)
        .get
    responseWs.map { response =>
      Ok(response.json) //.as("application/json")
    }
  }



}
