package repositories

import java.io.FileInputStream

import ftd_api.yaml.{BrokenLink, Catalog, Distribution}
import play.api.libs.json._
import play.Environment
import repositories.monitor.MonitorDevRepoUtil

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
    def datasetCounts(): Seq[Distribution]
    def allDistributionLiceses(): Seq[Distribution]
    def allDistributionFormat(): Seq[Distribution]
    def allDistributionGroup(): Seq[Distribution]
    def catalogDatasetCount(catalogName: String): Seq[Distribution]
    def catalogBrokenLinks(catalogName: String): Seq[BrokenLink]
    def allBrokenLinks(): Seq[BrokenLink]

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
        (JsPath \ "dataset_url").readNullable[String] and
        (JsPath \ "label").readNullable[String]
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

    def datasetCounts() :Seq[Distribution] = {
      val distritbutionJs: JsValue = (jsonDistribution \ "Distribution").get

      val counts = MonitorDevRepoUtil.labelGroupBy(distritbutionJs)

      val valid = counts.validate[Seq[Distribution]]
      val results = valid match {
        case s: JsSuccess[Seq[Distribution]] => s.get
        case e: JsError => Seq()
      }
      results

    }

    def allDistributionLiceses():Seq[Distribution] = {
      val distritbutionJs: JsValue = (jsonDistribution \ "Distribution").get

      val counts = MonitorDevRepoUtil.labelGroupBy(distritbutionJs)

      val valid = counts.validate[Seq[Distribution]]
      val results = valid match {
        case s: JsSuccess[Seq[Distribution]] => s.get
        case e: JsError => Seq()
      }
      results
    }

    def allDistributionFormat():Seq[Distribution] = {
      val distritbutionJs: JsValue = (jsonDistribution \ "Distribution").get

      val counts = MonitorDevRepoUtil.labelGroupBy(distritbutionJs)

      val valid = counts.validate[Seq[Distribution]]
      val results = valid match {
        case s: JsSuccess[Seq[Distribution]] => s.get
        case e: JsError => Seq()
      }
      results
    }

    def allDistributionGroup():Seq[Distribution] = {
      val distritbutionJs: JsValue = (jsonDistribution \ "Distribution").get

      val counts = MonitorDevRepoUtil.labelGroupBy(distritbutionJs)

      val valid = counts.validate[Seq[Distribution]]
      val results = valid match {
        case s: JsSuccess[Seq[Distribution]] => s.get
        case e: JsError => Seq()
      }
      results
    }

    def catalogDatasetCount(catalogName: String) : Seq[Distribution] = {
      val distritbutionJs: JsValue = (jsonDistribution \ "Distribution").get

      val counts = MonitorDevRepoUtil.labelGroupBy(distritbutionJs)

      val valid = counts.validate[Seq[Distribution]]
      val results = valid match {
        case s: JsSuccess[Seq[Distribution]] => s.get
        case e: JsError => Seq()
      }
      Seq(results.head)
    }

    def catalogBrokenLinks(catalogName: String): Seq[BrokenLink] = {
      val brokenLinkJs: JsValue = ((jsonBrokenLink \ "BrokenLink") \ catalogName).get
      val brokenLinks: JsResult[Seq[BrokenLink]] = brokenLinkJs.validate[Seq[BrokenLink]]
      val results = brokenLinks match {
        case s: JsSuccess[Seq[BrokenLink]] => s.get
        case e: JsError => Seq()
      }
      results
    }

    def allBrokenLinks(): Seq[BrokenLink] = {
      val brokenLinkJs: JsValue = (jsonBrokenLink \ "BrokenLink").get
      val keys = brokenLinkJs match {
        case o: JsObject => o.keys //++ o.values.flatMap(allKeys)
        case _ => Set()
      }

      val flattened: JsValue =  (brokenLinkJs \ keys.head).get

      val brokenLinks: JsResult[Seq[BrokenLink]] = flattened.validate[Seq[BrokenLink]]
      val results = brokenLinks match {
        case s: JsSuccess[Seq[BrokenLink]] => s.get
        case e: JsError => Seq()
      }
      println("TEST aooo ...")
      println(results)
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

      def datasetCounts() :Seq[Distribution] = {
        Seq(Distribution(None,None))
      }

      def allDistributionLiceses():Seq[Distribution] = {
        Seq(Distribution(None,None),Distribution(None,None))
      }

      def allDistributionFormat():Seq[Distribution] = {
        Seq(Distribution(None,None),Distribution(None,None))
      }

      def allDistributionGroup():Seq[Distribution] = {
        Seq(Distribution(None,None),Distribution(None,None))
      }

      def catalogDatasetCount(catalogName: String) : Seq[Distribution] = {
        Seq(Distribution(None,None),Distribution(None,None))
      }

      def catalogBrokenLinks(catalogName: String): Seq[BrokenLink] = {
        Seq(BrokenLink(None,None,None,None,None,None,None))
      }

      def allBrokenLinks(): Seq[BrokenLink] = {
        Seq(BrokenLink(None,None,None,None,None,None,None))
      }
    }

  }

