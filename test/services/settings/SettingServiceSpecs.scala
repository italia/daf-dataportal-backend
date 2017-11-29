package services.settings

import java.io.FileInputStream

import com.mongodb.casbah.Imports.{MongoCredential, ServerAddress}
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.{MongoClient, MongoCursor, MongoDB}
import ftd_api.yaml.{Settings, Success}
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.prop.PropertyChecks
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import repositories.settings.SettingsRepositoryProd
import play.api.libs.ws.WSResponse
import play.api.test.{WithServer, WsTestClient}
import utils.ConfigReader

import scala.collection.immutable.List
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.language.postfixOps

class SettingServiceSpecs extends FlatSpec with Matchers with PropertyChecks {
  private val mongoHost: String = "10.100.82.218"
  private val mongoPort = 27017

  private val userName = "ckan"
  private val source = "ckan"
  private val password = "ckan"

  val server = new ServerAddress(mongoHost, mongoPort)
  val credentials = MongoCredential.createCredential(userName, source, password.toCharArray)

  val name = "nomeProva"

  val settingsRepositoryProd: SettingsRepositoryProd = new SettingsRepositoryProd
  val settings: Settings = Settings(
    Some("https://developers.italia.it/assets/icons/dt-logo.svg"),
    Some("https://medium.com/team-per-la-trasformazione-digitale/"),
    Some("https://twitter.com/datigovit"),
    Some("https://designers.italia.it/note-legali/"),
    Some("TEAM PER LA TRASFORMAZIONE DIGITALE"),
    Some("https://teamdigitale.governo.it/images/loghi/governo.svg"),
    Some("https://developers.italia.it/news"),
    Some("https://forum.italia.it"),
    Some("1"),
    Some("/daf"),
    Some("https://designers.italia.it/privacy-policy/"),
    Some("https://www.spid.gov.it/assets/img/agid-logo-bb.svg"),
    Some("Il framework dei dati pubblici del Paese"),
    Some("https://developers.italia.it/assets/icons/logo-it.png")
  )
  val settings2: Settings = Settings(
    Some("https://developers.italia.it/assets/icons/dt-logo.svg"),
    Some("https://medium.com/team-per-la-trasformazione-digitale/"),
    Some("https://twitter.com/datigovit"),
    Some("https://designers.italia.it/note-legali/"),
    Some("TEAM PER LA TRASFORMAZIONE DIGITALE"),
    Some("https://teamdigitale.governo.it/images/loghi/governo.svg"),
    Some("https://developers.italia.it/news"),
    Some("https://forum.italia.it"),
    Some("13"),
    Some("/daf"),
    Some("https://designers.italia.it/privacy-policy/"),
    Some("https://www.spid.gov.it/assets/img/agid-logo-bb.svg"),
    Some("Il framework dei dati pubblici del Paese"),
    Some("https://developers.italia.it/assets/icons/logo-it.png")
  )
  val json = settingsRepositoryProd.settingsToJson(settings)


//  "SettingsRepositoryProd clean db" should "" in {
//    val mongoClient = MongoClient(server, List(credentials))
//    val db: MongoDB = mongoClient(source)
//    settingsRepositoryProd.settingsByName(db, name)
//    val coll = db("settings")
//    coll.drop()
//  }
//
//  "SettingsRepositoryProd create a collection and insert json in mongodb and respond" should "success" in {
//    val mongoClient = MongoClient(server, List(credentials))
//    val db: MongoDB = mongoClient(source)
//    val result: Success = settingsRepositoryProd.insertSettings(db, name, settings)
//    println(s"test1: $result")
//    mongoClient.close()
//    true shouldBe true
//  }
//
//  "SettingsRepositoryProd get settings from mongodb" should "equal to val settings" in {
//    val mongoClient = MongoClient(server, List(credentials))
//    val db: MongoDB = mongoClient(source)
//
//    //get old value if not exist get a Settings(None)
//    val result: Settings = settingsRepositoryProd.settingsByName(db, name)
//    println(s"test1: ${result.toString}")
//
//    //update
//    val result2 = settingsRepositoryProd.saveSettings(db, name, settings2)
//    println("test2")
//    println(s"field: ${result2.fields}, messagge: ${result2.message}")
//
//    //get
//    val result3: Settings = settingsRepositoryProd.settingsByName(db, name)
//    println(s"test3: ${result3.toString}")
//
//    mongoClient.close()
//    true shouldBe true
//  }


//  "SettingsRepositoryProd get settings collection" should "non empty" in {
//    val mongoClient = MongoClient(server, List(credentials))
//    val db = mongoClient(source)
//    val coll = db("settings")
//    val res1: Success = settingsRepositoryProd.insertSettings(db, name, settings)
//    println(res1)
//    val query = MongoDBObject("organization" -> name)
//    val result: MongoCursor = coll.find(query)
//    result.toList.foreach(r => println(r))
//    println(s"json: ${settingsRepositoryProd.settingsToJson(settings2)}")
//    val res = settingsRepositoryProd.saveSettings(db, name, settings2)
//    println(res)
//    val r = coll.find(query)
//    r.foreach(k => println(k))
//  }

//"test conversion from json to settings" should "" in {
//  val r = settingsRepositoryProd.jsonToSettings(json)
//
//  println(r)
//  val k = json \ "thema"
//  println(k.get)
//}

}
