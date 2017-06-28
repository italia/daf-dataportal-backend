package repositories.monitor

import ftd_api.yaml.{BrokenLink, Catalog, Distribution}
import scala.collection.immutable.List
import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.Imports._
import play.api.libs.json._
import utils.{ConfigReader}


/**
  * Created by ale on 07/04/17.
  *
  */


class MonitorRepositoryProd  extends MonitorRepository {


  private val mongoHost: String = ConfigReader.getDbHost
  private val mongoPort = ConfigReader.getDbPort


  def createDataset(jsonDataset: JsValue): Unit = println("TODO")
  def dataset(datasetId: String): JsValue = JsString("TODO")

  def allCatalogs(): List[Catalog] = {
    val mongoClient = MongoClient(mongoHost, mongoPort)
    val db = mongoClient("monitor_mdb")
    val coll = db("dist_format_by_group")
    val results = coll.distinct("title").toList
    mongoClient.close
    val names: List[String] = Json.parse(com.mongodb.util.JSON.serialize(results)).as[List[String]]
    val catalogs = names.map(x => Catalog(Some(x)))
    catalogs
  }

  def datasetCatalogLicenses(catalogName: String): Seq[Distribution] = {
    val mongoClient = MongoClient(mongoHost, mongoPort)
    val db = mongoClient("monitor_mdb")
    val coll = db("license_cat")
    val catalog = catalogName.replaceAll("_", " ")
    val query = "title" $eq catalog
    val excludeId =  MongoDBObject("_id" -> 0)
    val results = coll.find(query, excludeId).toList
    mongoClient.close
    val json = Json.parse(com.mongodb.util.JSON.serialize(results)).as[List[JsObject]]
    val distributions = json.map(x => {
      val label: Option[String] = (x \ "license_id").asOpt[String] match {
        case Some(data) => Some(data)
        case None => Some("")
      }
      val count = (x \ "count").asOpt[Float]
      Distribution(label,count)
    })
    distributions
    //Seq(Distribution(None, None))
  }

  def datasetCatalogFormat(catalogName: String): Seq[Distribution] = {
    val mongoClient = MongoClient(mongoHost, mongoPort)
    val db = mongoClient("monitor_mdb")
    val coll = db("dist_format_by_group")
    val catalog = catalogName.replaceAll("_", " ")
    val query = "title" $eq catalog
    val excludeId =  MongoDBObject("_id" -> 0)
    val results = coll.find(query, excludeId).toList
    mongoClient.close
    val jsonString = com.mongodb.util.JSON.serialize(results)
    println(jsonString)
    val json = Json.parse(jsonString).as[List[JsObject]]
    val distributions = json.map(x => {
      val label: Option[String] = (x \ "format").asOpt[String] match {
        case Some(data) => Some(data)
        case None => Some("")
      }
      val count = (x \ "count").asOpt[Float]
      Distribution(label,count)
    }
    )
    distributions
  }

  def datasetCatalogGroup(catalogName: String): Seq[Distribution] = {
    val mongoClient = MongoClient(mongoHost, mongoPort)
    val db = mongoClient("monitor_mdb")
    val coll = db("dataset_by_group")
    val catalog = catalogName.replaceAll("_", " ")
    val query = "catalog_parent_name" $eq catalog
    val excludeId =  MongoDBObject("_id" -> 0)
    val results = coll.find(query, excludeId).toList
    mongoClient.close
    val jsonString = com.mongodb.util.JSON.serialize(results)
    println(jsonString)
    val json = Json.parse(jsonString).as[List[JsObject]]
    val distributions = json.map(x => {
      val label: Option[String] = (x \ "catalog_parent_name").asOpt[String] match {
        case Some(data) => Some(data)
        case None => Some("")
      }
      val count = (x \ "count").asOpt[Float]
      Distribution(label,count)
    }
    )
    distributions
  }

  def datasetCounts() :Seq[Distribution] = {
    val mongoClient = MongoClient(mongoHost, mongoPort)
    val db = mongoClient("monitor_mdb")
    val coll = db("datasets")
    val results = coll.find().toList
    mongoClient.close
    val jsonString = com.mongodb.util.JSON.serialize(results)
    val json = Json.parse(jsonString).as[List[JsObject]]
    val distributions = json.map(x => {
      val label: Option[String] = (x \ "catalog_parent_name").asOpt[String] match {
        case Some(data) => Some(data)
        case None => Some("")
      }
      val count = (x \ "count").asOpt[Float]
      Distribution(label,count)
    })
    distributions
  }

  def allDistributionLiceses():Seq[Distribution] = {
    val mongoClient = MongoClient(mongoHost, mongoPort)
    val db = mongoClient("monitor_mdb")
    val coll = db("license")
    val results = coll.find().toList
    mongoClient.close
    val jsonString = com.mongodb.util.JSON.serialize(results)
    val json = Json.parse(jsonString).as[List[JsObject]]
    val distributions = json.map(x => {
      val label: Option[String] = (x \ "license_id").asOpt[String] match {
        case Some(data) => Some(data)
        case None => Some("")
      }
      val count = (x \ "count").asOpt[Float]
      Distribution(label,count)
    })
    distributions
  }

  def allDistributionFormat():Seq[Distribution] = {
    val mongoClient = MongoClient(mongoHost, mongoPort)
    val db = mongoClient("monitor_mdb")
    val coll = db("format_dist")
    val results = coll.find().toList
    mongoClient.close
    val jsonString = com.mongodb.util.JSON.serialize(results)
    val json = Json.parse(jsonString).as[List[JsObject]]
    val distributions = json.map(x => {
      val label: Option[String] = (x \ "format").asOpt[String] match {
        case Some(data) => Some(data)
        case None => Some("")
      }
      val count = (x \ "count").asOpt[Float]
      Distribution(label,count)
    })
    distributions
  }

  def allDistributionGroup():Seq[Distribution] = {
    Seq(Distribution(None,None),Distribution(None,None))
  }

  def catalogDatasetCount(catalogName: String) : Seq[Distribution] = {
    val mongoClient = MongoClient(mongoHost, mongoPort)
    val db = mongoClient("monitor_mdb")
    val coll = db("datasets")
    val catalog = catalogName.replaceAll("_", " ")
    val query = "catalog_parent_name" $eq catalog
    println(query)
    val results = coll.find(query).toList
    mongoClient.close
    val jsonString = com.mongodb.util.JSON.serialize(results)
    val json = Json.parse(jsonString).as[List[JsObject]]
    val distributions = json.map(x => {
      val label: Option[String] = (x \ "catalog_parent_name").asOpt[String] match {
        case Some(data) => Some(data)
        case None => Some("")
      }
      val count = (x \ "count").asOpt[Float]
      Distribution(label,count)
    })
    distributions
  }

  def catalogBrokenLinks(catalogName: String): Seq[BrokenLink] = {
    val mongoClient = MongoClient(mongoHost, mongoPort)
    val db = mongoClient("monitor_mdb")
    val coll = db("ko")
    val catalog = catalogName.replaceAll("_", " ")
    val query = "catalog_parent_name" $eq catalog
    val results = coll.find(query).toList
    mongoClient.close
    val jsonString = com.mongodb.util.JSON.serialize(results)
    val json = Json.parse(jsonString) //.as[List[JsObject]]
    val brokenLinksJs = json.validate[Seq[BrokenLink]]
    val brokenLinks = brokenLinksJs match {
      case s: JsSuccess[Seq[BrokenLink]] => s.get
      case e: JsError => Seq()
    }
    brokenLinks
    //Seq(BrokenLink(None,None,None,None,None,None,None))
  }

  def allBrokenLinks(): Seq[BrokenLink] = {
    val mongoClient = MongoClient(mongoHost, mongoPort)
    val db = mongoClient("monitor_mdb")
    val coll = db("ko")
    val results = coll.find().toList
    mongoClient.close
    val jsonString = com.mongodb.util.JSON.serialize(results)
    val json = Json.parse(jsonString) //.as[List[JsObject]]
    val brokenLinksJs = json.validate[Seq[BrokenLink]]
    val brokenLinks = brokenLinksJs match {
      case s: JsSuccess[Seq[BrokenLink]] => s.get
      case e: JsError => Seq()
    }
    brokenLinks
   // Seq(BrokenLink(None,None,None,None,None,None,None))
  }
}
