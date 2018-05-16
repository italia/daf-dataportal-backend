package controllers.dashboard

import javax.inject.Inject

import com.mongodb.casbah.Imports.{MongoCredential, MongoDBObject, ServerAddress}
import com.mongodb.casbah.MongoClient
import ftd_api.yaml.Success
import play.api.cache.CacheApi
import play.api.inject.ConfigurationProvider
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc.{Action, Controller}
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
  private val mongoPort = ConfigReader.getDbPort

  val server = new ServerAddress(mongoHost, 27017)
  val credentials = MongoCredential.createCredential(userName, source, password.toCharArray)

  def medium(url: String) = Action.async { implicit request =>
    val scheme = url.matches("^http[s]?://.*") match {
      case true => ""
      case false => "https://"
    }
    val responseWs: Future[WSResponse] = ws.url(scheme + url).get
    responseWs.map { response =>
      Ok(response.body).as("text/xml")
    }
  }

  def ckanProxy(action: String) = Action.async { implicit request =>
    val baseUrl = "http://dcatapit.geo-solutions.it/api/3/action/"
    val url = baseUrl + action
    val queryString: Map[String, String] = request.queryString.map { case (k,v) => k -> v.mkString}
    val responseWs = ws.url(url)
      .withQueryString(queryString.toList: _*)
      .get
    responseWs.map { response =>
      Ok(response.body).as("application/json")
    }
  }

  def ckanGeoProxy(action: String) = Action.async { implicit request =>
    val baseUrl = "http://ckan-geo.default.svc.cluster.local:5000/api/3/action/"
    val url = baseUrl + action
    val queryString: Map[String, String] = request.queryString.map { case (k,v) => k -> v.mkString}
    val responseWs = ws.url(url)
      .withQueryString(queryString.toList: _*)
      .get
    responseWs.map { response =>
      Ok(response.body).as("application/json")
    }
  }

  def externalOpendata(name :String) = Action {  request =>
    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(source)
    val coll = db("ext_opendata")
    val query = MongoDBObject("name" -> name)
    val dataset = coll.findOne(query)
    val json = dataset match {
      case None => Json.parse("{}")
      case Some(ser) =>  {
        val jsonString = com.mongodb.util.JSON.serialize(dataset)
        Json.parse(jsonString)
      }
    }
    Ok(json)
  }

  def dafResourcesInExtOpendata(name :String) = Action {  request =>
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
