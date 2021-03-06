package repositories.dashboard

import java.io.File

import ftd_api.yaml.{Catalog, Dashboard, DashboardIframes, DataApp, Error, Filters, Organization, SearchResult, Success, SupersetTable, UserStory}
import play.api.libs.ws.WSClient

import scala.concurrent.Future

/**
  * Created by ale on 14/04/17.
  */
trait DashboardRepository {
  def save(upFile: File, tableName: String, fileType: String, wsClient: WSClient): Success
  def update(upFile: File, tableName: String, fileType: String): Success
  def tables(): Seq[Catalog]
  def iframes(metaUser: String, wsClient: WSClient): Future[Seq[DashboardIframes]]
  def iframesByOrg(user: String,org: String, wsClient: WSClient): Future[Seq[DashboardIframes]]
  def dashboards(username:String, groups: List[String], status: Option[Int]): Seq[Dashboard]
  def dashboardById(username: String, groups: List[String], id: String): Option[Dashboard]
  def saveDashboard(dashboard: Dashboard, user: String, shared: Option[Boolean], token: String, wsClient: WSClient): Success
  def deleteDashboard(dashboardId: String): Success
  def stories(username: String, groups: List[String], status: Option[Int], page: Option[Int], limit: Option[Int]): Seq[UserStory]
  def storyById(username:String, groups: List[String], id: String): Option[UserStory]
  def publicStoryById(id: String): Option[UserStory]
  def saveStory(story: UserStory, user: String, shared: Option[Boolean], token: String, wsClient: WSClient): Success
  def deleteStory(storyId: String): Success
  def storiesPublic(org: Option[String]): Seq[UserStory]
  def dashboardsPublic(org: Option[String]): Seq[Dashboard]
  def publicDashboardById(id: String): Option[Dashboard]
  def getAllDataApp: Seq[DataApp]
  def getSupersetTableByTableNameIdAndOrgs(user: String, tableName: String, orgs: Seq[Organization], ws: WSClient): Future[Either[Error, Seq[SupersetTable]]]
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


