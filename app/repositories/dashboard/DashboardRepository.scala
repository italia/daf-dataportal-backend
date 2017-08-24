package repositories.dashboard

import java.io.File

import ftd_api.yaml.{Catalog, DashboardIframes, Success, Dashboard}

import scala.concurrent.Future

/**
  * Created by ale on 14/04/17.
  */
trait DashboardRepository {
   def save(upFile :File,tableName :String, fileType :String) :Success
   def update(upFile :File,tableName :String, fileType :String) :Success
   def tables(): Seq[Catalog]
   def iframes(metaUser :String): Future[Seq[DashboardIframes]]
   def dashboards(user :String): Seq[Dashboard]
   def dashboardById(user: String, id: String) :Dashboard
   def saveDashboard(dashboard: Dashboard): Success
   def deleteDashboard(dashboardId :String): Success
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


