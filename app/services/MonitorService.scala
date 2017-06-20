package services

import scala.collection.immutable.List
import ftd_api.yaml.{BrokenLink, Catalog, Distribution}
import play.api.{Configuration, Environment}
import repositories.MonitorRepositoryComponent
import repositories.monitor.MonitorRepository



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
  val conf = Configuration.load(Environment.simple())
  val app: String = conf.getString("app.type").getOrElse("dev")
  val monitorRepository =  MonitorRepository(app)
  val monitorService = new MonitorService
}
