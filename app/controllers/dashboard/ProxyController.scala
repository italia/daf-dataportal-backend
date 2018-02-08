package controllers.dashboard

import javax.inject.Inject

import play.api.cache.CacheApi
import play.api.inject.ConfigurationProvider
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc.{Action, Controller}

import scala.concurrent.{ExecutionContext, Future}

class ProxyController @Inject()(ws: WSClient,
                                cache: CacheApi,
                                config: ConfigurationProvider
                                )(implicit context: ExecutionContext) extends Controller {


  def medium(url: String) = Action.async { implicit request =>
    val scheme = url.matches("^http[s]?://.*") match {
      case true => ""
      case false => "https://"
    }
    val responseWs: Future[WSResponse] = ws.url(scheme + url).get
    responseWs.map { response =>
      Ok(response.body).as("text/xml")
    }
  }

  def ckanProxy(action: String) = Action.async { implicit request =>
    val baseUrl = "http://dcatapit.geo-solutions.it/api/3/action/"
    val url = baseUrl + action
    val queryString: Map[String, String] = request.queryString.map { case (k,v) => k -> v.mkString}
    val responseWs = ws.url(url)
      .withQueryString(queryString.toList: _*)
      .get
    responseWs.map { response =>
      Ok(response.body).as("application/json")
    }
  }
}
