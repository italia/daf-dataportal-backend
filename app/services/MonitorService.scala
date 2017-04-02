package services

import scala.collection.immutable.List

//Model
case class Catalog(name :String)

/**
  * Created by ale on 31/03/17.
  */
//class MonitorRepository {
//  def allCatalogs(): List[Catalog] = {
//    println("Lists of catalogs")
//    List(Catalog("ale"), Catalog("test"))
//  }
//}

//class MonitorService {
//  val monitorRepository = new MonitorRepository
//  def allCatalogs(): List[Catalog] =
//    monitorRepository.allCatalogs()

//}




trait MonitorRepositoryComponent {
  val monitorRepository : Repository //= new MonitorRepository
  trait Repository{
    def allCatalogs(): List[Catalog]
    // def apply(config :String) :Unit = config match {
    //   case "dev" => new MonitorRepositoryDev
    //   case "prod" => new MonitorRepositoryProd
    // }
  }
  object Repository {
     def apply(config :String) :Repository = config match {
       case "dev" => new MonitorRepositoryDev
       case "prod" => new MonitorRepositoryProd
     }
  }
  class MonitorRepositoryDev extends Repository{
    def allCatalogs(): List[Catalog] = {
      println("Lists of catalogs")
      List(Catalog("ale"), Catalog("test"))
    }
  }
  class MonitorRepositoryProd extends Repository {
    def allCatalogs(): List[Catalog] = {
      println("Lists of catalogs")
      List(Catalog("gigio"), Catalog("prova"))
    }
  }
}



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
