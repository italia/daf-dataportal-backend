package repositories.dashboard

import java.io.File

import ftd_api.yaml.{Catalog, Dashboard, DashboardIframes, Success, UserStory}

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
   def saveDashboard(dashboard: Dashboard, user :String): Success
   def deleteDashboard(dashboardId :String): Success
   def stories(user :String, status :Option[Int], page :Option[Int], limit :Option[Int]): Seq[UserStory]
   def storyById(user: String, id: String) :UserStory
   def saveStory(story: UserStory, user :String): Success
   def deleteStory(storyId :String): Success
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


