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

  val supersetURL= playConfig.getString("superset.url")
  val supersetOpenDataUrl = playConfig.getString("superset.openDataUrl")
  val supersetUser = playConfig.getString("superset.user")
  val supersetPass = playConfig.getString("superset.pass")
  val supersetOpenDataUser = playConfig.getString("superset.openDataUser")
  val supersetOpenDataPwd = playConfig.getString("superset.openDataPwd")

  val grafanaURL= playConfig.getString("grafana.url")

  val tdMetabaseURL = playConfig.getString("tdmetabase.url")

  val userName :Option[String] = playConfig.getString("mongo.username")
  val password :Option[String] = playConfig.getString("mongo.password")
  val database :Option[String] = playConfig.getString("mongo.database")

  val securityManHost :Option[String] = playConfig.getString("security.manager.host")

  val cookieExpiration :Option[Long] = playConfig.getLong("cookie.expiration")

  val kyloUrl : Option[String] = playConfig.getString("kylo.url")
  val kyloInferUrl : Option[String] = playConfig.getString("kylo.inferUrl")
  val kyloSystemUrl : Option[String] = playConfig.getString("kylo.systemUrl")
  val kyloUser : Option[String] = playConfig.getString("kylo.user")
  val kyloPassword : Option[String] = playConfig.getString("kylo.pwd")
  val kyloCsvSerde : Option[String] = playConfig.getString("kylo.csvSerde")
  val kyloJsonSerde : Option[String] = playConfig.getString("kylo.jsonSerde")

  val elasticsearchUrl = playConfig.getString("elasticsearch.url")
  val elasticsearchPort = playConfig.getInt("elasticsearch.port")

  val kafkaProxy: Option[String] = playConfig.getString("kafka-proxy.url")

  val datasetUrl: Option[String] = playConfig.getString("dataset-manager.url")
  val datasetUserOpendataEmail: Option[String] = playConfig.getString("dataset-manager.email")
  val datasetUserOpendataPwd: Option[String] = playConfig.getString("dataset-manager.pwd")



}

object ConfigReader {
  private val config = new AppConfig(Configuration.load(Environment.simple()))

  require(config.elasticsearchUrl.nonEmpty, "A elasticsearch url must be specified")

  def getDbHost: String = config.dbHost.getOrElse("localhost")
  def getDbPort: Int = config.dbPort.getOrElse(27017)
  def getCkanHost = config.ckanHost.getOrElse("localhost")
  def getCkanApiKey = config.ckanApiKey.getOrElse("dsadsadas")
  def getLocalUrl = config.localUrl.getOrElse("http://localhost:9000")

  def getMetabaseUrl = config.metabaseURL.getOrElse("http://localhost:13479")
  def getMetaUser = config.metauser.getOrElse("ale.ercolani@gmail.com")
  def getMetaPass = config.metapass.getOrElse("password")

  def getSupersetUrl = config.supersetURL.getOrElse("http://localhost:8088")
  def getSupersetOpenDataUrl = config.supersetOpenDataUrl.getOrElse("")
  def getSupersetUser = config.supersetUser.getOrElse("alessandro")
  def getSupersetPass = config.supersetPass.getOrElse("password")
  def getSupersetOpenDataUser = config.supersetOpenDataUser.getOrElse("")
  def getSupersetOpenDataPwd = config.supersetOpenDataPwd.getOrElse("")

  def getGrafanaUrl = config.grafanaURL.getOrElse("TO DO")

  def getTdMetabaseURL = config.tdMetabaseURL.getOrElse("https://dashboard.teamdigitale.governo.it")

  def database :String = config.database.getOrElse("monitor_mdb")
  def password :String = config.password.getOrElse("")
  def userName :String = config.userName.getOrElse("")
  def securityManHost :String = config.securityManHost.getOrElse("xxx")

  def cookieExpiration:Long = config.cookieExpiration.getOrElse(30L)// 30 min by default
  // TODO think about a defaul ingestion mechanism without kylo
  def kyloUrl = config.kyloUrl.getOrElse("No default")
  def kyloInferUrl = config.kyloInferUrl.getOrElse("No default")
  def kyloSystemUrl = config.kyloSystemUrl.getOrElse("No default")
  def kyloCsvSerde = config.kyloCsvSerde.getOrElse("No default")
  def kyloJsonSerde = config.kyloJsonSerde.getOrElse("No default")
  def kyloUser = config.kyloUser.getOrElse("dladmin")
  def kyloPwd = config.kyloPassword.getOrElse("XXXXXXXXX")

  def getElasticsearchUrl = config.elasticsearchUrl.get
  def getElasticsearchPort = config.elasticsearchPort.getOrElse(9200)

  def getKafkaProxy = config.kafkaProxy.getOrElse("localhost:8085")

  def getDatasetUrl =  config.datasetUrl.getOrElse("XXXXX")
  def getDatasetUserOpendataEmail = config.datasetUserOpendataEmail.getOrElse("XXXXX")
  def getDatasetUserOpendataPwd = config.datasetUserOpendataPwd.getOrElse("XXXXXXX")

}
