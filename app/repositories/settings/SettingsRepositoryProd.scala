package repositories.settings

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.mongodb.DBObject
import com.mongodb.casbah.Imports.{MongoCredential, MongoDBObject, ServerAddress}
import com.mongodb.casbah._
import ftd_api.yaml.{Settings, Success}
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json._
import utils.ConfigReader

import scala.collection.immutable.List

class SettingsRepositoryProd extends SettingsRepository {

  import ftd_api.yaml.BodyReads._
  import scala.concurrent.ExecutionContext.Implicits._

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  private val localUrl = ConfigReader.getLocalUrl

  private val supersetUser = ConfigReader.getSupersetUser

  private val mongoHost: String = ConfigReader.getDbHost
  private val mongoPort = ConfigReader.getDbPort
  private val userName = ConfigReader.userName
  private val source = ConfigReader.database
  private val password = ConfigReader.password


  val server = new ServerAddress(mongoHost, mongoPort)
  val credentials = MongoCredential.createCredential(userName, source, password.toCharArray)

  private def updateSettings(db: MongoDB, name: String, settings: Settings): Success = {
    val coll = db("settings")
    val json = settingsToJson(settings)
    val obj = com.mongodb.util.JSON.parse(json.toString()).asInstanceOf[DBObject]
    obj.put("organization", name)
    val query = MongoDBObject("organization" -> name)
    val res = coll.update(query, obj)
    val message = res.getN match {
      case 1 => Some(s"Settings of organization $name update")
      case _ => Some("Error")
    }
    Success(message, Some(name))
  }

  private def insertSettings(db: MongoDB, name: String, settings: Settings): Success = {
    val coll = db("settings")
    val json = settingsToJson(settings)
    val obj = com.mongodb.util.JSON.parse(json.toString()).asInstanceOf[DBObject]
    obj.put("organization", name)
    val res = coll.save(obj)
    Success(Some(s"Settings of organization $name saved"), Some(name))
  }

  override def saveSettings(name: String, settings: Settings) = {
    val mongoClient = MongoClient(server, List(credentials))
    val db: MongoDB = mongoClient(source)
    val settingsInMongo = getSettingsByName(name, db)
    val response: Success = settingsInMongo match {
        case Some(_) => updateSettings(db, name, settings)
        case None => insertSettings(db, name, settings)
      }
    mongoClient.close()
    response
  }

  //only for test
  //  def saveSettings(db: MongoDB, name: String, settings: Settings) = {
  //    val settingsInMongo = getSettingsByName(name, db)
  //    val response: Success =
  //      settingsInMongo match {
  //        case Some(_) => updateSettings(db, name, settings)
  //        case None => insertSettings(db, name, settings)
  //      }
  //    //    mongoClient.close()
  //    response
  //  }


  private def getSettingsByName(name: String, db: MongoDB) = {
    val coll = db("settings")
    val query = MongoDBObject("organization" -> name)
    try{
      val result = coll.findOne(query)
      result
    } catch{
      case _ => None
    }

  }

  override def settingsByName(name: String) = {
    val mongoClient = MongoClient(server, List(credentials))
    val db: MongoDB = mongoClient(source)
    val result = getSettingsByName(name, db)
    mongoClient.close()
    result match{
      case Some(_) => {
        val jsonString = com.mongodb.util.JSON.serialize(result)
        val json = Json.parse(jsonString)
        val settingsJsResult = json.validate[Settings]
        val settings = settingsJsResult match {
          case s: JsSuccess[Settings] => s.get
          case _: JsError => Settings(None, None, None, None, None, None, None, None, None, None, None, None, None, None)
        }
        settings
      }
      case None => Settings(None, None, None, None, None, None, None, None, None, None, None, None, None, None)
    }
//    val jsonString = com.mongodb.util.JSON.serialize(result)
//    val json = Json.parse(jsonString)
//    val settingsJsResult = json.validate[Settings]
//    val settings = settingsJsResult match {
//      case s: JsSuccess[Settings] => s.get
//      case _: JsError => Settings(None, None, None, None, None, None, None, None, None, None, None, None, None, None)
//    }
//    settings
  }

  //only for test
  //  def settingsByName(db: MongoDB, name: String) = {
  //    //    val mongoClient = MongoClient(server, List(credentials))
  //    //    val db: MongoDB = mongoClient(source)
  //    val result = getSettingsByName(name, db)
  //    //    mongoClient.close()
  //    val jsonString = com.mongodb.util.JSON.serialize(result)
  //    val json = Json.parse(jsonString)
  //    val settingsJsResult = json.validate[Settings]
  //    val settings = settingsJsResult match {
  //      case s: JsSuccess[Settings] => s.get
  //      case _: JsError => Settings(None, None, None, None, None, None, None, None, None, None, None, None)
  //    }
  //    settings
  //  }

  def settingsToJson(settings: Settings): JsValue = {
    val json = Json.parse(
      s"""
         |{
         |  "theme": "${settings.theme.getOrElse("")}",
         |  "headerLogo":"${settings.headerLogo.getOrElse("")}",
         |  "headerSiglaTool":"${settings.headerSiglaTool.getOrElse("")}",
         |  "headerDescTool":"${settings.headerDescTool.getOrElse("")}",
         |  "twitterURL":"${settings.twitterURL.getOrElse("")}",
         |  "mediumURL":"${settings.mediumURL.getOrElse("")}",
         |  "notizieURL": "${settings.notizieURL.getOrElse("")}",
         |  "forumURL": "${settings.forumURL.getOrElse("")}",
         |  "footerLogoAGID":"${settings.footerLogoAGID.getOrElse("")}",
         |  "footerLogoGov":"${settings.footerLogoGov.getOrElse("")}",
         |  "footerLogoDevITA":"${settings.footerLogoDevITA.getOrElse("")}",
         |  "footerNomeEnte":"${settings.footerNomeEnte.getOrElse("")}",
         |  "footerPrivacy":"${settings.footerPrivacy.getOrElse("")}",
         |  "footerLegal":"${settings.footerLegal.getOrElse("")}"
         |}
      """.stripMargin
    )
    json
  }
}
