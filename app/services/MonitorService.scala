package services

import scala.collection.immutable.List
import ftd_api.yaml.{Catalog, Distribution}
import repositories.MonitorRepositoryComponent
import play.api.Environment



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
  }

}


object ComponentRegistry extends
  MonitorServiceComponent with
  MonitorRepositoryComponent
{
  val monitorRepository =  Repository("dev")
  val monitorService = new MonitorService
}
