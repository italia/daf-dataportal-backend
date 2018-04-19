package repositories.settings

import akka.actor.ActorSystem
import com.mongodb.DBObject
import com.mongodb.casbah.Imports.{MongoCredential, MongoDBObject, ServerAddress}
import com.mongodb.casbah._
import ftd_api.yaml.{Error, Settings, Success}
import play.api.Logger
import play.api.libs.json._
import utils.ConfigReader


class SettingsRepositoryProd extends SettingsRepository {

  import ftd_api.yaml.BodyReads._

  implicit val system = ActorSystem()

  private val mongoHost: String = ConfigReader.getDbHost
  private val mongoPort = ConfigReader.getDbPort
  private val userName = ConfigReader.userName
  private val source = ConfigReader.database
  private val password = ConfigReader.password


  val server = new ServerAddress(mongoHost, mongoPort)
  val credentials = MongoCredential.createCredential(userName, source, password.toCharArray)
  val logger = Logger.logger

  private def updateSettings(db: MongoDB, name: String, settings: Settings): Either[Error, Success] = {
    val coll = db("settings")
    val json = settingsToJson(settings)
    val obj = com.mongodb.util.JSON.parse(json.toString()).asInstanceOf[DBObject]
    obj.put("domain", name)
    val query = MongoDBObject("domain" -> name)
    val res = coll.update(query, obj)
    if (res.getN > 0)
      Right(Success(Some(s"Settings of domain $name update"), Some(name)))
    else
      Left(Error(Some(0), Some("Error update settings"), Some("")))
  }

  private def insertSettings(db: MongoDB, name: String, settings: Settings): Either[Error, Success] = {
    val coll = db("settings")
    val json = settingsToJson(settings)
    val obj = com.mongodb.util.JSON.parse(json.toString()).asInstanceOf[DBObject]
    obj.put("domain", name)
    try {
      coll.save(obj)
      val resultInsert = getSettingsByName(name, db).isDefined
      resultInsert match {
        case true => Right(Success(Some(s"Settings of domain $name saved"), Some(name)))
        case false => Left(setInsertErrorMessage(name))
      }
    }
    catch {
      case _: Exception => {
        logger.debug("error in mongo connection in insertSettings")
        Left(setInsertErrorMessage(name))
      }
    }
  }

  private def setInsertErrorMessage(name: String): Error = {
    Error(Some(0), Some("Error save settings"), Some(name))
  }

  override def saveSettings(name: String, settings: Settings): Either[Error, Success] = {
    val mongoClient = MongoClient(server, List(credentials))
    val db: MongoDB = mongoClient(source)
    val settingsInMongo: Boolean = getSettingsByName(name, db).isDefined
    val response = settingsInMongo match {
      case true => updateSettings(db, name, settings)
      case false => insertSettings(db, name, settings)
    }
    mongoClient.close()
    response
  }

  private def getSettingsByName(name: String, db: MongoDB) = {
    val coll = db("settings")
    val query = MongoDBObject("domain" -> name)
    try {
      coll.findOne(query)
    } catch {
      case _: Exception => {
        logger.debug("exception in mongo connection")
        None
      }
    }

  }

  override def settingsByName(name: String): Either[Error, Settings] = {
    val mongoClient = MongoClient(server, List(credentials))
    val db: MongoDB = mongoClient(source)
    val result = getSettingsByName(name, db)
    mongoClient.close()
    result match {
      case Some(_) => {
        val jsonString = com.mongodb.util.JSON.serialize(result)
        val json = Json.parse(jsonString)
        val settingsJsResult = json.validate[Settings]
        settingsJsResult match {
          case s: JsSuccess[Settings] => Right(s.get)
          case _: JsError => Left(Error(Some(0), Some("Error in get settings"), Some("")))
        }
      }
      case None => Right(Settings(None, None, None, None, None, None, None, None, None, None, None, None, None, None, None))
    }
  }

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
         |  "footerLegal":"${settings.footerLegal.getOrElse("")}",
         |  "organization":"${settings.organization.getOrElse("")}"
         |}
      """.stripMargin
    )
    json
  }

  override def getDomain(groups: List[String], isAdmin: Boolean): Seq[String] = {
    val mongoClient = MongoClient(server, List(credentials))
    val db: MongoDB = mongoClient(source)
    val coll = db("settings")
    val mapDomain: List[(String, String)] = coll.map(
      settings =>
        (settings.get("organization").toString , settings.get("domain").toString)
    ).toList
    mongoClient.close()
    if(isAdmin) mapDomain.map(elem => elem._2)
    else mapDomain.filter(elem => groups.contains(elem._1)).map(elem => elem._2)
  }
}
