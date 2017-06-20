package repositories.monitor

import ftd_api.yaml.{BrokenLink, Catalog, Distribution}
import play.api.libs.json.{JsPath, Reads}
import scala.collection.immutable.List

/**
  * Created by ale on 07/04/17.
  */
trait MonitorRepository {

  import play.api.libs.functional.syntax._


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

}

object MonitorRepository {
  def apply(config: String): MonitorRepository = config match {
    case "dev" => new MonitorRepositoryDev
    case "prod" => new MonitorRepositoryProd
  }
}