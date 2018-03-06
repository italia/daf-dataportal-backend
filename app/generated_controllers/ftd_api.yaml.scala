
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
import it.gov.daf.common.utils.WebServiceUtil
import play.api.libs.ws.WSClient
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.Base64
import play.api.Environment
import scala.io.Source
import play.api.libs.json._
import services.settings.SettingsRegistry
import utils.InferSchema._
import akka.stream.scaladsl.FileIO
import play.api.mvc.MultipartFormData.{DataPart,FilePart}
import play.api.libs.ws.WSAuthScheme
import utils.ConfigReader
import it.gov.daf.common.authentication.Role
import java.io.PrintWriter
import play.api.libs.Files.TemporaryFile
import play.api.libs.ws.WSResponse
import scala.concurrent.Future
import utils.InferSchema
import java.nio.charset.CodingErrorAction
import scala.io.Codec

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

        // ----- End of unmanaged code area for constructor Ftd_apiYaml
        val catalogDistributionLicense = catalogDistributionLicenseAction { input: (String, String) =>
            val (catalogName, apikey) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.catalogDistributionLicense
            val distributions: Seq[Distribution] = ComponentRegistry.monitorService.datasetCatalogLicenses(catalogName)
      CatalogDistributionLicense200(distributions)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.catalogDistributionLicense
        }
        val wsKyloInferschema = wsKyloInferschemaAction { input: (String, String, Credentials) =>
            val (url, file_type, credentials) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.wsKyloInferschema
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

                  val tempFile = TemporaryFile(prefix = "uploaded").file
                  val pw = new PrintWriter(tempFile)
                  pw.write(stringified)
                  pw.close
                  tempFile
                }
                case "csv" =>   {
                  val first10lines = resp.body.split("\n").slice(0,10)
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

            val response   =  for {
                ff <- tempFile
                resp <- inferKylo(ff)
            } yield resp

            response.flatMap(r => {
              logger.debug(Json.stringify(r))
              WsKyloInferschema200(Json.stringify(r))
            })

       //   NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.wsKyloInferschema
        }
        val settingsByName = settingsByNameAction { (domain: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.settingsByName
            val response = SettingsRegistry.settingsService.settingsByName(domain)
      if (response.isRight)
        SettingsByName200(response.right.get)
      else
        SettingsByName400(response.left.get)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.settingsByName
        }
        val savestories = savestoriesAction { (story: UserStory) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.savestories
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)
      Savestories200(DashboardRegistry.dashboardService.saveStory(story, credentials.username))
            // ----- End of unmanaged code area for action  Ftd_apiYaml.savestories
        }
        val createSnapshot = createSnapshotAction { input: (File, String, String) =>
            val (upfile, snapshot_id, apikey) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.createSnapshot
            upfile.renameTo(new File("public/img", snapshot_id + ".png"));
      CreateSnapshot200(Success(Some("File created"), Some("File created")))
            // ----- End of unmanaged code area for action  Ftd_apiYaml.createSnapshot
        }
        val stories = storiesAction { input: (ErrorCode, ErrorCode, PublicDashboardsGetLimit) =>
            val (status, page, limit) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.stories
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)
      Stories200(DashboardRegistry.dashboardService.stories(
        credentials.groups.toList.filterNot(g => Role.roles.contains(g)), status, page, limit)
      )
            // ----- End of unmanaged code area for action  Ftd_apiYaml.stories
        }
        val dashboardTables = dashboardTablesAction { (apikey: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.dashboardTables
            val tables = DashboardRegistry.dashboardService.tables()
      DashboardTables200(tables)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.dashboardTables
        }
        val dashboardIframes = dashboardIframesAction {  _ =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.dashboardIframes
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)
      val iframes = DashboardRegistry.dashboardService.iframes(credentials.username)
      DashboardIframes200(iframes)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.dashboardIframes
        }
        val iframesByTableName = iframesByTableNameAction { (tableName: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.iframesByTableName
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)
          val iframes = DashboardRegistry.dashboardService.iframes(credentials.username)
          val iframesByName = iframes.map(_.filter(_.table.getOrElse("").endsWith(tableName)))
          IframesByTableName200(iframesByName)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.iframesByTableName
        }
        val allDistributionLiceses = allDistributionLicesesAction { (apikey: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.allDistributionLiceses
            val distributions: Seq[Distribution] = ComponentRegistry.monitorService.allDistributionLiceses()
      AllDistributionLiceses200(distributions)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.allDistributionLiceses
        }
        val catalogDistrubutionFormat = catalogDistrubutionFormatAction { input: (String, String) =>
            val (catalogName, apikey) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.catalogDistrubutionFormat
            val distributions: Seq[Distribution] = ComponentRegistry.monitorService.datasetCatalogFormat(catalogName)
      CatalogDistrubutionFormat200(distributions)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.catalogDistrubutionFormat
        }
        val allDistributionGroups = allDistributionGroupsAction { (apikey: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.allDistributionGroups
            val distributions: Seq[Distribution] = ComponentRegistry.monitorService.allDistributionGroup()
      AllDistributionGroups200(distributions)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.allDistributionGroups
        }
        val allDatasets = allDatasetsAction { (apikey: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.allDatasets
            val distributions: Seq[Distribution] = ComponentRegistry.monitorService.datasetsCount()
      AllDatasets200(distributions)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.allDatasets
        }
        val storiesbyid = storiesbyidAction { (story_id: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.storiesbyid
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)
      Storiesbyid200(DashboardRegistry.dashboardService.storyById(
        credentials.groups.toList.filterNot(g => Role.roles.contains(g)), story_id)
      )
            // ----- End of unmanaged code area for action  Ftd_apiYaml.storiesbyid
        }
        val kyloFeedByName = kyloFeedByNameAction { (feed_name: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.kyloFeedByName
            val kyloUrl = ConfigReader.kyloUrl + "/api/v1/feedmgr/feeds/by-name/" + feed_name
          val feed = ws.url(kyloUrl)
            .withAuth(ConfigReader.kyloUser, ConfigReader.kyloPwd, WSAuthScheme.BASIC)
            .get().map{ resp =>
               println(resp.body)
               val name = (resp.json \ "feedName").as[String]
               val active = (resp.json \ "active").as[Boolean]
               val state = (resp.json \ "state").as[String]
               val updatedate =  (resp.json \ "updateDate").as[Long]
               KyloFeed(name,state,active,updatedate)

          }
          KyloFeedByName200(feed)
         // NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.kyloFeedByName
        }
        val snapshotbyid = snapshotbyidAction { input: (String, String) =>
            val (iframe_id, sizexsize) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.snapshotbyid
            val conf = Configuration.load(Environment.simple())
      val URL: String = conf.getString("daf-cacher.url").get

      val url = URL + iframe_id + "/" + sizexsize
      val response = ws.url(url).get().map(x => {
        x.bodyAsBytes
        val d = x.bodyAsBytes.toArray
        println(d)
        Base64.getEncoder.encodeToString(d)
      })
      Snapshotbyid200(response)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.snapshotbyid
        }
        val dashboards = dashboardsAction { input: (ErrorCode, ErrorCode, PublicDashboardsGetLimit) =>
            val (status, page, limit) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.dashboards
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)
      Dashboards200(DashboardRegistry.dashboardService.dashboards(credentials.groups.toList.filterNot(g => Role.roles.contains(g)), status))
            // ----- End of unmanaged code area for action  Ftd_apiYaml.dashboards
        }
        val inferschema = inferschemaAction { input: (File, String) =>
            val (upfile, fileType) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.inferschema
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
            // ----- End of unmanaged code area for action  Ftd_apiYaml.inferschema
        }
        val supersetTableFromDataset = supersetTableFromDatasetAction { (dataset_name: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.supersetTableFromDataset
            val conf = Configuration.load(Environment.simple())
          val URL = conf.getString("app.local.url").get
          val supersetUrl = conf.getString("superset.url").get
          val username = WebServiceUtil.readCredentialFromRequest(currentRequest).username

          val tables = ws.url(URL + s"/superset/table/$username/$dataset_name")
            .withHeaders("Content-Type" -> "application/json",
              "Accept" -> "application/json"
            ).get().map { resp =>
              println(resp.body)
              val tables = resp.json.as[Seq[JsValue]]
              val supersetTables: Seq[SupersetUrl] = tables.map(x => {
                val id = (x \ "id").as[Int]
                val stringId = id.toString
                val tableName = (x \ "text").as[String]
                val url = s"$supersetUrl/superset/explore/table/$stringId/"
                SupersetUrl(id,tableName,url)
              })
             supersetTables
          }

          SupersetTableFromDataset200(tables)
        //  NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.supersetTableFromDataset
        }
        val dashboardsbyid = dashboardsbyidAction { (dashboard_id: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.dashboardsbyid
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)
            Dashboardsbyid200(DashboardRegistry.dashboardService.dashboardById(
              credentials.groups.toList.filterNot(g => Role.roles.contains(g)), dashboard_id)
            )
            // ----- End of unmanaged code area for action  Ftd_apiYaml.dashboardsbyid
        }
        val allDistributionFormats = allDistributionFormatsAction { (apikey: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.allDistributionFormats
            val distributions: Seq[Distribution] = ComponentRegistry.monitorService.allDistributionFormat()
      AllDistributionFormats200(distributions)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.allDistributionFormats
        }
        val dashboardIframesbyorg = dashboardIframesbyorgAction { (orgName: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.dashboardIframesbyorg
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)
            val iframes = DashboardRegistry.dashboardService.iframesByOrg(credentials.username,orgName)
            DashboardIframesbyorg200(iframes)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.dashboardIframesbyorg
        }
        val kyloSystemName = kyloSystemNameAction { (name: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.kyloSystemName
            val systemUrl = ConfigReader.kyloSystemUrl
          val url = systemUrl + name
          //val sysName =

          val test: Future[InferSystem_nameKyloGetResponses200] = ws.url(url)
            .withAuth(ConfigReader.kyloUser, ConfigReader.kyloPwd, WSAuthScheme.BASIC)
            .get().map{ resp =>
              resp.body
             InferSystem_nameKyloGetResponses200(Some(resp.body))
          }
          KyloSystemName200(test)
            //NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.kyloSystemName
        }
        val publicStories = publicStoriesAction { input: (ErrorCode, ErrorCode, PublicDashboardsGetLimit) =>
            val (status, page, limit) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.publicStories
            PublicStories200(DashboardRegistry.dashboardRepository.storiesPublic(status))
            // ----- End of unmanaged code area for action  Ftd_apiYaml.publicStories
        }
        val catalogDatasetCount = catalogDatasetCountAction { input: (String, String) =>
            val (catalogName, apikey) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.catalogDatasetCount
            val distribution: Seq[Distribution] = ComponentRegistry.monitorService.catalogDatasetCount(catalogName)
      CatalogDatasetCount200(distribution)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.catalogDatasetCount
        }
        val savedashboard = savedashboardAction { (dashboard: Dashboard) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.savedashboard
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)
      val user = credentials.username
      val save = DashboardRegistry.dashboardService.saveDashboard(dashboard, user)
      Savedashboard200(save)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.savedashboard
        }
        val publicDashboards = publicDashboardsAction { input: (ErrorCode, ErrorCode, PublicDashboardsGetLimit) =>
            val (status, page, limit) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.publicDashboards
            PublicDashboards200(DashboardRegistry.dashboardService.dashboardsPublic(status))
            // ----- End of unmanaged code area for action  Ftd_apiYaml.publicDashboards
        }
        val catalogBrokenLinks = catalogBrokenLinksAction { input: (String, String) =>
            val (catalogName, apikey) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.catalogBrokenLinks
            val brokenLinks: Seq[BrokenLink] = ComponentRegistry.monitorService.catalogBrokenLinks(catalogName)
      CatalogBrokenLinks200(brokenLinks)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.catalogBrokenLinks
        }
        val publicDashboardsById = publicDashboardsByIdAction { (dashboard_id: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.publicDashboardsById
            PublicDashboardsById200(DashboardRegistry.dashboardService.publicDashboardById(dashboard_id))
            // ----- End of unmanaged code area for action  Ftd_apiYaml.publicDashboardsById
        }
        val allBrokenLinks = allBrokenLinksAction { (apikey: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.allBrokenLinks
            val allBrokenLinks = ComponentRegistry.monitorService.allBrokenLinks()
      AllBrokenLinks200(allBrokenLinks)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.allBrokenLinks
        }
        val publicStoriesbyid = publicStoriesbyidAction { (story_id: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.publicStoriesbyid
            PublicStoriesbyid200(DashboardRegistry.dashboardService.publicStoryById(story_id))
            // ----- End of unmanaged code area for action  Ftd_apiYaml.publicStoriesbyid
        }
        val updateTable = updateTableAction { input: (File, String, String, String) =>
            val (upfile, tableName, fileType, apikey) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.updateTable
            val success = DashboardRegistry.dashboardService.update(upfile, tableName, fileType)
      UpdateTable200(success)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.updateTable
        }
        val deletestory = deletestoryAction { (story_id: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.deletestory
            Deletestory200(DashboardRegistry.dashboardService.deleteStory(story_id))
            // ----- End of unmanaged code area for action  Ftd_apiYaml.deletestory
        }
        val catalogDistrubutionGroups = catalogDistrubutionGroupsAction { input: (String, String) =>
            val (catalogName, apikey) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.catalogDistrubutionGroups
            val distributions: Seq[Distribution] = ComponentRegistry.monitorService.datasetCatalogGroup(catalogName)
      CatalogDistrubutionGroups200(distributions)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.catalogDistrubutionGroups
        }
        val saveDataForNifi = saveDataForNifiAction { input: (File, String) =>
            val (upfile, path_to_save) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.saveDataForNifi
            NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.saveDataForNifi
        }
        val deletedashboard = deletedashboardAction { (dashboard_id: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.deletedashboard
            Deletedashboard200(DashboardRegistry.dashboardService.deleteDashboard(dashboard_id))
            // ----- End of unmanaged code area for action  Ftd_apiYaml.deletedashboard
        }
        val kyloInferschema = kyloInferschemaAction { input: (File, String) =>
            val (upfile, fileType) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.kyloInferschema
            // TODO refactor and parametrize when dealing with other format
          val inferUrl = ConfigReader.kyloInferUrl
          val serde = fileType match {
            case "csv" => ConfigReader.kyloCsvSerde
            case "json" => ConfigReader.kyloJsonSerde
          }

          var file = upfile
          if (fileType.equals("csv")){
            implicit val codec = Codec("UTF-8")
            codec.onMalformedInput(CodingErrorAction.REPLACE)
            codec.onUnmappableCharacter(CodingErrorAction.REPLACE)

            val lines: Iterator[String] = Source.fromFile(upfile).getLines
            val header = lines.next()
            val separator = InferSchema.inferSeparator(header, InferSchema.SEPARATORS)
            val columns = header.split(separator)
            val newColumns = columns.map(col => {
              val newCol = if(col.trim.contains(" "))
                col.replaceAll(" ", "_")
              else col
              newCol
            })
            val newHeader = newColumns.mkString(separator)

            val tempFile = TemporaryFile(prefix = file.getName).file
            val writer = new PrintWriter(tempFile)
            writer.println(newHeader)
            for (line <- lines){
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
            // ----- End of unmanaged code area for action  Ftd_apiYaml.kyloInferschema
        }
        val monitorcatalogs = monitorcatalogsAction { (apikey: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.monitorcatalogs
            NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.monitorcatalogs
        }
        val saveSettings = saveSettingsAction { input: (String, Settings) =>
            val (domain, settings) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.saveSettings
            val response: Either[Error, Success] = SettingsRegistry.settingsService.saveSettings(domain, settings)
      if (response.isRight)
        SaveSettings200(response.right.get)
      else
        SaveSettings400(response.left.get)
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
            val success = DashboardRegistry.dashboardService.save(upfile, tableName, fileType)
      CreateTable200(success)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.createTable
        }
        val getDomains = getDomainsAction {  _ =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.getDomains
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)
      val response: Seq[String] = SettingsRegistry.settingsRepository.getDomain(
        credentials.groups.toList.filterNot(g => Role.roles.contains(g)), WebServiceUtil.isDafAdmin(currentRequest)
      )
      GetDomains200(response)
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
