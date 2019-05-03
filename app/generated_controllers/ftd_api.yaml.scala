
import play.api.mvc.{Action,Controller}

import play.api.data.validation.Constraint

import play.api.i18n.MessagesApi

import play.api.inject.{ApplicationLifecycle,ConfigurationProvider}

import de.zalando.play.controllers._

import PlayBodyParsing._

import PlayValidations._

import scala.util._

import javax.inject._

import java.io.File

import de.zalando.play.controllers.PlayBodyParsing._
import it.gov.daf.common.authentication.Authentication
import org.pac4j.play.store.PlaySessionStore
import services.ComponentRegistry
import services.dashboard.DashboardRegistry
import play.api.Configuration
import play.api.libs.ws.WSClient
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.Base64
import play.api.Environment
import scala.io.Source
import play.api.libs.json._
import services.settings.SettingsRegistry
import services.push_notification.PushNotificationRegistry
import utils.InferSchema._
import akka.stream.scaladsl.FileIO
import play.api.mvc.MultipartFormData.{DataPart,FilePart}
import play.api.libs.ws.WSAuthScheme
import utils.ConfigReader
import java.io.PrintWriter
import play.api.libs.Files.TemporaryFile
import scala.concurrent.Future
import utils.InferSchema
import java.nio.charset.CodingErrorAction
import scala.io.Codec
import it.gov.daf.common.sso.common.CredentialManager
import it.gov.daf.common.utils.RequestContext
import java.net.URLEncoder
import play.api.mvc.Headers
import services.datastory.DatastoryRegistry
import services.widgets.WidgetsRegistry
import services.elasticsearch.ElasticsearchRegistry

/**
 * This controller is re-generated after each change in the specification.
 * Please only place your hand-written code between appropriate comments in the body of the controller.
 */

package ftd_api.yaml {
    // ----- Start of unmanaged code area for package Ftd_apiYaml
                                    
    // ----- End of unmanaged code area for package Ftd_apiYaml
    class Ftd_apiYaml @Inject() (
        // ----- Start of unmanaged code area for injections Ftd_apiYaml
                               val configuration: Configuration,
                               val playSessionStore: PlaySessionStore,
                               val ws: WSClient,
        // ----- End of unmanaged code area for injections Ftd_apiYaml
        val messagesApi: MessagesApi,
        lifecycle: ApplicationLifecycle,
        config: ConfigurationProvider
    ) extends Ftd_apiYamlBase {
        // ----- Start of unmanaged code area for constructor Ftd_apiYaml


    Authentication(configuration, playSessionStore)
    val GENERIC_ERROR = Error(None, Some("An Error occurred"), None)

      private def readTokenFromRequest(requestHeader: Headers): Option[String] = {
        val authHeader = requestHeader.get("authorization").get.split(" ")
        val authType = authHeader(0)
        val authCredentials = authHeader(1)

        if( authType.equalsIgnoreCase("bearer")) Some(authCredentials)
        else None
      }

      private def getUserOrgs(username:String)={
        //val credentials = CredentialManager.readCredentialFromRequest(currentRequest)
        val internalUrl = s"${ConfigReader.securityManHost}/security-manager/v1/ipa/userbyname/$username"

        val inAuth=("authorization",currentRequest.headers.get("authorization").get)

        logger.debug(s"internalUrl: $internalUrl")
        ws.url(internalUrl)
          .withHeaders(inAuth)
          .get()
          .map{ resp =>
          logger.debug("security-manager userbyname response: "+resp)
          (resp.json \ "organizations").as[Seq[String]]
        }
      }
      private def getUserOrgsWorkgroups(username: String) = {
        val internalUrl = s"${ConfigReader.securityManHost}/security-manager/v1/ipa/userbyname/$username"

        val inAuth=("authorization",currentRequest.headers.get("authorization").get)

        logger.debug(s"internalUrl: $internalUrl")
        ws.url(internalUrl)
          .withHeaders(inAuth)
          .get()
          .map{ resp =>
            logger.debug("security-manager userbyname response: "+resp)
            (resp.json \ "organizations").as[Seq[String]] ++ (resp.json \ "workgroups").as[Seq[String]]
          }
      }
        // ----- End of unmanaged code area for constructor Ftd_apiYaml
        val openIframesByTableName = openIframesByTableNameAction { (tableName: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.openIframesByTableName
            RequestContext.execInContext[Future[OpenIframesByTableNameType[T] forSome { type T }]]("openIframesByTableName") { () =>
            val openDataUser = ConfigReader.getSupersetOpenDataUser
            val iframes = DashboardRegistry.dashboardService.iframes(openDataUser, ws)
            val iframesByName = iframes.map(_.filter(_.table.getOrElse("").endsWith(tableName)))
            OpenIframesByTableName200(iframesByName)
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.openIframesByTableName
        }
        val deleteAllSubscription = deleteAllSubscriptionAction { (user: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.deleteAllSubscription
            RequestContext.execInContext[Future[DeleteAllSubscriptionType[T] forSome { type T }]]("deleteAllSubscription") { () =>
            val username = CredentialManager.readCredentialFromRequest(currentRequest).username
            if (username.equals(user)) DeleteAllSubscription200(PushNotificationRegistry.pushNotificationService.deleteAllSubscription(user))
            else DeleteAllSubscription401(Error(None, Some(s"Unauthorized to delete subscriptions for user $user"), None))
          }
//            NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.deleteAllSubscription
        }
        val deleteDatastory = deleteDatastoryAction { (id: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.deleteDatastory
            RequestContext.execInContext[Future[DeleteDatastoryType[T] forSome { type T }]]("deleteDatastory") { () =>

            def parseError(error: Error) = {
              error.code match {
                case Some(401) => DeleteDatastory401(error)
                case Some(404) => DeleteDatastory404(error)
                case _         => DeleteDatastory500(error)
              }
            }
            val user = CredentialManager.readCredentialFromRequest(currentRequest).username
            DatastoryRegistry.datastoryService.deleteDatastory(id, user) flatMap {
              case Right(success) => DeleteDatastory200(success)
              case Left(error)    => parseError(error)
            }
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.deleteDatastory
        }
        val catalogDistributionLicense = catalogDistributionLicenseAction { input: (String, String) =>
            val (catalogName, apikey) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.catalogDistributionLicense
            RequestContext.execInContext[Future[CatalogDistributionLicenseType[T] forSome { type T }]]("catalogDistributionLicense") { () =>
            val distributions: Seq[Distribution] = ComponentRegistry.monitorService.datasetCatalogLicenses(catalogName)
            CatalogDistributionLicense200(distributions)
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.catalogDistributionLicense
        }
        val wsKyloInferschema = wsKyloInferschemaAction { input: (String, String, Credentials) =>
            val (url, file_type, credentials) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.wsKyloInferschema
            RequestContext.execInContext[Future[WsKyloInferschemaType[T] forSome { type T }]]("wsKyloInferschema") { () =>
            val inferUrl = ConfigReader.kyloInferUrl

            val serde = file_type match {
              case "csv" => ConfigReader.kyloCsvSerde
              case "json" => ConfigReader.kyloJsonSerde
            }
            val tempFile: Future[File] = ws.url(url).get().map { resp =>

              val temp = file_type match {
                case "json" => {

                  val singleObj: JsObject = resp.json match {
                    case arr: JsArray => arr.as[List[JsObject]].head
                    case obj: JsObject => obj
                  }

                  val stringified = Json.stringify(singleObj).replaceAll("\n", "").replace("\r", "")

                  println("*****==============******")
                  println(stringified)

                  val tempFile = TemporaryFile(prefix = "uploaded").file
                  val pw = new PrintWriter(tempFile)
                  pw.write(stringified)
                  pw.close
                  tempFile
                }
                case "csv" => {
                  val first10lines = resp.body.split("\n").slice(0, 10)
                  val tempFile = TemporaryFile(prefix = "uploaded").file
                  val pw = new PrintWriter(tempFile)
                  for (line <- first10lines) {
                    pw.write(line + "\n")
                    pw.close
                  }
                  tempFile
                }
              }
              temp
            }

            def inferKylo(ff: File): Future[JsValue] = {
              logger.info(ff.getName)
              logger.info(inferUrl)
              //  ws.url("http://localhost:9000/dati-gov/v1/infer/kylo/json")
              //      .withAuth("test", "test", WSAuthScheme.BASIC)
              ws.url(inferUrl)
                .withAuth(ConfigReader.kyloUser, ConfigReader.kyloPwd, WSAuthScheme.BASIC)
                .post(akka.stream.scaladsl.Source(FilePart("file", ff.getName, Option("text/csv"),
                  FileIO.fromFile(ff)) :: DataPart("parser", serde) :: List()))
                .map { resp =>
                  ff.delete()
                  resp.json
                }
            }

            val response = for {
              ff <- tempFile
              resp <- inferKylo(ff)
            } yield resp

            response.flatMap(r => {
              logger.debug(Json.stringify(r))
              WsKyloInferschema200(Json.stringify(r))
            })
          }
       //   NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.wsKyloInferschema
        }
        val settingsByName = settingsByNameAction { (domain: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.settingsByName
            RequestContext.execInContext[Future[SettingsByNameType[T] forSome { type T }]]("settingsByName") { () =>
            val response = SettingsRegistry.settingsService.settingsByName(domain)
            if (response.isRight)
              SettingsByName200(response.right.get)
            else
              SettingsByName400(response.left.get)
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.settingsByName
        }
        val savestories = savestoriesAction { input: (UserStory, LayoutDataStoryIsDraggable) =>
            val (story, shared) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.savestories
            RequestContext.execInContext[Future[SavestoriesType[T] forSome { type T }]]("savestories") { () =>
            val user = CredentialManager.readCredentialFromRequest(currentRequest).username
              println(user)
            val token = readTokenFromRequest(currentRequest.headers)
            token match {
              case Some(t) => Savestories200(DashboardRegistry.dashboardService.saveStory(story, user, shared, t, ws))
              case None => Savestories401("No token found")
            }
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.savestories
        }
        val createSnapshot = createSnapshotAction { input: (File, String, String) =>
            val (upfile, snapshot_id, apikey) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.createSnapshot
            RequestContext.execInContext[Future[CreateSnapshotType[T] forSome { type T }]]("createSnapshot") { () =>
            upfile.renameTo(new File("public/img", snapshot_id + ".png"));
            CreateSnapshot200(Success(Some("File created"), Some("File created")))
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.createSnapshot
        }
        val searchFullText = searchFullTextAction { input: (Filters, FiltersLimit) =>
            val (filters, limit) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.searchFullText
            RequestContext.execInContext[Future[SearchFullTextType[T] forSome { type T }]]("searchFullText") { () =>
            val credentials = CredentialManager.readCredentialFromRequest(currentRequest)

            val result = for {
              orgsWorks <- getUserOrgsWorkgroups(credentials.username)
              out <- ElasticsearchRegistry.elasticsearchService.searchText(filters, credentials.username, orgsWorks.toList, limit)
            } yield out
            result flatMap (SearchFullText200(_))
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.searchFullText
        }
        val getDatastoryById = getDatastoryByIdAction { (id: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.getDatastoryById
            RequestContext.execInContext[Future[GetDatastoryByIdType[T] forSome { type T }]]("getDatastoryById") { () =>
            def parseError(error: Error) = {
              error.code match {
                case Some(404) => GetDatastoryById404(error)
                case _         => GetDatastoryById500(error)
              }
            }
            val credential = CredentialManager.readCredentialFromRequest(currentRequest)
            val response = DatastoryRegistry.datastoryService.getDatastoryById(id, credential.username, credential.groups.toList)
            response.flatMap{
              case Right(datastory) => GetDatastoryById200(datastory)
              case Left(error)      => parseError(error)
            }
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.getDatastoryById
        }
        val stories = storiesAction { input: (TTLErrorType, TTLErrorType, PublicDashboardsGetLimit) =>
            val (status, page, limit) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.stories
            RequestContext.execInContext[Future[StoriesType[T] forSome { type T }]]("stories") { () =>
            val credentials = CredentialManager.readCredentialFromRequest(currentRequest)

            val result = for {
              orgs <- getUserOrgs(credentials.username)
              out <- Future.successful {
                DashboardRegistry.dashboardService.stories(credentials.username, orgs.toList, status, page, limit)
              }
            } yield out

            result flatMap (Stories200(_))
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.stories
        }
        val getPublicDatastoryById = getPublicDatastoryByIdAction { (id: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.getPublicDatastoryById
            RequestContext.execInContext[Future[GetPublicDatastoryByIdType[T] forSome { type T }]]("getPublicDatastoryById") { () =>
            def parseError(error: Error) = {
              error.code match {
                case Some(404) => GetPublicDatastoryById404(error)
                case _         => GetPublicDatastoryById500(error)
              }
            }
            val response = DatastoryRegistry.datastoryService.getPublicDatastoryById(id)
            response.flatMap{
              case Right(datastory) => GetPublicDatastoryById200(datastory)
              case Left(error)      => parseError(error)
            }
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.getPublicDatastoryById
        }
        val dashboardTables = dashboardTablesAction { (apikey: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.dashboardTables
            RequestContext.execInContext[Future[DashboardTablesType[T] forSome { type T }]]("dashboardTables") { () =>
            val tables = DashboardRegistry.dashboardService.tables()
            DashboardTables200(tables)
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.dashboardTables
        }
        val supersetTablesByOrgs = supersetTablesByOrgsAction { input: (String, DashboardSupersetTablesTableNamePostOrgs) =>
            val (tableName, orgs) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.supersetTablesByOrgs
            RequestContext.execInContext[Future[SupersetTablesByOrgsType[T] forSome { type T }]]("supersetTablesByOrgs") { () =>
            val user = CredentialManager.readCredentialFromRequest(currentRequest).username
            val response: Future[Either[Error, Seq[SupersetTable]]] = DashboardRegistry.dashboardService.getSupersetTableByTableNameIdAndOrgs(user, tableName, orgs, ws)
            response.flatMap{
              case Left(l) => SupersetTablesByOrgs404(l)
              case Right(r) => SupersetTablesByOrgs200(r)
            }

          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.supersetTablesByOrgs
        }
        val dashboardIframes = dashboardIframesAction {  _ =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.dashboardIframes
            RequestContext.execInContext[Future[DashboardIframesType[T] forSome { type T }]]("dashboardIframes") { () =>
            val credentials = CredentialManager.readCredentialFromRequest(currentRequest)
            val iframes = DashboardRegistry.dashboardService.iframes(credentials.username, ws)
            DashboardIframes200(iframes)
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.dashboardIframes
        }
        val iframesByTableName = iframesByTableNameAction { input: (String, DistributionLabel) =>
            val (tableName, user) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.iframesByTableName
            RequestContext.execInContext[Future[IframesByTableNameType[T] forSome { type T }]]("iframesByTableName") { () =>
            val credentials = CredentialManager.readCredentialFromRequest(currentRequest)
            val iframes = DashboardRegistry.dashboardService.iframes(credentials.username, ws)
            val iframesByName = iframes.map(_.filter(_.table.getOrElse("").endsWith(tableName)))
            IframesByTableName200(iframesByName)
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.iframesByTableName
        }
        val searchLastPublic = searchLastPublicAction { (org: DistributionLabel) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.searchLastPublic
            RequestContext.execInContext[Future[SearchLastPublicType[T] forSome { type T }]]("searchLastPublic") { () =>
            SearchLastPublic200(ElasticsearchRegistry.elasticsearchService.searchLastPublic(org))
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.searchLastPublic
        }
        val allDistributionLiceses = allDistributionLicesesAction { (apikey: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.allDistributionLiceses
            RequestContext.execInContext[Future[AllDistributionLicesesType[T] forSome { type T }]]("allDistributionLiceses") { () =>
            val distributions: Seq[Distribution] = ComponentRegistry.monitorService.allDistributionLiceses()
            AllDistributionLiceses200(distributions)
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.allDistributionLiceses
        }
        val saveDatastory = saveDatastoryAction { (datastory: Datastory) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.saveDatastory
            RequestContext.execInContext[Future[SaveDatastoryType[T] forSome { type T }]]("saveDatastory") { () =>
              def parseError(error: Error) = {
                error.code match {
                  case Some(401) => SaveDatastory401(error)
                  case Some(403) => SaveDatastory403(error)
                  case _         => SaveDatastory500(error)
                }
              }

              val credential = CredentialManager.readCredentialFromRequest(currentRequest)
              val token = readTokenFromRequest(currentRequest.headers)
            if(datastory.user.equals(credential.username) && credential.groups.contains(datastory.org) && token.isDefined)
              DatastoryRegistry.datastoryService.saveDatastory(credential.username, datastory, token.get, ws) flatMap{
                case Right(success) => SaveDatastory200(success)
                case Left(error)    => parseError(error)
              }
            else {
              logger.debug(s"${credential.username} unauthorized to insert datastory $datastory")
              SaveDatastory401(Future.successful(Error(Some(401), Some("Unauthorized to insert datastory"), None)))
            }
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.saveDatastory
        }
        val catalogDistrubutionFormat = catalogDistrubutionFormatAction { input: (String, String) =>
            val (catalogName, apikey) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.catalogDistrubutionFormat
            RequestContext.execInContext[Future[CatalogDistrubutionFormatType[T] forSome { type T }]]("catalogDistrubutionFormat") { () =>
            val distributions: Seq[Distribution] = ComponentRegistry.monitorService.datasetCatalogFormat(catalogName)
            CatalogDistrubutionFormat200(distributions)
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.catalogDistrubutionFormat
        }
        val getAllNotifications = getAllNotificationsAction { input: (String, PublicDashboardsGetLimit) =>
            val (user, limit) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.getAllNotifications
            RequestContext.execInContext[Future[GetAllNotificationsType[T] forSome { type T }]]("getAllNotifications") { () =>
            val credentials = CredentialManager.readCredentialFromRequest(currentRequest)
            if (credentials.username.equals(user) || CredentialManager.isDafSysAdmin(currentRequest)
              || CredentialManager.isOrgsAdmin(currentRequest, credentials.groups))
              GetAllNotifications200(PushNotificationRegistry.pushNotificationService.getAllNotifications(user, limit))
            else
              GetAllNotifications401(Error(Some(401), Some(s"Unauthorized to read notifications for ${user}"), None))
          }
//          NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.getAllNotifications
        }
        val deleteAllNotifications = deleteAllNotificationsAction { input: (String, Notification) =>
            val (user, notifation) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.deleteAllNotifications
            NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.deleteAllNotifications
        }
        val getAllPublicDatastory = getAllPublicDatastoryAction { (limit: TTLErrorType) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.getAllPublicDatastory
            RequestContext.execInContext[Future[GetAllPublicDatastoryType[T] forSome { type T }]]("getAllPublicDatastory") { () =>
            def parseError(error: Error) = {
              error.code match {
                case Some(404) => GetAllPublicDatastory404(error)
                case _         => GetAllPublicDatastory500(error)
              }
            }
            DatastoryRegistry.datastoryService.getAllPublicDatastory(limit) flatMap{
              case Right(seqDatastory) => GetAllPublicDatastory200(seqDatastory)
              case Left(error)         => parseError(error)

            }
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.getAllPublicDatastory
        }
        val allDistributionGroups = allDistributionGroupsAction { (apikey: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.allDistributionGroups
            RequestContext.execInContext[Future[AllDistributionGroupsType[T] forSome { type T }]]("allDistributionGroups") { () =>
            val distributions: Seq[Distribution] = ComponentRegistry.monitorService.allDistributionGroup()
            AllDistributionGroups200(distributions)
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.allDistributionGroups
        }
        val deleteTtl = deleteTtlAction { (deleteTTLNotificationsInfo: DeleteTTLNotificationInfo) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.deleteTtl
            RequestContext.execInContext[Future[DeleteTtlType[T] forSome { type T }]]("deleteTtl") { () =>
            if(CredentialManager.isDafSysAdmin(currentRequest)){
              PushNotificationRegistry.pushNotificationService.deleteTtl(deleteTTLNotificationsInfo) flatMap {
                case Right(success) => DeleteTtl200(success)
                case Left(error)    => DeleteTtl500(error)
              }
            } else {
              logger.debug("only sys admin can delete a ttl")
              DeleteTtl500(Future.successful(Error(Some(401), Some("Only sys admin can delete a ttl"), None)))
            }
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.deleteTtl
        }
        val allDatasets = allDatasetsAction { (apikey: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.allDatasets
            RequestContext.execInContext[Future[AllDatasetsType[T] forSome { type T }]]("allDatasets") { () =>
            val distributions: Seq[Distribution] = ComponentRegistry.monitorService.datasetsCount()
            AllDatasets200(distributions)
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.allDatasets
        }
        val storiesbyid = storiesbyidAction { (story_id: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.storiesbyid
            RequestContext.execInContext[Future[StoriesbyidType[T] forSome { type T }]]("storiesbyid") { () =>
            val credentials = CredentialManager.readCredentialFromRequest(currentRequest)

            val result = for {
              orgs <- getUserOrgs(credentials.username)
              out <- Future.successful {
                DashboardRegistry.dashboardService.storyById(
                  credentials.username, orgs.toList, story_id)
              }
            } yield out
            result flatMap {
              case Some(s) => Storiesbyid200(s)
              case _ => Storiesbyid404(Error(Some(404), Some("User-Story non trovata"), None))
            }
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.storiesbyid
        }
        val getSystemNotificationByOffset = getSystemNotificationByOffsetAction { (offset: Int) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.getSystemNotificationByOffset
            RequestContext.execInContext[Future[GetSystemNotificationByOffsetType[T] forSome { type T }]]("getSystemNotificationByOffset") { () =>
            def parseError(error: Error) = {
              error.code match {
                case Some(404) => GetSystemNotificationByOffset404(error)
                case _         => GetSystemNotificationByOffset500(error)
              }
            }
            if(CredentialManager.isDafSysAdmin(currentRequest)){
              PushNotificationRegistry.pushNotificationService.getSystemNotificationByOffset(offset) flatMap{
                case Right(success) => GetSystemNotificationByOffset200(success)
                case Left(error)    => parseError(error)
              }
            } else {
              logger.debug("is not a sys admin")
              GetSystemNotificationByOffset401(Future.successful(Error(Some(401), Some("Only SysAdmin can add system notifications"), None)))
            }
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.getSystemNotificationByOffset
        }
        val deleteSystemNotificationByOffset = deleteSystemNotificationByOffsetAction { (offset: Int) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.deleteSystemNotificationByOffset
            RequestContext.execInContext[Future[DeleteSystemNotificationByOffsetType[T] forSome { type T }]]("deleteSystemNotificationByOffset") { () =>
            def parseError(error: Error) = {
              error.code match {
                case Some(404) => DeleteSystemNotificationByOffset404(error)
                case _         => DeleteSystemNotificationByOffset500(error)
              }
            }
            if(CredentialManager.isDafSysAdmin(currentRequest)) {
              PushNotificationRegistry.pushNotificationService.deleteSystemNotificationByOffset(offset) flatMap {
                case Right(success) => DeleteSystemNotificationByOffset200(success)
                case Left(error)    => parseError(error)
              }
            } else {
              logger.debug("is not a sys admin")
              DeleteSystemNotificationByOffset401(Future.successful(Error(Some(401), Some("Only SysAdmin can add system notifications"), None)))
            }
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.deleteSystemNotificationByOffset
        }
        val systemNotificationInsert = systemNotificationInsertAction { (kafkaMessageInfo: SysNotificationInfo) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.systemNotificationInsert
            RequestContext.execInContext[Future[SystemNotificationInsertType[T] forSome { type T }]]("systemNotificationInsert") { () =>
            def parseError(error: Error) = {
              error.code match {
                case Some(401) => SystemNotificationInsert401(error)
                case Some(403) => SystemNotificationInsert403(error)
                case _         => SystemNotificationInsert500(error)
              }
            }

            val optionToken = readTokenFromRequest(currentRequest.headers)
            optionToken match {
              case None        => logger.debug("no token found"); SystemNotificationInsert401(Future.successful(Error(Some(401), Some("Basic authentication it's not allowed"), None)))
              case Some(token) =>
                if(CredentialManager.isDafSysAdmin(currentRequest)) {
                  PushNotificationRegistry.pushNotificationService.systemNotificationInsert(kafkaMessageInfo, token, ws) flatMap {
                    case Right(success) => SystemNotificationInsert200(success)
                    case Left(error)    => parseError(error)
                  }
                } else {
                  logger.debug("is not a sys admin")
                  SystemNotificationInsert401(Future.successful(Error(Some(401), Some("Only SysAdmin can add system notifications"), None)))
                }
            }

          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.systemNotificationInsert
        }
        val kyloFeedByName = kyloFeedByNameAction { (feed_name: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.kyloFeedByName
            RequestContext.execInContext[Future[KyloFeedByNameType[T] forSome { type T }]]("kyloFeedByName") { () =>
              val kyloUrl: String = ConfigReader.kyloUrl + "/api/v1/feedmgr/feeds/by-name/" + feed_name
              logger.debug("HERE WE ARE")
              logger.debug(kyloUrl)
              val feed: Future[KyloFeed] = ws.url(kyloUrl)
                .withAuth(ConfigReader.kyloUser, ConfigReader.kyloPwd, WSAuthScheme.BASIC)
                .get().map { resp =>
                logger.debug("HERE WE ARE")
                logger.debug(resp.body)
                val name = (resp.json \ "feedName").as[String]
                val active = (resp.json \ "active").as[Boolean]
                val state = (resp.json \ "state").as[String]
                val updatedate = (resp.json \ "updateDate").as[Long]
                KyloFeed(name, state, updatedate, active, None, None, None)
              }

                def feedWithJobs(kyloFeed: KyloFeed) = {
                  val queryParam = URLEncoder.encode("=~%", "UTF-8")
                  val kyloJobUrl = ConfigReader.kyloUrl + s"/api/v1/jobs?filter=job${queryParam}${kyloFeed.feed_name}&limit=5&sort=-startTime&start=0"
                  logger.info(kyloJobUrl)
                  ws.url(kyloJobUrl)
                    .withAuth(ConfigReader.kyloUser, ConfigReader.kyloPwd, WSAuthScheme.BASIC)
                    .get().map { resp =>
                    val data = (resp.json \ "data").as[Seq[JsValue]]
                    logger.info(data.toString())
                    val feed = data match {
                      case Seq() => kyloFeed.copy(has_job = Some(false))
                      case jobs => {
                        val job = jobs.head
                        val status = (job \ "status").as[String]
                        val created = (job \ "startTime").as[Long]
                        kyloFeed.copy(has_job = Some(true), job_status = Some(status), job_created = Some(created))
                      }
                    }
                    feed
                  }
                }

                val feedWithJob = for {
                  k <- feed
                  withJobStatus <- feedWithJobs(k)
                } yield withJobStatus

                KyloFeedByName200(feedWithJob)
              }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.kyloFeedByName
        }
        val searchFullTextPublic = searchFullTextPublicAction { input: (Filters, FiltersLimit) =>
            val (filters, limit) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.searchFullTextPublic
            RequestContext.execInContext[Future[SearchFullTextPublicType[T] forSome { type T }]]("searchFullTextPublic") { () =>
            SearchFullTextPublic200(ElasticsearchRegistry.elasticsearchService.searchTextPublic(filters, limit))
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.searchFullTextPublic
        }
        val snapshotbyid = snapshotbyidAction { input: (String, String) =>
            val (iframe_id, sizexsize) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.snapshotbyid
            RequestContext.execInContext[Future[SnapshotbyidType[T] forSome { type T }]]("snapshotbyid") { () =>
            val conf = Configuration.load(Environment.simple())
            val URL: String = conf.getString("daf-cacher.url").get

            val url = URL + iframe_id + "/" + sizexsize

            val response = ws.url(url).get().map(x => {
              x.status match {
                case 500 => "noimage"
                case _ => Base64.getEncoder.encodeToString(x.bodyAsBytes.toArray)
              }
            })

            Snapshotbyid200(response)
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.snapshotbyid
        }
        val createSubscription = createSubscriptionAction { (subscription: Subscription) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.createSubscription
            RequestContext.execInContext[Future[CreateSubscriptionType[T] forSome { type T }]]("createSubscription") { () =>
            val user = CredentialManager.readCredentialFromRequest(currentRequest).username
            val resp = PushNotificationRegistry.pushNotificationService.saveSubscription(user, subscription)
            resp.flatMap {
              case Right(r) => CreateSubscription200(r)
              case Left(l) => CreateSubscription500(l)
            }
          }
//          NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.createSubscription
        }
        val deleteDataApplication = deleteDataApplicationAction { (data_app: DataApp) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.deleteDataApplication
            NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.deleteDataApplication
        }
        val dashboards = dashboardsAction { input: (TTLErrorType, TTLErrorType, PublicDashboardsGetLimit) =>
            val (status, page, limit) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.dashboards
            RequestContext.execInContext[Future[DashboardsType[T] forSome { type T }]]("dashboards") { () =>
            val credentials = CredentialManager.readCredentialFromRequest(currentRequest)

            val result = for {
              orgs <- getUserOrgs(credentials.username)
              out <- Future.successful {
                DashboardRegistry.dashboardService.dashboards(
                  credentials.username, orgs.toList, status)
              }
            } yield out
            result flatMap (Dashboards200(_))
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.dashboards
        }
        val getAllWidgets = getAllWidgetsAction { (org: DistributionLabel) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.getAllWidgets
            RequestContext.execInContext[Future[GetAllWidgetsType[T] forSome { type T }]]("getAllWidgets") { () =>
            def parseError(error: Error) = {
              error.code match {
                case Some(401) => GetAllWidgets401(error)
                case Some(404) => GetAllWidgets404(error)
                case _         => GetAllWidgets500(error)
              }
            }
            val credential = CredentialManager.readCredentialFromRequest(currentRequest)
            val result = WidgetsRegistry.widgetsService.getAllWidgets(credential.username, credential.groups.toList, org, ws)
            result.flatMap {
              case Right(widgets) => GetAllWidgets200(widgets)
              case Left(error)    => parseError(error)
            }
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.getAllWidgets
        }
        val searchLast = searchLastAction {  _ =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.searchLast
            RequestContext.execInContext[Future[SearchLastType[T] forSome { type T }]]("searchLast") { () =>
            val credentials = CredentialManager.readCredentialFromRequest(currentRequest)
            val result = for {
              orgsWorks <- getUserOrgsWorkgroups(credentials.username)
              out <- Future.successful {
                ElasticsearchRegistry.elasticsearchService.searchLast(credentials.username, orgsWorks.toList)
              }
            } yield out
            result flatMap (SearchLast200(_))
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.searchLast
        }
        val inferschema = inferschemaAction { input: (File, String) =>
            val (upfile, fileType) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.inferschema
            RequestContext.execInContext[Future[InferschemaType[T] forSome { type T }]]("inferschema") { () =>
            // DEPRECATED NOT USED ANYMORE WE ARE USING KYLO
            if (fileType.equals("csv")) {

        val (content, rows) = Source
          .fromFile(upfile)
          .getLines.slice(0, 100).duplicate

        val rows20 = rows.slice(0, 20)

        val header = content.next

        val separator = inferSeparator(header, SEPARATORS)

        val headerRows = rows20.next.split(separator)

        val dataColumn = rows20
          .toList
          .flatMap(_.split(separator).zipWithIndex)
          .groupBy(_._2)
          .map(_._2)
          .toList
          .sortBy(_ (0)._2)

        val data = headerRows
          .zip(dataColumn)
          .toMap
          .mapValues(x => (x.map(_._1)))

        val headers: Array[String] = header.split(separator)

        val result = content.map(x => {
          val row = x.split(separator)
          val contents: Array[Seq[String]] = x.split(separator).map(_.castToString.flatten)
          headers.zip(contents).toMap
        })

        val infered = result
          .toList
          .flatten
          .distinct
          .groupBy(_._1)
          .mapValues((x => x.flatMap(_._2))
        )


        val total = for {
          (k, v1) <- infered
          v2 <- data.get(k)
        } yield (InferredType(Some(k), Some(v2), Some(v1)))

        Inferschema200(Inferred(Some(separator), Some("csv"), Some(total.toList)))
      } else {
              object JsFlattener {

                def apply(js: JsValue): JsValue = flatten(js).foldLeft(JsObject(Nil))(_ ++ _.as[JsObject])

                def flatten(js: JsValue, prefix: String = ""): Seq[JsValue] = {
                  js.as[JsObject].fieldSet.toSeq.flatMap { case (key, values) =>
                    values match {
                      case JsBoolean(x) => Seq(Json.obj(concat(prefix, key) -> x))
                      case JsNumber(x) => Seq(Json.obj(concat(prefix, key) -> x))
                      case JsString(x) => Seq(Json.obj(concat(prefix, key) -> x))
                      case JsArray(seq) => seq.zipWithIndex.flatMap { case (x, i) => flatten(x, concat(prefix, key + s"[$i]")) }
                      case x: JsObject => flatten(x, concat(prefix, key))
                      case _ => Seq(Json.obj(concat(prefix, key) -> JsNull))
                    }
                  }
                }

                def concat(prefix: String, key: String): String = if (prefix.nonEmpty) s"$prefix.$key" else key

              }

              val jsonString = Source.fromFile(upfile).getLines().mkString
              println(jsonString)
              val jsonObj = Json.parse(jsonString)
              val correct = jsonObj match {
                case x: JsArray => JsFlattener(x.value.head)
                case y: JsObject => JsFlattener(y)
              }
              println(correct.toString())
              Inferschema200(Inferred(None, None, None))
            }
      }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.inferschema
        }
        val supersetTableFromDataset = supersetTableFromDatasetAction { (dataset_name: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.supersetTableFromDataset
            RequestContext.execInContext[Future[SupersetTableFromDatasetType[T] forSome { type T }]]("supersetTableFromDataset") { () =>
            val openDataUser = ConfigReader.getSupersetOpenDataUser
            val supersetOpenUrl = ConfigReader.getSupersetOpenDataUrl
            val appNameSuperset = "superset"
            val appNameSupersetOpen = "superset_open"
            val conf = Configuration.load(Environment.simple())
            val URL = conf.getString("app.local.url").get
            val supersetUrl = conf.getString("superset.url").get
            val username = CredentialManager.readCredentialFromRequest(currentRequest).username

            val tables: Future[Seq[SupersetUrl]] = ws.url(URL + s"/superset/table/$username/$dataset_name")
              .withHeaders("Content-Type" -> "application/json",
                "Accept" -> "application/json"
              ).get().map { resp =>
              val tables = resp.json.as[Seq[JsValue]]
              val supersetTables: Seq[SupersetUrl] = tables.map(x => {
                val id = (x \ "id").as[Int]
                val stringId = id.toString
                val tableName = (x \ "text").as[String]
                val url = s"$supersetUrl/superset/explore/table/$stringId/"
                SupersetUrl(id, tableName, url, appNameSuperset)
              })
              supersetTables
            }

            val tablesOpen: Future[Seq[SupersetUrl]] = ws.url(URL + s"/superset/table/$openDataUser/$dataset_name")
              .withHeaders("Content-Type" -> "application/json",
                "Accept" -> "application/json"
              ).get().map { resp =>
              val tables = resp.json.as[Seq[JsValue]]
              val supersetOpenTables: Seq[SupersetUrl] = tables.map(x => {
                val id = (x \ "id").as[Int]
                val stringId = id.toString
                val tableName = (x \ "text").as[String]
                val url = s"$supersetOpenUrl/superset/explore/table/$stringId/"
                SupersetUrl(id, tableName, url, appNameSupersetOpen)
              })
              supersetOpenTables
            }

            val res = for {
              supersetOpen <- tablesOpen
              superset <- tables
            } yield superset.toList ::: supersetOpen.toList

            SupersetTableFromDataset200(res)
          }
        //  NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.supersetTableFromDataset
        }
        val updateTtl = updateTtlAction { (ttl: NotificationsUpdateTtlPutTtl) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.updateTtl
            RequestContext.execInContext[Future[UpdateTtlType[T] forSome { type T }]]("updateTtl") { () =>
              if(!CredentialManager.isDafSysAdmin(currentRequest)) {
                logger.debug("only daf sys admin can update ttl")
                UpdateTtl401(Error(Some(401), Some("only daf sys admin can update ttl"), None))
              }
              else if(ttl.groupBy(x => x.name).map{_._2.head}.size != ttl.size) {
                logger.debug("no duplicate keys are allowed")
                UpdateTtl400(Error(Some(400), Some("no duplicate keys are allowed"), None))
              }
              else if(ttl.isEmpty){
                logger.debug("no ttl found in request")
                UpdateTtl400(Error(Some(400), Some("no ttl found"), None))
              }else {
                PushNotificationRegistry.pushNotificationService.updateTtl(ttl.distinct).flatMap{
                  case Right(r) => UpdateTtl200(r)
                  case Left(l)  => UpdateTtl500(l)
                }
              }

          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.updateTtl
        }
        val dashboardsbyid = dashboardsbyidAction { (dashboard_id: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.dashboardsbyid
            RequestContext.execInContext[Future[DashboardsbyidType[T] forSome { type T }]]("dashboardsbyid") { () =>
            val credentials = CredentialManager.readCredentialFromRequest(currentRequest)

            val result = for {
              orgs <- getUserOrgs(credentials.username)
              out <- Future.successful {
                DashboardRegistry.dashboardService.dashboardById(
                  credentials.username, orgs.toList, dashboard_id)
              }
            } yield out
            result flatMap {
              case Some(d) => Dashboardsbyid200(d)
              case _ => Dashboardsbyid404(Error(Some(404), Some("Dashboard non trovata"), None))
            }
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.dashboardsbyid
        }
        val checkNewNotifications = checkNewNotificationsAction { (user: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.checkNewNotifications
            RequestContext.execInContext[Future[CheckNewNotificationsType[T] forSome { type T }]]("checkNewNotifications") { () =>
            val credential = CredentialManager.readCredentialFromRequest(currentRequest)
            if (credential.username.equals(user) || CredentialManager.isDafSysAdmin(currentRequest)
              || CredentialManager.isOrgsAdmin(currentRequest, credential.groups))
              CheckNewNotifications200(PushNotificationRegistry.pushNotificationService.checkNewNotifications(user))
            else
              CheckNewNotifications401(Error(Some(401), Some(s"Unauthorized to read notifications for ${user}"), None))
          }
//            NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.checkNewNotifications
        }
        val allDistributionFormats = allDistributionFormatsAction { (apikey: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.allDistributionFormats
            RequestContext.execInContext[Future[AllDistributionFormatsType[T] forSome { type T }]]("allDistributionFormats") { () =>
            val distributions: Seq[Distribution] = ComponentRegistry.monitorService.allDistributionFormat()
            AllDistributionFormats200(distributions)
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.allDistributionFormats
        }
        val getAllDatastory = getAllDatastoryAction { input: (TTLErrorType, TTLErrorType) =>
            val (limit, status) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.getAllDatastory
            RequestContext.execInContext[Future[GetAllDatastoryType[T] forSome { type T }]]("getAllDatastory") { () =>
            def parseError(error: Error) = {
              error.code match {
                case Some(404) => GetAllDatastory404(error)
                case _         => GetAllDatastory500(error)
              }
            }
            val credential = CredentialManager.readCredentialFromRequest(currentRequest)
            DatastoryRegistry.datastoryService.getAllDatastory(credential.username, credential.groups.toList, limit, status) flatMap {
              case Right(success) => GetAllDatastory200(success)
              case Left(error)    => parseError(error)
            }
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.getAllDatastory
        }
        val dashboardIframesbyorg = dashboardIframesbyorgAction { (orgName: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.dashboardIframesbyorg
            RequestContext.execInContext[Future[DashboardIframesbyorgType[T] forSome { type T }]]("dashboardIframesbyorg") { () =>
              val credentials = CredentialManager.readCredentialFromRequest(currentRequest)
              val iframes = DashboardRegistry.dashboardService.iframesByOrg(credentials.username, orgName, ws)
              DashboardIframesbyorg200(iframes)
            }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.dashboardIframesbyorg
        }
        val updateNotifications = updateNotificationsAction { (notifications: NotificationsUpdatePostNotifications) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.updateNotifications
            RequestContext.execInContext[Future[UpdateNotificationsType[T] forSome { type T }]]("updateNotifications") { () =>
          val result = PushNotificationRegistry.pushNotificationService.updateNotifications(notifications)
          result.flatMap {
            case Right(r) => UpdateNotifications200(r)
            case Left(l) => UpdateNotifications500(l)
          }
        }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.updateNotifications
        }
        val getDataApplications = getDataApplicationsAction {  _ =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.getDataApplications
            RequestContext.execInContext[Future[GetDataApplicationsType[T] forSome { type T }]]("getDataApplications") { () =>
            GetDataApplications200(DashboardRegistry.dashboardService.getAllDataApp)
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.getDataApplications
        }
        val kyloSystemName = kyloSystemNameAction { (name: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.kyloSystemName
            RequestContext.execInContext[Future[KyloSystemNameType[T] forSome { type T }]]("kyloSystemName") { () =>
            val systemUrl = ConfigReader.kyloSystemUrl
            val url = systemUrl + name
            //val sysName =

            val test: Future[InferSystem_nameKyloGetResponses200] = ws.url(url)
              .withAuth(ConfigReader.kyloUser, ConfigReader.kyloPwd, WSAuthScheme.BASIC)
              .get().map { resp =>
              resp.body
              InferSystem_nameKyloGetResponses200(Some(resp.body))
            }
            KyloSystemName200(test)
          }
            //NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.kyloSystemName
        }
        val publicStories = publicStoriesAction { input: (DistributionLabel, TTLErrorType, PublicDashboardsGetLimit) =>
            val (org, page, limit) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.publicStories
            RequestContext.execInContext[Future[PublicStoriesType[T] forSome { type T }]]("publicStories") { () =>
            PublicStories200(DashboardRegistry.dashboardRepository.storiesPublic(org))
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.publicStories
        }
        val getAllSystemNotifications = getAllSystemNotificationsAction {  _ =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.getAllSystemNotifications
            RequestContext.execInContext[Future[GetAllSystemNotificationsType[T] forSome { type T }]]("getAllSystemNotifications") { () =>
            if(CredentialManager.isDafSysAdmin(currentRequest)){
              PushNotificationRegistry.pushNotificationService.getAllSystemNotification flatMap {
                case Right(success) => GetAllSystemNotifications200(success)
                case Left(error)    => GetAllSystemNotifications500(error)
              }
            } else {
              logger.debug("Only SysAdmin can get sys notifications")
              GetAllSystemNotifications401(Future.successful(Error(Some(401), Some("Only SysAdmin can get sys notifications"), None)))
            }
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.getAllSystemNotifications
        }
        val catalogDatasetCount = catalogDatasetCountAction { input: (String, String) =>
            val (catalogName, apikey) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.catalogDatasetCount
            RequestContext.execInContext[Future[CatalogDatasetCountType[T] forSome { type T }]]("catalogDatasetCount") { () =>
            val distribution: Seq[Distribution] = ComponentRegistry.monitorService.catalogDatasetCount(catalogName)
            CatalogDatasetCount200(distribution)
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.catalogDatasetCount
        }
        val savedashboard = savedashboardAction { input: (Dashboard, LayoutDataStoryIsDraggable) =>
            val (dashboard, shared) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.savedashboard
            RequestContext.execInContext[Future[SavedashboardType[T] forSome { type T }]]("savedashboard") { () =>
            val credentials = CredentialManager.readCredentialFromRequest(currentRequest)
            val user = credentials.username
            val token = readTokenFromRequest(currentRequest.headers)
            token match {
              case Some(t) => Savedashboard200(DashboardRegistry.dashboardService.saveDashboard(dashboard, user, shared, t, ws))
              case None => Savedashboard401("No token found")
            }
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.savedashboard
        }
        val publicDashboards = publicDashboardsAction { input: (DistributionLabel, TTLErrorType, PublicDashboardsGetLimit) =>
            val (org, page, limit) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.publicDashboards
            RequestContext.execInContext[Future[PublicDashboardsType[T] forSome { type T }]]("publicDashboards") { () =>
            PublicDashboards200(DashboardRegistry.dashboardService.dashboardsPublic(org))
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.publicDashboards
        }
        val getLastOffset = getLastOffsetAction { (topicName: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.getLastOffset
            RequestContext.execInContext[Future[GetLastOffsetType[T] forSome { type T }]]("getLastOffset") { () =>
            PushNotificationRegistry.pushNotificationService.getLastOffset(topicName) flatMap{
              case Right(offset) => GetLastOffset200(offset)
              case Left(error)   => GetLastOffset500(error)
            }
          }
//          NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.getLastOffset
        }
        val catalogBrokenLinks = catalogBrokenLinksAction { input: (String, String) =>
            val (catalogName, apikey) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.catalogBrokenLinks
            RequestContext.execInContext[Future[CatalogBrokenLinksType[T] forSome { type T }]]("catalogBrokenLinks") { () =>
            val brokenLinks: Seq[BrokenLink] = ComponentRegistry.monitorService.catalogBrokenLinks(catalogName)
            CatalogBrokenLinks200(brokenLinks)
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.catalogBrokenLinks
        }
        val publicDashboardsById = publicDashboardsByIdAction { (dashboard_id: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.publicDashboardsById
            RequestContext.execInContext[Future[PublicDashboardsByIdType[T] forSome { type T }]]("publicDashboardsById") { () =>
            DashboardRegistry.dashboardService.publicDashboardById(dashboard_id) match {
              case Some(d) => PublicDashboardsById200(d)
              case _ => PublicDashboardsById404(Error(Some(404), Some("Dashboard non trovata"), None))
            }
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.publicDashboardsById
        }
        val insertTtl = insertTtlAction { (ttl: InsertTTLInfo) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.insertTtl
            RequestContext.execInContext[Future[InsertTtlType[T] forSome { type T }]]("insertTtl") { () =>

            if(ttl.partialFilter.isEmpty) {
              logger.debug("partial filter not found in request")
              InsertTtl400(Error(Some(400), Some("no ttl found"), None))
            } else if(!CredentialManager.isDafSysAdmin(currentRequest)) {
              logger.debug("only daf sys admin can insert ttl")
              InsertTtl401(Error(Some(401), Some("only daf sys admin can insert ttl"), None))
            } else {
              PushNotificationRegistry.pushNotificationService.insertTtl(ttl) flatMap {
                case Right(success) => InsertTtl200(success)
                case Left(error)    => InsertTtl500(error)
              }
            }
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.insertTtl
        }
        val isDatasetOnMetabase = isDatasetOnMetabaseAction { (dataset_name: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.isDatasetOnMetabase
            RequestContext.execInContext[Future[IsDatasetOnMetabaseType[T] forSome { type T }]]("isDatasetOnMetabase") { () =>
            val conf = Configuration.load(Environment.simple())
            val URL = conf.getString("app.local.url").get
            val finalUrl = URL + s"/metabase/is_table/$dataset_name"
            println(finalUrl)
            val isOnMetabase = ws.url(finalUrl)
              //  .withHeaders("Content-Type" -> "application/json",
              //    "Accept" -> "application/json"
              // )
              .get().map { resp =>
              println(resp.body)
              val isOnMetabase = resp.body.trim.toBoolean
              MetabaseTableDataset_nameGetResponses200(Some(isOnMetabase))
            }
            IsDatasetOnMetabase200(isOnMetabase)
          }
          //NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.isDatasetOnMetabase
        }
        val allBrokenLinks = allBrokenLinksAction { (apikey: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.allBrokenLinks
            RequestContext.execInContext[Future[AllBrokenLinksType[T] forSome { type T }]]("allBrokenLinks") { () =>
            val allBrokenLinks = ComponentRegistry.monitorService.allBrokenLinks()
            AllBrokenLinks200(allBrokenLinks)
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.allBrokenLinks
        }
        val publicStoriesbyid = publicStoriesbyidAction { (story_id: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.publicStoriesbyid
            RequestContext.execInContext[Future[PublicStoriesbyidType[T] forSome { type T }]]("publicStoriesbyid") { () =>
            DashboardRegistry.dashboardService.publicStoryById(story_id) match {
              case Some(s) => PublicStoriesbyid200(s)
              case _ => PublicStoriesbyid404(Error(Some(404), Some("User-Story non trovata"), None))
            }
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.publicStoriesbyid
        }
        val updateTable = updateTableAction { input: (File, String, String, String) =>
            val (upfile, tableName, fileType, apikey) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.updateTable
            RequestContext.execInContext[Future[UpdateTableType[T] forSome { type T }]]("updateTable") { () =>
            val success = DashboardRegistry.dashboardService.update(upfile, tableName, fileType)
            UpdateTable200(success)
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.updateTable
        }
        val getSubscriptions = getSubscriptionsAction { (user: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.getSubscriptions
            RequestContext.execInContext[Future[GetSubscriptionsType[T] forSome { type T }]]("getSubscriptions") { () =>
            GetSubscriptions200(PushNotificationRegistry.pushNotificationService.getSubscriptions(user))
          }
//            NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.getSubscriptions
        }
        val deletestory = deletestoryAction { (story_id: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.deletestory
            RequestContext.execInContext[Future[DeletestoryType[T] forSome { type T }]]("deletestory") { () =>
            Deletestory200(DashboardRegistry.dashboardService.deleteStory(story_id))
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.deletestory
        }
        val updateSystemNotification = updateSystemNotificationAction { input: (SysNotificationInfo, Int) =>
            val (notificationInfo, offset) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.updateSystemNotification
            RequestContext.execInContext[Future[UpdateSystemNotificationType[T] forSome { type T }]]("updateSystemNotification") { () =>
              def parseError(error: Error) = {
                error.code match {
                  case Some(404) => UpdateSystemNotification404(error)
                  case _         => UpdateSystemNotification500(error)
                }
              }
            if(CredentialManager.isDafSysAdmin(currentRequest)){
              PushNotificationRegistry.pushNotificationService.updateSystemNotification(offset, notificationInfo) flatMap{
                case Right(success) => UpdateSystemNotification200(success)
                case Left(error)    => parseError(error)
              }

            } else {
              logger.debug("Only SysAdmin can update sys notification")
              UpdateSystemNotification401(Future.successful(Error(Some(401), Some("Only SysAdmin can update sys notification"), None)))
            }
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.updateSystemNotification
        }
        val catalogDistrubutionGroups = catalogDistrubutionGroupsAction { input: (String, String) =>
            val (catalogName, apikey) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.catalogDistrubutionGroups
            RequestContext.execInContext[Future[CatalogDistrubutionGroupsType[T] forSome { type T }]]("catalogDistrubutionGroups") { () =>
            val distributions: Seq[Distribution] = ComponentRegistry.monitorService.datasetCatalogGroup(catalogName)
            CatalogDistrubutionGroups200(distributions)
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.catalogDistrubutionGroups
        }
        val getTtl = getTtlAction {  _ =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.getTtl
            RequestContext.execInContext[Future[GetTtlType[T] forSome { type T }]]("getTtl") { () =>
              def parseResponse(error: Error) = {
                error.code.getOrElse(0) match {
                  case 404  => GetTtl404(error)
                  case _    => GetTtl500(error)
                }
              }
              if(CredentialManager.isDafSysAdmin(currentRequest)) {
                PushNotificationRegistry.pushNotificationService.getTtl.flatMap{
                  case Right(r) => GetTtl200(r)
                  case Left(l)  => parseResponse(l)
                }
              } else {
                GetTtl401(Error(Some(401), Some("Only SysAdmin can get ttl"), None))
              }

          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.getTtl
        }
        val saveDataForNifi = saveDataForNifiAction { input: (File, String) =>
            val (upfile, path_to_save) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.saveDataForNifi
            NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.saveDataForNifi
        }
        val deletedashboard = deletedashboardAction { (dashboard_id: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.deletedashboard
            RequestContext.execInContext[Future[DeletedashboardType[T] forSome { type T }]]("deletedashboard") { () =>
            Deletedashboard200(DashboardRegistry.dashboardService.deleteDashboard(dashboard_id))
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.deletedashboard
        }
        val deleteSubscription = deleteSubscriptionAction { (subscription: Subscription) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.deleteSubscription
            RequestContext.execInContext[Future[DeleteSubscriptionType[T] forSome { type T }]]("deleteSubscription") { () =>
            val username = CredentialManager.readCredentialFromRequest(currentRequest).username
            val result = PushNotificationRegistry.pushNotificationService.deleteSubscription(username, subscription)
            result.flatMap {
              case Right(r) => DeleteSubscription200(r)
              case Left(l) => DeleteSubscription404(l)
            }
          }
//            NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.deleteSubscription
        }
        val dashboardOpenIframes = dashboardOpenIframesAction {  _ =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.dashboardOpenIframes
            RequestContext.execInContext[Future[DashboardOpenIframesType[T] forSome { type T }]]("dashboardOpenIframes") { () =>
            val openDataUser = ConfigReader.getSupersetOpenDataUser
            val openIframes = DashboardRegistry.dashboardService.iframes(openDataUser, ws)
            DashboardOpenIframes200(openIframes)
          }
//          NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.dashboardOpenIframes
        }
        val kyloInferschema = kyloInferschemaAction { input: (File, String) =>
            val (upfile, fileType) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.kyloInferschema
            RequestContext.execInContext[Future[KyloInferschemaType[T] forSome { type T }]]("kyloInferschema") { () =>
            // TODO refactor and parametrize when dealing with other format
            val inferUrl = ConfigReader.kyloInferUrl
            val serde = fileType match {
              case "csv" => ConfigReader.kyloCsvSerde
              case "json" => ConfigReader.kyloJsonSerde
            }

            var file = upfile
            if (fileType.equals("csv")) {
              implicit val codec = Codec("UTF-8")
              codec.onMalformedInput(CodingErrorAction.REPLACE)
              codec.onUnmappableCharacter(CodingErrorAction.REPLACE)

              val lines: Iterator[String] = Source.fromFile(upfile).getLines
              val header = lines.next()
              val separator = InferSchema.inferSeparator(header, InferSchema.SEPARATORS)
              val columns = header.split(separator)
              val newColumns = columns.map(col => {
                val newCol = if (col.trim.contains(" "))
                  col.replaceAll(" ", "_").trim()
                else col.trim()
                newCol
              })
              val newHeader = newColumns.mkString(separator)

              val tempFile = TemporaryFile(prefix = file.getName).file
              val writer = new PrintWriter(tempFile)
              writer.println(newHeader)
              for (line <- lines) {
                writer.println(line)
              }
              writer.close()
              file = tempFile
            }

            val response = ws.url(inferUrl)
              .withAuth(ConfigReader.kyloUser, ConfigReader.kyloPwd, WSAuthScheme.BASIC)
              .post(akka.stream.scaladsl.Source(FilePart("file", file.getName,
                Option("text/csv"), FileIO.fromFile(file)) :: DataPart("parser",
                serde) :: List()))

            response.flatMap(r => {
              logger.debug(Json.stringify(r.json))
              KyloInferschema200(Json.stringify(r.json))
            })
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.kyloInferschema
        }
        val monitorcatalogs = monitorcatalogsAction { (apikey: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.monitorcatalogs
            NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.monitorcatalogs
        }
        val getAllPublicSystemNotifications = getAllPublicSystemNotificationsAction {  _ =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.getAllPublicSystemNotifications
            RequestContext.execInContext[Future[GetAllPublicSystemNotificationsType[T] forSome { type T }]]("getAllPublicSystemNotifications") { () =>
            PushNotificationRegistry.pushNotificationService.getAllPublicSystemNotifications flatMap{
              case Right(success) => GetAllPublicSystemNotifications200(success)
              case Left(error)    => GetAllPublicSystemNotifications500(error)
            }
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.getAllPublicSystemNotifications
        }
        val saveSettings = saveSettingsAction { input: (String, Settings) =>
            val (domain, settings) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.saveSettings
            RequestContext.execInContext[Future[SaveSettingsType[T] forSome { type T }]]("saveSettings") { () =>
            val response: Either[Error, Success] = SettingsRegistry.settingsService.saveSettings(domain, settings)
            if (response.isRight)
              SaveSettings200(response.right.get)
            else
              SaveSettings400(response.left.get)
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.saveSettings
        }
        val deleteSettings = deleteSettingsAction { input: (String, Settings) =>
            val (domain, settings) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.deleteSettings
            NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.deleteSettings
        }
        val createTable = createTableAction { input: (File, String, String, String) =>
            val (upfile, tableName, fileType, apikey) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.createTable
            RequestContext.execInContext[Future[CreateTableType[T] forSome { type T }]]("createTable") { () =>
            val success = DashboardRegistry.dashboardService.save(upfile, tableName, fileType, ws)
            CreateTable200(success)
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.createTable
        }
        val saveNotification = saveNotificationAction { (notification: Notification) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.saveNotification
            RequestContext.execInContext[Future[SaveNotificationType[T] forSome { type T }]]("saveNotification") { () =>
            val resul = PushNotificationRegistry.pushNotificationService.saveNotifications(notification)
            resul.flatMap {
              case Right(r) => SaveNotification200(r)
              case Left(l) => SaveNotification500(l)
            }
          }
//            NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.saveNotification
        }
        val getDomains = getDomainsAction {  _ =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.getDomains
            RequestContext.execInContext[Future[GetDomainsType[T] forSome { type T }]]("getDomains") { () =>
            val credentials = CredentialManager.readCredentialFromRequest(currentRequest)

            val result = for {
              orgs <- getUserOrgs(credentials.username)
              out <- Future.successful {
                SettingsRegistry.settingsRepository.getDomain(
                  orgs.toList, CredentialManager.isDafSysAdmin(currentRequest))
              }
            } yield out
            GetDomains200(result)
          }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.getDomains
        }
    
     // Dead code for absent methodFtd_apiYaml.kyloInfersch
     /*
  // ----- Start of unmanaged code area for action  Ftd_apiYaml.kyloInfersch
  val response = ws.url("http://tba-kylo-services.default.svc.cluster.local:8420/api/v1/schema-discovery/hive/sample-file")
      .withAuth("dladmin", "thinkbig", WSAuthScheme.BASIC)
    .post(akka.stream.scaladsl.Source(FilePart("agency_infer", "agency_infer.csv",
        Option("text/csv"), FileIO.fromFile(upfile)) :: DataPart("parser",
        """{   "name": "CSV",   "objectClassType": "com.thinkbiganalytics.discovery.parsers.csv.CSVFileSchemaParser",   "objectShortClassType": "CSVFileSchemaParser",   "supportsBinary": false,   "generatesHiveSerde": true,   "clientHelper": null }""") :: List()))


  response.map(r => {
      logger.debug(r.body)
  })


  NotImplementedYet
  // ----- End of unmanaged code area for action  Ftd_apiYaml.kyloInferschema
     */

    
     // Dead code for absent methodFtd_apiYaml.getsport
     /*
   // ----- Start of unmanaged code area for action  Ftd_apiYaml.getsport
   NotImplementedYet
   // ----- End of unmanaged code area for action  Ftd_apiYaml.getsport
     */

    
     // Dead code for absent methodFtd_apiYaml.sport
     /*
   // ----- Start of unmanaged code area for action  Ftd_apiYaml.sport

   NotImplementedYet
   // ----- End of unmanaged code area for action  Ftd_apiYaml.sport
     */

    
    }
}
