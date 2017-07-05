package services.ckan

import ftd_api.yaml.Dataset
import play.api.{Configuration, Environment}
import play.api.libs.json.{JsResult, JsValue}
import repositories.ckan.{CkanRepository, CkanRepositoryComponent}

import scala.concurrent.Future
/**
  * Created by ale on 01/07/17.
  */

trait CkanServiceComponent {
  this: CkanRepositoryComponent =>
  val ckanService: CkanService

  class CkanService {
    def createDataset(jsonDataset: JsValue): Unit = {
         ckanRepository.createDataset(jsonDataset)
    }
    def dataset(datasetId: String): JsValue = {
      ckanRepository.dataset(datasetId)
    }

    def testDataset(datasetId :String) : Future[JsResult[Dataset]] = {
        ckanRepository.testDataset(datasetId)
    }

  }
}

object CkanRegistry extends
  CkanServiceComponent with
  CkanRepositoryComponent {
     val conf = Configuration.load(Environment.simple())
     val app: String = conf.getString("app.type").getOrElse("dev")
     val ckanRepository =  CkanRepository(app)
     val ckanService = new CkanService
}