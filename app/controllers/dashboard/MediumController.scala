package controllers.dashboard

import javax.inject.Inject

import play.api.cache.CacheApi
import play.api.inject.ConfigurationProvider
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc.{Action, Controller}

import scala.concurrent.{ExecutionContext, Future}

class MediumController @Inject()(ws: WSClient,
                                 cache: CacheApi,
                                 config: ConfigurationProvider
                                )(implicit context: ExecutionContext) extends Controller {


  def medium(url: String) = Action.async { implicit request =>
    val scheme = url.matches("^http[s]?://[a-zA-Z0-9.@/]*") match {
      case true => ""
      case false => "https://"
    }
    val responseWs: Future[WSResponse] = ws.url(scheme + url).get
    responseWs.map { response =>
      Ok(response.body).as("text/xml")
    }
  }
}
