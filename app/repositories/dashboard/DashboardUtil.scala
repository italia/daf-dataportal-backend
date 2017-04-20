package repositories.dashboard

import play.api.libs.json.{JsArray, JsObject, Json}

/**
  * Created by ale on 17/04/17.
  */


object DashboardUtil {
  def toJson(fileString :String) : Option[JsArray] = {
    val json = Json.parse(fileString)
    val result = json match {
      case x : JsArray => Some(x)
      case _ => None
    }
    println(result)
    result
  }

  def convertToJsonString(json :Option[JsArray]) :Seq[JsObject] = {
    val tmp = json.get
    tmp.as[Seq[JsObject]]
  }
}