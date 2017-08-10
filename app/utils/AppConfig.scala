package utils

import javax.inject.Inject

import play.api.{Configuration, Environment}

/**
  * Created by ale on 16/04/17.
  */
class AppConfig @Inject()(playConfig: Configuration) {
  val dbHost: Option[String] = playConfig.getString("mongo.host")
  val dbPort: Option[Int] = playConfig.getInt("mongo.port")
  val ckanHost  = playConfig.getString("app.ckan.url")
  val ckanApiKey =  playConfig.getString("app.ckan.auth.token")
  val localUrl = playConfig.getString("app.local.url")

  val metabaseURL= playConfig.getString("metabase.url")
  val metauser = playConfig.getString("metabase.user")
  val metapass = playConfig.getString("metabase.pass")
}

object ConfigReader {
  private val config = new AppConfig(Configuration.load(Environment.simple()))
  def getDbHost: String = config.dbHost.getOrElse("localhost")
  def getDbPort: Int = config.dbPort.getOrElse(27017)
  def getCkanHost = config.ckanHost.getOrElse("localhost")
  def getCkanApiKey = config.ckanApiKey.getOrElse("dsadsadas")
  def getLocalUrl = config.localUrl.getOrElse("http://localhost:9000")

  def getMetabaseUrl = config.metabaseURL.getOrElse("http://localhost:13479")
  def getMetaUser = config.metauser.getOrElse("ale.ercolani@gmail.com")
  def getMetaPass = config.metapass.getOrElse("password")
}
