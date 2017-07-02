package repositories.ckan

import play.api.libs.json.JsValue

/**
  * Created by ale on 01/07/17.
  */
trait CkanRepository {
  def createDataset(jsonDataset: JsValue): Unit
  def dataset(datasetId: String): JsValue
}

object CkanRepository {
  def apply(config: String): CkanRepository = config match {
    case "dev" => new CkanRepositoryDev
    case "prod" => new CkanRepositoryProd
  }
}

trait CkanRepositoryComponent {
  val ckanRepository :CkanRepository
}