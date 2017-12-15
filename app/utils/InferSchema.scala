package utils

import scala.util.Try

object InferSchema {

  val SEPARATORS = Seq(",", ";", "|", ".|.", "\t", ":")

  implicit class CastString(val s: String) {

    private val castToInt = (x: String) => Try({
      x.toInt;
      Some(("int"))
    }).getOrElse(None)

    private val castToDouble = (x: String) => if (x.contains(".")) Try({
      x.toDouble;
      Some(("double"))
    }).getOrElse(None) else None

    private val castoToBoolean = (x: String) => Try({
      x.toBoolean;
      Some("boolean")
    }).getOrElse(None)

    private val castoToString = (x: String) => Try(Some("string")).getOrElse(None)

    val castingFunctions = Seq(castToInt, castToDouble, castoToBoolean, castoToString)

    def castToString: Seq[Option[String]] = castingFunctions.map(_ (s))

  }

  def countSubstring(str: String, sub: String): Int =
    str.sliding(sub.length).count(_ == sub)

  def inferSeparator(header: String, supportedSeparators: Seq[String]): String = {
    val max = supportedSeparators.map(x => {
      (x, countSubstring(header,x))
    }).maxBy(_._2)
    max._1
  }

}
