import javax.inject._

import play.api.http.DefaultHttpErrorHandler
import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.routing.Router

import scala.concurrent.Future

import de.zalando.play.controllers.PlayBodyParsing


/**
  * The purpose of this ErrorHandler is to override default play's error reporting with application/json content type.
  */

class ErrorHandler @Inject() (
                               env: Environment,
                               config: Configuration,
                               sourceMapper: OptionalSourceMapper,
                               router: Provider[Router]
                             ) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) {

  private def contentType(request: RequestHeader): String =
    request.acceptedTypes.map(_.toString).filterNot(_ == "text/html").headOption.getOrElse("application/json")

  override def onProdServerError(request: RequestHeader, exception: UsefulException) = {
    implicit val writer = PlayBodyParsing.anyToWritable[Throwable](contentType(request))
    Future.successful(InternalServerError(exception))
  }


  // called when a route is found, but it was not possible to bind the request parameters
  override def onBadRequest(request: RequestHeader, error: String): Future[Result] = {
    implicit val writer = PlayBodyParsing.anyToWritable[String](contentType(request))
    Future.successful(BadRequest("Bad Request: " + error))
  }

  // 404 - page not found error
  override def onNotFound(request: RequestHeader, message: String): Future[Result] = {
    implicit val writer = PlayBodyParsing.anyToWritable[String](contentType(request))
    Future.successful(NotFound(request.path))
  }
}