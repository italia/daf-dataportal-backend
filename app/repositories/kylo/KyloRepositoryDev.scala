package repositories.kylo

import java.io.File
import ftd_api.yaml.Error
import play.api.libs.ws.WSClient

import scala.concurrent.Future

class KyloRepositoryDev extends KyloRepository {
  override def inferSchema(fileType: String, sampleFile: File, ws: WSClient): Future[Either[Error, String]] = {
    Future.successful(Right("success"))
  }
}
