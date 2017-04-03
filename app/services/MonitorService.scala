package services

import scala.collection.immutable.List
import ftd_api.yaml.Catalog
import repositories.MonitorRepositoryComponent

//Model
//case class Catalog(name :String)





trait MonitorServiceComponent { this: MonitorRepositoryComponent =>
  val monitorService  :MonitorService//= new MonitorService
  class MonitorService {
    def allCatalogs(): List[Catalog] =
      monitorRepository.allCatalogs()
  }
}


object ComponentRegistry extends
  MonitorServiceComponent with
  MonitorRepositoryComponent
{
  val monitorRepository =  Repository("dev")
  val monitorService = new MonitorService
}
