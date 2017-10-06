package services.dashboard

import java.io.File

import ftd_api.yaml.{Catalog, Dashboard, DashboardIframes, Success, UserStory}
import play.api.{Configuration, Environment}
import repositories.dashboard.{DashboardRepository, DashboardRepositoryComponent}

import scala.concurrent.Future



/**
  * Created by ale on 16/04/17.
  */
trait DashboardServiceComponent {
  this: DashboardRepositoryComponent =>
  val dashboardService: DashboardService

  class DashboardService {
    def save(upFile :File,tableName :String, fileType :String) :Success = {
      val result = dashboardRepository.save(upFile,tableName, fileType)
      result
    }

    def update(upFile :File,tableName :String, fileType :String) :Success = {
      val result = dashboardRepository.update(upFile,tableName, fileType)
      result
    }

    def tables() : Seq[Catalog] = {
       dashboardRepository.tables()
    }

    def iframes(metaUser :String) :Future[Seq[DashboardIframes]] = {
      dashboardRepository.iframes(metaUser)
    }

    def dashboards(user :String): Seq[Dashboard] = {
       dashboardRepository.dashboards(user)
    }

    def dashboardById(user: String, id: String) :Dashboard = {
      dashboardRepository.dashboardById(user, id)
    }

    def saveDashboard(dashboard: Dashboard, user :String): Success = {
        dashboardRepository.saveDashboard(dashboard, user)
    }

    def deleteDashboard(dashboardId :String): Success = {
      dashboardRepository.deleteDashboard(dashboardId)
    }

    def stories(user :String, status: Option[Int], page :Option[Int], limit :Option[Int]): Seq[UserStory] = {
      dashboardRepository.stories(user, status, page, limit)
    }

    def storyById(user: String, id: String) :UserStory = {
      dashboardRepository.storyById(user,id)
    }

    def saveStory(story: UserStory, user :String): Success = {
      dashboardRepository.saveStory(story, user)
    }

    def deleteStory(storyId :String): Success = {
      dashboardRepository.deleteStory(storyId)
    }
  }
}


object DashboardRegistry extends
  DashboardServiceComponent with
  DashboardRepositoryComponent
{
  val conf = Configuration.load(Environment.simple())
  val app: String = conf.getString("app.type").getOrElse("dev")
  val dashboardRepository =  DashboardRepository(app)
  val dashboardService = new DashboardService
}
