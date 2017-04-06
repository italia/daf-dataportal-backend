package repositories

import ftd_api.yaml.Catalog

import scala.collection.immutable.List

/**
  * Created by ale on 31/03/17.
  */


trait MonitorRepositoryComponent {
  val monitorRepository : Repository //= new MonitorRepository
  trait Repository{
    def allCatalogs(): List[Catalog]
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
      List(Catalog(Some("ale")), Catalog(Some("ale")))
    }
  }

  class MonitorRepositoryProd extends Repository {
    def allCatalogs(): List[Catalog] = {
      println("Lists of catalogs")
      List(Catalog(None), Catalog(None))
    }
  }
}
