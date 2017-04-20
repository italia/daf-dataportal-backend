package repositories.dashboard

import java.io.File

import ftd_api.yaml.{Catalog, Success}

/**
  * Created by ale on 14/04/17.
  */
trait DashboardRepository {
   def save(upFile :File,tableName :String) :Success
   def update(upFile :File,tableName :String) :Success
   def tables(): Seq[Catalog]
}

object DashboardRepository {
  def apply(config: String): DashboardRepository = config match {
    case "dev" => new DashboardRepositoryDev
    case "prod" => new DashboardRepositoryProd
  }
}

trait DashboardRepositoryComponent {
  val dashboardRepository :DashboardRepository
}


