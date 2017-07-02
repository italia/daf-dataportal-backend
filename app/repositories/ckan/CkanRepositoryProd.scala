package repositories.ckan

import play.api.libs.json.{JsString, JsValue}

/**
  * Created by ale on 01/07/17.
  */
class CkanRepositoryProd  extends CkanRepository{

  def createDataset(jsonDataset: JsValue): Unit = println("TODO")
  def dataset(datasetId: String): JsValue = JsString("TODO")

}
