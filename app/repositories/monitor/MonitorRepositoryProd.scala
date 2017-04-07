package repositories.monitor

import ftd_api.yaml.{BrokenLink, Catalog, Distribution}

import scala.collection.immutable.List

/**
  * Created by ale on 07/04/17.
  */
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
