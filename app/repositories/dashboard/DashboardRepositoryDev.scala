package repositories.dashboard

import java.io.File
import java.util.Date

import ftd_api.yaml.{Catalog, Dashboard, DashboardIframes, Success, UserStory}
import java.nio.file.Files
import java.nio.file.StandardCopyOption

import play.api.libs.json.{JsArray, JsObject, Json}

import scala.concurrent.Future
import scala.io.Source


/**
  * Created by ale on 14/04/17.
  */
class DashboardRepositoryDev extends DashboardRepository {

  import scala.concurrent.ExecutionContext.Implicits._

  def save(upFile: File, tableName: String, fileType: String): Success = {
    val message = s"Table created  $tableName"
    val fileName = new Date().getTime() + ".txt"
    val currentPath = new java.io.File(".").getCanonicalPath
    val copyFile = new File(System.getProperty("user.home") + "/metabasefile/" + tableName)
    copyFile.mkdirs()
    val copyFilePath = copyFile.toPath
    Files.copy(upFile.toPath, copyFilePath, StandardCopyOption.REPLACE_EXISTING)
    if (fileType.toLowerCase.equals("json")) {
      val fileString = Source.fromFile(upFile).getLines().mkString
      val jsonArray: Option[JsArray] = DashboardUtil.toJson(fileString)
      val readyToBeSaved = DashboardUtil.convertToJsonString(jsonArray)
      readyToBeSaved.foreach(x => {
        val jsonStr = x.toString()
        println(jsonStr)
      })
    } else if (fileType.toLowerCase.equals("csv")) {
      val csvs = DashboardUtil.trasformMap(upFile)
      val jsons: Seq[JsObject] = csvs.map(x => Json.toJson(x).as[JsObject])
      jsons.foreach(x => {
        val jsonStr = x.toString()
        println(jsonStr)
      })
    }
    Success(Some(message), Some("Good!!"))
  }

  def update(upFile: File, tableName: String, fileType: String): Success = {
    val message = s"Table created  $tableName"
    val fileName = new Date().getTime() + ".txt"
    val copyFile = new File(System.getProperty("user.home") + "/metabasefile/" + tableName)
    copyFile.mkdirs()
    val copyFilePath = copyFile.toPath
    Files.copy(upFile.toPath, copyFilePath, StandardCopyOption.REPLACE_EXISTING)
    Success(Some(message), Some("Good!!"))
  }

  def tables(): Seq[Catalog] = {
    Seq(Catalog(None), Catalog(None))
  }

  def iframesByOrg(user: String,org: String): Future[Seq[DashboardIframes]] = {
    Future(Seq(DashboardIframes(None,None,None,None,None, None)))
  }

  def iframes(metaUser :String) :Future[Seq[DashboardIframes]] = {
    Future(Seq(DashboardIframes(None,None,None,None,None,None)))
  }

  def dashboards(groups: List[String], status: Option[Int]): Seq[Dashboard] = {
    Seq(Dashboard(None, None, None, None, None, None, None, None, None, None))
  }

  def dashboardById(groups: List[String], id: String): Dashboard = {
    Dashboard(None, None, None, None, None, None, None, None, None, None)
  }

  def saveDashboard(dashboard: Dashboard, user: String): Success = {
    Success(None, None)
  }

  def deleteDashboard(dashboardId: String): Success = {
    Success(None, None)
  }

  def stories(groups: List[String], status: Option[Int], page: Option[Int], limit: Option[Int]): Seq[UserStory] = {
    Seq(UserStory(None, None, None, None, None, None, None, None, None, None))
  }

  def storyById(groups: List[String], id: String): UserStory = {
    UserStory(None, None, None, None, None, None, None, None, None, None)
  }

  def publicStoryById(id: String): UserStory = {
    UserStory(None, None, None, None, None, None, None, None, None, None)
  }

  def saveStory(story: UserStory, user: String): Success = {
    Success(None, None)
  }

  def deleteStory(storyId: String): Success = {
    Success(None, None)
  }

  def storiesPublic(status: Option[Int]): Seq[UserStory] = {
    Seq(UserStory(None, None, None, None, None, None, None, None, None, None))
  }

  def dashboardsPublic(status: Option[Int]): Seq[Dashboard] = {
    Seq(Dashboard(None, None, None, None, None, None, None, None, None, None))
  }

  def publicDashboardById(id: String): Dashboard = {
    Dashboard(None, None, None, None, None, None, None, None, None, None)
  }
}
