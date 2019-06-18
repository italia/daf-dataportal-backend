package repositories.kylo

import java.io.File

import play.api.libs.ws.WSClient
import ftd_api.yaml.Error
import scala.concurrent.Future

trait KyloRepository {
  def inferSchema(fileType: String, sampleFile: File, ws: WSClient): Future[Either[Error, String]]

}

object KyloRepository {
  def apply(config: String): KyloRepository = config match {
    case "dev"  => new KyloRepositoryDev
    case "prod" => new KyloRepositoryProd
  }
}

trait KyloRepositoryComponent {
  val kyloRepository: KyloRepository
}
