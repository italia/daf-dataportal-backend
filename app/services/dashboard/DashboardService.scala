package services.dashboard

import java.io.File

import ftd_api.yaml.{Catalog, Dashboard, DashboardIframes, Filters, SearchResult, Success, UserStory}
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

    def iframesByOrg(user: String,org: String): Future[Seq[DashboardIframes]] = {
      dashboardRepository.iframesByOrg(user,org)
    }

    def dashboards(username: String, groups: List[String], status: Option[Int]): Seq[Dashboard] = {
       dashboardRepository.dashboards(username, groups, status)
    }

    def dashboardById(username: String, group: List[String], id: String) :Dashboard = {
      dashboardRepository.dashboardById(username, group, id)
    }

    def saveDashboard(dashboard: Dashboard, user :String): Success = {
        dashboardRepository.saveDashboard(dashboard, user)
    }

    def deleteDashboard(dashboardId :String): Success = {
      dashboardRepository.deleteDashboard(dashboardId)
    }

    def stories(username: String, groups: List[String], status: Option[Int], page :Option[Int], limit :Option[Int]): Seq[UserStory] = {
      dashboardRepository.stories(username, groups, status, page, limit)
    }

    def storyById(username: String, group: List[String], id: String) :UserStory = {
      dashboardRepository.storyById(username, group,id)
    }

    def publicStoryById(id: String) :UserStory = {
      dashboardRepository.publicStoryById(id)
    }

    def saveStory(story: UserStory, user :String): Success = {
      dashboardRepository.saveStory(story, user)
    }

    def deleteStory(storyId :String): Success = {
      dashboardRepository.deleteStory(storyId)
    }

    def storiesPublic(org: Option[String]): Seq[UserStory] = {
      dashboardRepository.storiesPublic(org)
    }

    def dashboardsPublic(org: Option[String]): Seq[Dashboard] = {
      dashboardRepository.dashboardsPublic(org)
    }

    def publicDashboardById(id: String): Dashboard = {
      dashboardRepository.publicDashboardById(id)
    }

    def searchText(filters: Filters, username: String, groups: List[String]): Seq[SearchResult] = {
      dashboardRepository.searchText(filters, username, groups)
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
