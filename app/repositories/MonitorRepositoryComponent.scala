package repositories

import java.io.FileInputStream

import ftd_api.yaml.{BrokenLink, Catalog, Distribution}
import play.api.libs.json._
import play.Environment

import scala.collection.immutable.List


/**
  * Created by ale on 31/03/17.
  */


trait MonitorRepositoryComponent {
  val monitorRepository: Repository //= new MonitorRepository
  trait Repository {
    def allCatalogs(): List[Catalog]
    def datasetCatalogLicenses(catalogName: String): Seq[Distribution]
    def datasetCatalogFormat(catalogName: String): Seq[Distribution]
    def datasetCatalogGroup(catalogName: String): Seq[Distribution]

  }

  object Repository {
    def apply(config: String): Repository = config match {
      case "dev" => new MonitorRepositoryDev
      case "prod" => new MonitorRepositoryProd
    }
  }

  class MonitorRepositoryDev extends Repository {

    import play.api.libs.functional.syntax._

    private val streamCatalog = new FileInputStream(Environment.simple().getFile("data/Catalog.json"))
    private val jsonCatalog: JsValue = try {
      Json.parse(streamCatalog)
    } finally {
      streamCatalog.close()
    }

    private val streamDistribution = new FileInputStream(Environment.simple().getFile("data/Distribution.json"))
    private val jsonDistribution: JsValue = try {
      Json.parse(streamDistribution)
    } finally {
      streamDistribution.close()
    }

    private val streamBrokenLink = new FileInputStream(Environment.simple().getFile("data/BrokenLink.json"))
    private val jsonBrokenLink: JsValue = try {
      Json.parse(streamBrokenLink)
    } finally {
      streamBrokenLink.close()
    }

    implicit val distributionRead: Reads[Distribution] = (
       (JsPath \ "label").readNullable[String] and
         (JsPath \ "count").readNullable[Float]
      ) (Distribution.apply _)

    implicit val brokenLinkRead: Reads[BrokenLink] = (
      (JsPath \ "url").readNullable[String] and
        (JsPath \ "m_status").readNullable[String] and
        (JsPath \ "name").readNullable[String] and
        (JsPath \ "rurl").readNullable[String] and
        (JsPath \ "catalog_name").readNullable[String] and
        (JsPath \ "dataset_url").readNullable[String]
      ) (BrokenLink.apply _)



    def allCatalogs(): List[Catalog] = {
      val names: List[String] = ((jsonCatalog \ "Catalog") \\ "name").map(_.as[String]).toList
      val catalogs = names.map(x => Catalog(Some(x)))
      catalogs
    }

    def datasetCatalogLicenses(catalogName: String): Seq[Distribution] = {
      val distritbutionJs: JsValue = ((jsonDistribution \ "Distribution") \ catalogName).get
      val distributions: JsResult[Seq[Distribution]] = distritbutionJs.validate[Seq[Distribution]]
      val results = distributions match {
        case s: JsSuccess[Seq[Distribution]] => s.get
        case e: JsError => Seq()
      }
      results
    }

    def datasetCatalogFormat(catalogName: String): Seq[Distribution] = {
      val distritbutionJs: JsValue = ((jsonDistribution \ "Distribution") \ catalogName).get
      val distributions: JsResult[Seq[Distribution]] = distritbutionJs.validate[Seq[Distribution]]
      val results = distributions match {
        case s: JsSuccess[Seq[Distribution]] => s.get
        case e: JsError => Seq()
      }
      results
    }

    def datasetCatalogGroup(catalogName: String): Seq[Distribution] = {
      val distritbutionJs: JsValue = ((jsonDistribution \ "Distribution") \ catalogName).get
      val distributions: JsResult[Seq[Distribution]] = distritbutionJs.validate[Seq[Distribution]]
      val results = distributions match {
        case s: JsSuccess[Seq[Distribution]] => s.get
        case e: JsError => Seq()
      }
      results
    }
  }

    class MonitorRepositoryProd extends Repository {
      def allCatalogs(): List[Catalog] = {
        List(Catalog(None), Catalog(None))
      }

      def datasetCatalogLicenses(catalogName: String): Seq[Distribution] = {
        Seq(Distribution(None, None))
      }

      def datasetCatalogFormat(catalogName: String): Seq[Distribution] = {
        Seq(Distribution(None,None))
      }

      def datasetCatalogGroup(catalogName: String): Seq[Distribution] = {
        Seq(Distribution(None,None))
      }

    }

  }

