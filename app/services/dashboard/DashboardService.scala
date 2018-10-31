package services.dashboard

import java.io.File

import ftd_api.yaml.{Catalog, Dashboard, DashboardIframes, DataApp, Error, Filters, Organization, SearchResult, Success, SupersetTable, UserStory}
import play.api.libs.ws.WSClient
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
    def save(upFile :File,tableName :String, fileType :String, wsClient: WSClient) :Success = {
      val result = dashboardRepository.save(upFile,tableName, fileType, wsClient)
      result
    }

    def update(upFile :File,tableName :String, fileType :String) :Success = {
      val result = dashboardRepository.update(upFile,tableName, fileType)
      result
    }

    def tables() : Seq[Catalog] = {
       dashboardRepository.tables()
    }

    def iframes(metaUser :String, wsClient: WSClient) :Future[Seq[DashboardIframes]] = {
      dashboardRepository.iframes(metaUser, wsClient)
    }

    def iframesByOrg(user: String,org: String, wsClient: WSClient): Future[Seq[DashboardIframes]] = {
      dashboardRepository.iframesByOrg(user,org, wsClient)
    }

    def dashboards(username: String, groups: List[String], status: Option[Int]): Seq[Dashboard] = {
       dashboardRepository.dashboards(username, groups, status)
    }

    def dashboardById(username: String, group: List[String], id: String): Option[Dashboard] = {
      dashboardRepository.dashboardById(username, group, id)
    }

    def saveDashboard(dashboard: Dashboard, user :String, shared: Option[Boolean], token: String, wsClient: WSClient): Success = {
        dashboardRepository.saveDashboard(dashboard, user, shared, token, wsClient)
    }

    def deleteDashboard(dashboardId :String): Success = {
      dashboardRepository.deleteDashboard(dashboardId)
    }

    def stories(username: String, groups: List[String], status: Option[Int], page :Option[Int], limit :Option[Int]): Seq[UserStory] = {
      dashboardRepository.stories(username, groups, status, page, limit)
    }

    def storyById(username: String, group: List[String], id: String): Option[UserStory] = {
      dashboardRepository.storyById(username, group,id)
    }

    def publicStoryById(id: String): Option[UserStory] = {
      dashboardRepository.publicStoryById(id)
    }

    def saveStory(story: UserStory, user :String, shared: Option[Boolean], token: String, wsClient: WSClient): Success = {
      dashboardRepository.saveStory(story, user, shared, token, wsClient)
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

    def publicDashboardById(id: String): Option[Dashboard] = {
      dashboardRepository.publicDashboardById(id)
    }

    def searchText(filters: Filters, username: String, groups: List[String]): Future[List[SearchResult]] = {
      dashboardRepository.searchText(filters, username, groups)
    }

    def searchLast(username: String, groups: List[String]): Future[List[SearchResult]] = {
      dashboardRepository.searchLast(username, groups)
    }
    def searchLastPublic(org: Option[String]): Future[List[SearchResult]] = {
      dashboardRepository.searchLastPublic(org)
    }

    def searchTextPublic(filters: Filters): Future[List[SearchResult]] = {
      dashboardRepository.searchTextPublic(filters)
    }

    def getAllDataApp: Seq[DataApp] = {
      dashboardRepository.getAllDataApp
    }

    def getSupersetTableByTableNameIdAndOrgs(user: String, tableName: String, orgs: Seq[Organization], ws: WSClient): Future[Either[Error, Seq[SupersetTable]]]  = {
      dashboardRepository.getSupersetTableByTableNameIdAndOrgs(user, tableName, orgs, ws)
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
