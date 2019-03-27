package utils

import javax.inject.Inject

import play.api.{Configuration, Environment}

/**
  * Created by ale on 16/04/17.
  */
class AppConfig @Inject()(playConfig: Configuration) {
  val dbHost: Option[String] = playConfig.getString("mongo.host")
  val dbPort: Option[Int] = playConfig.getInt("mongo.port")
  val ckanHost: Option[String]  = playConfig.getString("app.ckan.url")
  val ckanApiKey: Option[String] =  playConfig.getString("app.ckan.auth.token")
  val localUrl: Option[String] = playConfig.getString("app.local.url")

  val metabaseURL: Option[String]= playConfig.getString("metabase.url")
  val metauser: Option[String] = playConfig.getString("metabase.user")
  val metapass: Option[String] = playConfig.getString("metabase.pass")

  val supersetURL: Option[String]= playConfig.getString("superset.url")
  val supersetOpenDataUrl: Option[String] = playConfig.getString("superset.openDataUrl")
  val supersetUser: Option[String] = playConfig.getString("superset.user")
  val supersetPass: Option[String] = playConfig.getString("superset.pass")
  val supersetOpenDataUser: Option[String] = playConfig.getString("superset.openDataUser")
  val supersetOpenDataPwd: Option[String] = playConfig.getString("superset.openDataPwd")

  val grafanaURL: Option[String] = playConfig.getString("grafana.url")

  val tdMetabaseURL: Option[String] = playConfig.getString("tdmetabase.url")

  val userName :Option[String] = playConfig.getString("mongo.username")
  val userRoot: Option[String] = playConfig.getString("mongo.usernameAdmin")
  val password :Option[String] = playConfig.getString("mongo.password")
  val rootPassword :Option[String] = playConfig.getString("mongo.rootPassword")
  val database :Option[String] = playConfig.getString("mongo.database")
  val databaseRoot: Option[String] = playConfig.getString("mongo.databaseAdmin")
  val collNotificationName: Option[String] = playConfig.getString("mongo.collNotificationName")
  val collSubscriptionName: Option[String] = playConfig.getString("mongo.collSubscriptionName")

  val securityManHost :Option[String] = playConfig.getString("security.manager.host")

  val catalogManagerHost: Option[String] = playConfig.getString("catalog-manager.host")
  val catalogManagerNotificationPath: Option[String] = playConfig.getString("catalog-manager.notificationPath")

  val sysAdminName: Option[String] = playConfig.getString("sys-admin")

  val openDataGroup: Option[String] = playConfig.getString("openDataGroup")
  val openDataUser: Option[String] = playConfig.getString("openDataUser")

  val cookieExpiration :Option[Long] = playConfig.getLong("cookie.expiration")

  val kyloUrl : Option[String] = playConfig.getString("kylo.url")
  val kyloInferUrl : Option[String] = playConfig.getString("kylo.inferUrl")
  val kyloSystemUrl : Option[String] = playConfig.getString("kylo.systemUrl")
  val kyloUser : Option[String] = playConfig.getString("kylo.user")
  val kyloPassword : Option[String] = playConfig.getString("kylo.pwd")
  val kyloCsvSerde : Option[String] = playConfig.getString("kylo.csvSerde")
  val kyloJsonSerde : Option[String] = playConfig.getString("kylo.jsonSerde")

  val elasticsearchUrl: Option[String] = playConfig.getString("elasticsearch.url")
  val elasticsearchPort: Option[Int] = playConfig.getInt("elasticsearch.port")
  val elastisearchMaxResult: Option[Int] = playConfig.getInt("elasticsearch.max_result_window")

  val kafkaProxy: Option[String] = playConfig.getString("kafka-proxy.url")

  val datasetUrl: Option[String] = playConfig.getString("dataset-manager.url")
  val datasetUserOpendataEmail: Option[String] = playConfig.getString("dataset-manager.email")
  val datasetUserOpendataPwd: Option[String] = playConfig.getString("dataset-manager.pwd")

  val notificationInfo = playConfig.getConfigSeq("notificationType")

  val sysNotificationTypeName = playConfig.getString("systemNotificationTypeName")


}

object ConfigReader {
  private val config = new AppConfig(Configuration.load(Environment.simple()))

  require(config.elasticsearchUrl.nonEmpty, "A elasticsearch url must be specified")
  require(config.elastisearchMaxResult.nonEmpty, "A elasticsearch max result must be specified")
  require(config.catalogManagerHost.nonEmpty, "A catalog-manager url must be specified")
  require(config.catalogManagerNotificationPath.nonEmpty, "Path of API to insert message to kafka must be specified")
  require(config.openDataGroup.nonEmpty, "Open data group must be specified")
  require(config.collNotificationName.nonEmpty, "The name of the collectio notification must be specified")
  require(config.collSubscriptionName.nonEmpty, "The name of the collectio subscription must be specified")
  require(config.sysAdminName.nonEmpty, "The name of sys admin must be specified")
  require(config.userRoot.nonEmpty, "The mongo user root must be specified")
  require(config.databaseRoot.nonEmpty, "The mongo database root must be specified")
  require(config.notificationInfo.nonEmpty, "NotificationInfo must be specified")
  require(config.openDataUser.nonEmpty, "Open data user must be specified")


  def getOpenDataGroup = config.openDataGroup.get
  def getOpenDataUser = config.openDataUser.get

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
  def databaseRoot: String = config.databaseRoot.get
  def password :String = config.password.getOrElse("")
  def getRootPasswprd: String = config.rootPassword.getOrElse("")
  def userName :String = config.userName.getOrElse("")
  def userRoot: String = config.userRoot.get
  def getCollNotificationName: String = config.collNotificationName.get
  def getCollSubscriptionName: String = config.collSubscriptionName.get

  def getSysAdminName: String = config.sysAdminName.get

  def securityManHost :String = config.securityManHost.getOrElse("xxx")
  def getCatalogManagerHost: String = config.catalogManagerHost.get
  def getCatalogManagerNotificationPath: String = config.catalogManagerNotificationPath.get

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
  def getElastcsearchMaxResult = config.elastisearchMaxResult.get

  def getKafkaProxy = config.kafkaProxy.getOrElse("localhost:8085")

  def getDatasetUrl =  config.datasetUrl.getOrElse("XXXXX")
  def getDatasetUserOpendataEmail = config.datasetUserOpendataEmail.getOrElse("XXXXX")
  def getDatasetUserOpendataPwd = config.datasetUserOpendataPwd.getOrElse("XXXXXXX")

  def getNotificationInfo = config.notificationInfo.get.map{x => x.getString("name").get -> x.getInt("value").get}.toMap

  def getSysNotificationTypeName = config.sysNotificationTypeName.get
}
