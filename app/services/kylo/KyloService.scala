package services.kylo

import java.io.File

import ftd_api.yaml.Error
import play.api.libs.ws.WSClient
import repositories.kylo.{KyloRepository, KyloRepositoryComponent}
import play.api.{Configuration, Environment}

import scala.concurrent.Future

trait KyloServiceComponent {
  this: KyloRepositoryComponent => val kyloService: KyloService

  class KyloService {

    def inferSchema(fileType: String, sampleFile: File, ws: WSClient): Future[Either[Error, String]] = {
      kyloRepository.inferSchema(fileType, sampleFile, ws)
    }

  }

}

object KyloRegistry extends KyloServiceComponent with KyloRepositoryComponent {
  val conf = Configuration.load(Environment.simple())
  val app = conf.getString("app.type").getOrElse("dev")
  val kyloRepository = KyloRepository(app)
  val kyloService = new KyloService

}
