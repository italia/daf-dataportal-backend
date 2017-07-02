package repositories.ckan

import java.io.{FileInputStream, PrintWriter}

import play.Environment
import play.api.libs.json.{JsString, JsValue, Json}

/**
  * Created by ale on 01/07/17.
  */
class CkanRepositoryDev extends  CkanRepository{

  private def readDataset():JsValue = {
    val streamDataset = new FileInputStream(Environment.simple().getFile("data/Dataset.json"))
    try {
      Json.parse(streamDataset)
    }catch {
      case tr: Throwable => tr.printStackTrace(); JsString("Empty file")
    }finally {
      streamDataset.close()
    }
  }

  private val datasetWriter = new PrintWriter(Environment.simple().getFile("data/Dataset.json"))

  def createDataset( jsonDataset: JsValue ): Unit = try {
    datasetWriter.println(jsonDataset.toString)
  } finally {
    datasetWriter.flush()
  }

  def dataset(datasetId: String): JsValue = {
    readDataset()
  }

}
