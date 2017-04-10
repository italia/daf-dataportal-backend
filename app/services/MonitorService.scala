package services

import scala.collection.immutable.List
import ftd_api.yaml.{BrokenLink, Catalog, Distribution}
import repositories.MonitorRepositoryComponent
import repositories.monitor.Repository




trait MonitorServiceComponent { this: MonitorRepositoryComponent =>
  val monitorService  :MonitorService//= new MonitorService
  class MonitorService {
    def allCatalogs(): List[Catalog] =
      monitorRepository.allCatalogs()

    def datasetCatalogLicenses(catalogName :String): Seq[Distribution] = {
      monitorRepository.datasetCatalogLicenses(catalogName)
    }

    def datasetCatalogFormat(catalogName :String): Seq[Distribution] = {
      monitorRepository.datasetCatalogFormat(catalogName)
    }

    def datasetCatalogGroup(catalogName :String): Seq[Distribution] = {
      monitorRepository.datasetCatalogGroup(catalogName)
    }

     def datasetsCount() :Seq[Distribution] = {
        monitorRepository.datasetCounts()
     }

    def allDistributionLiceses():Seq[Distribution] = {
      monitorRepository.allDistributionLiceses()
    }

    def allDistributionFormat():Seq[Distribution] = {
      monitorRepository.allDistributionFormat()
    }

    def allDistributionGroup():Seq[Distribution] = {
      monitorRepository.allDistributionGroup()
    }

    def catalogDatasetCount(catalogName: String) : Seq[Distribution] = {
      monitorRepository.catalogDatasetCount(catalogName)
    }

    def catalogBrokenLinks(catalogName: String) :Seq[BrokenLink] = {
      monitorRepository.catalogBrokenLinks(catalogName)
    }

    def allBrokenLinks() :Seq[BrokenLink] = {
      monitorRepository.allBrokenLinks()
    }
  }

}


object ComponentRegistry extends
  MonitorServiceComponent with
  MonitorRepositoryComponent
{
  val monitorRepository =  Repository("dev")
  val monitorService = new MonitorService
}
