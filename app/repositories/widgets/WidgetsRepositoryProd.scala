package repositories.widgets

import java.net.URL

import ftd_api.yaml.{ Error, Widget}
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient
import utils.ConfigReader

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import scala.util.Try

class WidgetsRepositoryProd extends WidgetsRepository {

  private val securityManagerUrl = ConfigReader.securityManHost
  private val localUrl = ConfigReader.getLocalUrl

  private val openDataUser = ConfigReader.getSupersetOpenDataUser


  override def getAllWidgets(user: String, groups: List[String], org: Option[String], wsClient: WSClient): Future[Either[Error, Seq[Widget]]] = {
    org match {
      case Some(organization) =>
        if(groups.contains(organization)){
          val res = for{
            privateWidgets <- getPrivateWidgets(user, organization, wsClient)
            publicWidgets  <- getPublicWidgets(openDataUser, wsClient)
          } yield privateWidgets.toList ::: publicWidgets.toList
          res.map{widgetsList =>
            if(widgetsList.isEmpty) { Logger.logger.debug("widgets not found"); Left(Error(Some(404), Some(s"widgets not found"), None)) }
            else { Logger.logger.debug(s"found ${widgetsList.size} widgets"); Right(widgetsList.sortWith((a, b) => extractToInt(a.identifier) > extractToInt(b.identifier))) }
          }
        } else {
          Future.successful(Left(Error(Some(401), Some(s"not allowed to user $user get widgets of the organization $organization"), None)))
        }

      case None =>
        getPublicWidgets(openDataUser, wsClient).map{publicWidgets =>
          if(publicWidgets.isEmpty) { Logger.logger.debug("widgets not found"); Left(Error(Some(404), Some("widgets not found"), None)) }
          else { Logger.logger.debug(s"found ${publicWidgets.size} public widgets"); Right(publicWidgets.toList) }
        }
    }
  }

  private def getPublicWidgets(user: String, wsClient: WSClient): Future[Seq[Widget]] = {
    widgets(user, wsClient)
  }

  private def getPrivateWidgets(user: String, org: String, wsClient: WSClient): Future[Seq[Widget]] = {
    val result = for {
      tables <- getSupersetTablesByOrg(org, wsClient)
      a <- widgetsByTables(user, tables, wsClient)
    } yield a
    result
  }

  private def getSupersetTablesByOrg(org: String, wsClient: WSClient): Future[Seq[String]] = {

    val url: String = s"$securityManagerUrl/security-manager/v1/internal/superset/find_orgtables/$org"
    Logger.logger.debug("calling security url: " + url)

    wsClient.url(url).get().map { resp =>
      Logger.logger.debug(s"getSupersetTablesByOrg response: $resp")
      (resp.json \ "tables").as[Seq[String]]
    }

  }

  private def widgetsByTables(user: String, tables: Seq[String], wsClient: WSClient): Future[Seq[Widget]] = {
    widgets(user, wsClient).map {
      _.filter(widgets => widgets.table.nonEmpty && tables.contains(widgets.table.getOrElse("")))
    }
  }


  def widgets(user: String, wsClient: WSClient): Future[Seq[Widget]] = {

    val internalSupersetUrl = s"$localUrl/superset/public_slice/$user"

    val supersetResponse = wsClient.url(internalSupersetUrl).get()

    supersetResponse.map { res =>
      Logger.logger.debug(s"$user: response status superset ${res.status}, text ${res.statusText}")
      val jsonSeq = res.json.as[Seq[JsValue]]
      parseWidgetFromSupersetResponse(jsonSeq, true)
    }
  }


  private def parseWidgetFromSupersetResponse(jsonSeq: Seq[JsValue], isPrivate: Boolean) = {
    val widgetsSeq = jsonSeq.map{ json =>
      val slice_link: String = (json \ "slice_link").get.as[String]
      val vizType: String = (json \ "viz_type").get.as[String]
      val title: String = slice_link.slice(slice_link.indexOf(">") + 1, slice_link.lastIndexOf("</a>")).trim
      val src: String = slice_link.slice(slice_link.indexOf("\"") + 1, slice_link.lastIndexOf("\"")) + "&standalone=true"
      val url: String = if(isPrivate) ConfigReader.getSupersetUrl + src else ConfigReader.getSupersetOpenDataUrl + src
      val decodeSuperset = java.net.URLDecoder.decode(url, "UTF-8")
      val uri = new URL(decodeSuperset)
      val queryString = uri.getQuery.split("&", 2).head
      val valore = queryString.split("=", 2)(1)
      if(valore.contains("{\"code") || vizType.equals("separtor") || vizType.equals("filter")) {
        Left(Widget(identifier = "", viz_type = "", origin = "", title = "", pvt = false, table = None, widget_url = None, text = None))
      } else {
        Try{
          val identifierJson = Json.parse(s"""$valore""")
          val slice_id: Int = (identifierJson \ "slice_id").asOpt[Int].getOrElse(0)
          val table: String = (json \ "datasource_link").get.asOpt[String].getOrElse("").split(">").last.split("<").head
          Right(Widget(
            identifier = s"superset_$slice_id",
            viz_type = vizType,
            origin = if(isPrivate) "superset" else "superset_open",
            title = title,
            pvt = isPrivate,
            table = Some(table),
            widget_url = Some(url),
            text = None
          ))
        }.getOrElse(Left(Widget(identifier = "", viz_type = "", origin = "", title = "", pvt = false, table = None, widget_url = None, text = None)))
      }
    }
    val response = widgetsSeq.filter {
      case Right(_) => true
      case _  => false
    }.collect{ case Right(b) => b }
    response
  }

  private def extractToInt(indentifier: String): Int  = {
    Try{indentifier.split("_")(0).toInt}.getOrElse(0)
  }

}
