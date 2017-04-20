package utils

import javax.inject.Inject

import play.api.{Configuration, Environment}

/**
  * Created by ale on 16/04/17.
  */
class AppConfig @Inject()(playConfig: Configuration) {
  val dbHost: Option[String] = playConfig.getString("mongo.host")
  val dbPort: Option[Int] = playConfig.getInt("mongo.port")
}

object ConfigReader {
  private val config = new AppConfig(Configuration.load(Environment.simple()))
  def getDbHost: String = config.dbHost.getOrElse("localhost")
  def getDbPort: Int = config.dbPort.getOrElse(27017)
}
