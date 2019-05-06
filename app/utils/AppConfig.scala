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
  val collKafkaOffsets: Option[String] = playConfig.getString("mongo.collKafkaOffsets")

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

  val notificationInfo: Option[Seq[Configuration]] = playConfig.getConfigSeq("notificationType")

  val sysNotificationTypeName: Option[String] = playConfig.getString("systemNotificationTypeName")


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
  require(config.collKafkaOffsets.nonEmpty, "Collection of kafka offsets must be specified")


  def getOpenDataGroup: String = config.openDataGroup.get
  def getOpenDataUser: String = config.openDataUser.get

  def getDbHost: String = config.dbHost.getOrElse("localhost")
  def getDbPort: Int = config.dbPort.getOrElse(27017)
  def getCkanHost: String = config.ckanHost.getOrElse("localhost")
  def getCkanApiKey: String = config.ckanApiKey.getOrElse("dsadsadas")
  def getLocalUrl: String = config.localUrl.getOrElse("http://localhost:9000")

  def getMetabaseUrl: String = config.metabaseURL.getOrElse("http://localhost:13479")
  def getMetaUser: String = config.metauser.getOrElse("ale.ercolani@gmail.com")
  def getMetaPass: String = config.metapass.getOrElse("password")

  def getSupersetUrl: String = config.supersetURL.getOrElse("http://localhost:8088")
  def getSupersetOpenDataUrl: String = config.supersetOpenDataUrl.getOrElse("")
  def getSupersetUser: String = config.supersetUser.getOrElse("alessandro")
  def getSupersetPass: String = config.supersetPass.getOrElse("password")
  def getSupersetOpenDataUser: String = config.supersetOpenDataUser.getOrElse("")
  def getSupersetOpenDataPwd: String = config.supersetOpenDataPwd.getOrElse("")

  def getGrafanaUrl: String = config.grafanaURL.getOrElse("TO DO")

  def getTdMetabaseURL: String = config.tdMetabaseURL.getOrElse("https://dashboard.teamdigitale.governo.it")

  def database :String = config.database.getOrElse("monitor_mdb")
  def databaseRoot: String = config.databaseRoot.get
  def password :String = config.password.getOrElse("")
  def getRootPasswprd: String = config.rootPassword.getOrElse("")
  def userName :String = config.userName.getOrElse("")
  def userRoot: String = config.userRoot.get
  def getCollNotificationName: String = config.collNotificationName.get
  def getCollSubscriptionName: String = config.collSubscriptionName.get
  def getCollKafkaOffsets: String = config.collKafkaOffsets.get

  def getSysAdminName: String = config.sysAdminName.get

  def securityManHost :String = config.securityManHost.getOrElse("xxx")
  def getCatalogManagerHost: String = config.catalogManagerHost.get
  def getCatalogManagerNotificationPath: String = config.catalogManagerNotificationPath.get

  def cookieExpiration:Long = config.cookieExpiration.getOrElse(30L)// 30 min by default
  // TODO think about a defaul ingestion mechanism without kylo
  def kyloUrl: String = config.kyloUrl.getOrElse("No default")
  def kyloInferUrl: String = config.kyloInferUrl.getOrElse("No default")
  def kyloSystemUrl: String = config.kyloSystemUrl.getOrElse("No default")
  def kyloCsvSerde: String = config.kyloCsvSerde.getOrElse("No default")
  def kyloJsonSerde: String = config.kyloJsonSerde.getOrElse("No default")
  def kyloUser: String = config.kyloUser.getOrElse("dladmin")
  def kyloPwd: String = config.kyloPassword.getOrElse("XXXXXXXXX")

  def getElasticsearchUrl: String = config.elasticsearchUrl.get
  def getElasticsearchPort: Int = config.elasticsearchPort.getOrElse(9200)
  def getElastcsearchMaxResult: Int = config.elastisearchMaxResult.get

  def getKafkaProxy: String = config.kafkaProxy.getOrElse("localhost:8085")

  def getDatasetUrl: String =  config.datasetUrl.getOrElse("XXXXX")
  def getDatasetUserOpendataEmail: String = config.datasetUserOpendataEmail.getOrElse("XXXXX")
  def getDatasetUserOpendataPwd: String = config.datasetUserOpendataPwd.getOrElse("XXXXXXX")

  def getNotificationInfo: Map[String, Int] = config.notificationInfo.get.map{x => x.getString("name").get -> x.getInt("value").get}.toMap

  def getSysNotificationTypeName: String = config.sysNotificationTypeName.get
}
