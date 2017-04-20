package repositories.monitor

import play.api.libs.json.{JsArray, JsObject, JsValue, Json}

import scala.concurrent.Future



/**
  * Created by ale on 07/04/17.
  */

import javax.inject._
import play.api.libs.ws._


object MonitorDevRepoUtil {

  private def allKeys(json: JsValue): collection.Set[String] = json match {
    case o: JsObject => o.keys //++ o.values.flatMap(allKeys)
    case _ => Set()
  }

  private def reducer(j :JsValue, label:String) :JsValue = {
    val sum = (j \\ "count").map(_.as[Float]).reduce(_ + _)
    Json.obj("label" -> label, "count" -> sum)
  }

  def labelGroupBy(distributionJs :JsValue) :JsValue = {
    val keyVals: collection.Set[String] = allKeys(distributionJs)
    val inter: Seq[JsValue] = keyVals.map(x => {
      reducer((distributionJs \ x).get, x)
    }).toSeq


    val counts: JsValue = JsArray(keyVals.map(x => {
      reducer((distributionJs \ x).get, x)
     }).toSeq)
    counts
  }


}
