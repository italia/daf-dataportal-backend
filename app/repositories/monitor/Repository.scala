package repositories.monitor

import ftd_api.yaml.{BrokenLink, Catalog, Distribution}

import scala.collection.immutable.List

/**
  * Created by ale on 07/04/17.
  */
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