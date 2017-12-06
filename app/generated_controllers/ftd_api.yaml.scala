
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
import play.api.Configuration
import services.ComponentRegistry
import services.dashboard.DashboardRegistry
import play.api.Configuration
import it.gov.daf.common.utils.WebServiceUtil
import akka.util.ByteString
import play.api.libs.ws.WSClient
import scala.concurrent.ExecutionContext.Implicits.global
import java.io.{ByteArrayInputStream,InputStreamReader}
import java.util.Base64
import com.google.common.base.Charsets
import com.google.common.io.{ByteStreams,CharStreams}
import scala.io.BufferedSource
import com.google.common.io.ByteStreams
import com.google.common.io.ByteStreams
import com.google.common.io.ByteStreams
import com.google.common.io.ByteStreams
import com.google.common.io.ByteStreams
import com.google.common.io.ByteStreams
import scala.concurrent.Future
import play.api.Environment
import scala.io.Source
import play.api.libs.json._
import services.settings.SettingsRegistry

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
        val GENERIC_ERROR=Error(None,Some("An Error occurred"),None)

        // ----- End of unmanaged code area for constructor Ftd_apiYaml
        val catalogDistributionLicense = catalogDistributionLicenseAction { input: (String, String) =>
            val (catalogName, apikey) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.catalogDistributionLicense
            val distributions: Seq[Distribution] = ComponentRegistry.monitorService.datasetCatalogLicenses(catalogName)
            CatalogDistributionLicense200(distributions)
            //NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.catalogDistributionLicense
        }
        val getsettings = getsettingsAction { (organization: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.getsettings
            Getsettings200(SettingsRegistry.settingsService.settingsByName(organization))
            // ----- End of unmanaged code area for action  Ftd_apiYaml.getsettings
        }
        val saveSettings = saveSettingsAction { input: (String, Settings) =>
            val (organization, settings) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.saveSettings
            //            SaveSettings200(SettingsRegistry.settingsService.saveSettings(organization, settings))
            val response = SettingsRegistry.settingsService.saveSettings(organization, settings)
          SaveSettings200(response)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.saveSettings
        }
        val deleteSettings = deleteSettingsAction { input: (DistributionLabel, Settings) =>
            val (organization, settings) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.deleteSettings
            NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.deleteSettings
        }
        val createSnapshot = createSnapshotAction { input: (File, String, String) =>
            val (upfile, snapshot_id, apikey) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.createSnapshot
            //println(Environment.simple().rootPath().toString)
            upfile.renameTo(new File("public/img", snapshot_id + ".png"));
            CreateSnapshot200(Success(Some("File created"), Some("File created")))
            // ----- End of unmanaged code area for action  Ftd_apiYaml.createSnapshot
        }
        val stories = storiesAction { input: (ErrorCode, ErrorCode, DashboardsGetLimit) =>
            val (status, page, limit) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.stories
            // Temporary without authorization
            //val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)
            Stories200(DashboardRegistry.dashboardService.stories("unknown", status, page, limit))
            //Stories200(DashboardRegistry.dashboardService.stories("ale"))
           // NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.stories
        }
        val savestories = savestoriesAction { (story: UserStory) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.savestories
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)
            Savestories200(DashboardRegistry.dashboardService.saveStory(story,credentials._1.get))
            // ----- End of unmanaged code area for action  Ftd_apiYaml.savestories
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
            //val iframes = DashboardRegistry.dashboardService.iframes("alessandro@teamdigitale.governo.it")
            val iframes = DashboardRegistry.dashboardService.iframes(credentials._1.get)
            DashboardIframes200(iframes)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.dashboardIframes
        }
        val allDistributionLiceses = allDistributionLicesesAction { (apikey: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.allDistributionLiceses
            val distributions : Seq[Distribution] = ComponentRegistry.monitorService.allDistributionLiceses()
            AllDistributionLiceses200(distributions)
            // NotImplementedYet
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
            // NotImplementedYet
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
            //val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)
            // Temporary removed
            Storiesbyid200(DashboardRegistry.dashboardService.storyById("", story_id))
            //Storiesbyid200(DashboardRegistry.dashboardService.storyById("ale", story_id))
            // ----- End of unmanaged code area for action  Ftd_apiYaml.storiesbyid
        }
        val deletestory = deletestoryAction { (story_id: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.deletestory
            Deletestory200(DashboardRegistry.dashboardService.deleteStory(story_id))
           // Delete
            // ----- End of unmanaged code area for action  Ftd_apiYaml.deletestory
        }
        val snapshotbyid = snapshotbyidAction { input: (String, String) =>
            val (iframe_id, sizexsize) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.snapshotbyid
            //Snapshotbyid200()
            //import scala.concurrent.ExecutionContext.Implicits.global
            val conf = Configuration.load(Environment.simple())
            val URL : String = conf.getString("daf-cacher.url").get

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
        val dashboards = dashboardsAction { input: (ErrorCode, ErrorCode, DashboardsGetLimit) =>
            val (status, page, limit) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.dashboards
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)
            Dashboards200(DashboardRegistry.dashboardService.dashboards(credentials._1.get))
            //Dashboards200(DashboardRegistry.dashboardService.dashboards("ale"))
            // ----- End of unmanaged code area for action  Ftd_apiYaml.dashboards
        }
        val savedashboard = savedashboardAction { (dashboard: Dashboard) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.savedashboard
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)
            val user = credentials._1.get
            val save = DashboardRegistry.dashboardService.saveDashboard(dashboard, user)
            Savedashboard200(save)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.savedashboard
        }
        val inferschema = inferschemaAction { input: (File, String) =>
            val (upfile, fileType) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.inferschema
            if (fileType.equals("csv")) {
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

                val (content, rows) = Source
                  .fromFile(upfile)
                  .getLines.slice(0, 100).duplicate

                val rows20 = rows.slice(0, 20)

                val header = content.next

                def inferSeparator(header: String, supportedSeparators: Seq[String]): String = {
                    val max = supportedSeparators.map(x => {
                        (x, header.count(_ == x))
                    }).maxBy(_._2)
                    max._1
                }

                val separator = inferSeparator(header, Seq(",", ";", "|", ".|.", "\t", ":"))

                val headerRows = rows20.next.split(separator)

                val dataColumn = rows20
                  .toList
                  .flatMap(_.split(separator).zipWithIndex)
                  .groupBy(_._2)
                  .map(_._2)
                  .toList
                  .sortBy(_(0)._2)


                val data = headerRows
                  .zip(dataColumn)
                  .toMap
                  .mapValues(x => (x.map(_._1)))


                val headers: Array[String] = header.split(separator)

                val result = content.map(x => {
                    // val contents: Array[Seq[Option[IDB]]] = x.split(separator).map(_.castTo)
                    val row = x.split(separator)
                    val contents: Array[Seq[String]] = x.split(separator).map(_.castToString.flatten)
                    headers.zip(contents).toMap
                })

                val infered = result
                  .toList
                  .flatten
                  .distinct
                  .groupBy(_._1)
                  //.mapValues(_.map(_._2).flatten)
                  .mapValues((x => x.flatMap(_._2))
                )


                val total = for {
                    (k, v1) <- infered
                    v2 <- data.get(k)
                } yield (InferredType(Some(k), Some(v2), Some(v1)))

                Inferschema200(Inferred(Some(separator), Some("csv"), Some(total.toList)))
            } else {
                object JsFlattener {

                    def apply(js: JsValue): JsValue = flatten(js).foldLeft(JsObject(Nil))(_++_.as[JsObject])

                    def flatten(js: JsValue, prefix: String = ""): Seq[JsValue] = {
                        js.as[JsObject].fieldSet.toSeq.flatMap{ case (key, values) =>
                            values match {
                                case JsBoolean(x) => Seq(Json.obj(concat(prefix, key) -> x))
                                case JsNumber(x) => Seq(Json.obj(concat(prefix, key) -> x))
                                case JsString(x) => Seq(Json.obj(concat(prefix, key) -> x))
                                case JsArray(seq) => seq.zipWithIndex.flatMap{ case (x, i) => flatten(x, concat(prefix, key + s"[$i]")) }
                                case x: JsObject => flatten(x, concat(prefix, key))
                                case _ => Seq(Json.obj(concat(prefix, key) -> JsNull))
                            }
                        }
                    }

                    def concat(prefix: String, key: String): String = if(prefix.nonEmpty) s"$prefix.$key" else key

                }
                Inferschema200(Inferred(None, None, None))
              //  Inferschema200(Seq(InferredType(None,None,None,None,None)))
            }
           // NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.inferschema
        }
        val dashboardsbyid = dashboardsbyidAction { (dashboard_id: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.dashboardsbyid
            Dashboardsbyid200(DashboardRegistry.dashboardService.dashboardById("ale", dashboard_id))
            // ----- End of unmanaged code area for action  Ftd_apiYaml.dashboardsbyid
        }
        val deletedashboard = deletedashboardAction { (dashboard_id: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.deletedashboard
            Deletedashboard200(DashboardRegistry.dashboardService.deleteDashboard(dashboard_id))
            // ----- End of unmanaged code area for action  Ftd_apiYaml.deletedashboard
        }
        val allDistributionFormats = allDistributionFormatsAction { (apikey: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.allDistributionFormats
            //NotImplementedYet
            val distributions: Seq[Distribution] = ComponentRegistry.monitorService.allDistributionFormat()
            AllDistributionFormats200(distributions)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.allDistributionFormats
        }
        val catalogDatasetCount = catalogDatasetCountAction { input: (String, String) =>
            val (catalogName, apikey) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.catalogDatasetCount
            val distribution :Seq[Distribution] = ComponentRegistry.monitorService.catalogDatasetCount(catalogName)
            CatalogDatasetCount200(distribution)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.catalogDatasetCount
        }
        val catalogBrokenLinks = catalogBrokenLinksAction { input: (String, String) =>
            val (catalogName, apikey) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.catalogBrokenLinks
            val brokenLinks :Seq[BrokenLink] = ComponentRegistry.monitorService.catalogBrokenLinks(catalogName)
            CatalogBrokenLinks200(brokenLinks)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.catalogBrokenLinks
        }
        val allBrokenLinks = allBrokenLinksAction { (apikey: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.allBrokenLinks
            val allBrokenLinks = ComponentRegistry.monitorService.allBrokenLinks()
            AllBrokenLinks200(allBrokenLinks)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.allBrokenLinks
        }
        val updateTable = updateTableAction { input: (File, String, String, String) =>
            val (upfile, tableName, fileType, apikey) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.updateTable
            val success = DashboardRegistry.dashboardService.update(upfile,tableName, fileType)
            UpdateTable200(success)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.updateTable
        }
        val catalogDistrubutionGroups = catalogDistrubutionGroupsAction { input: (String, String) =>
            val (catalogName, apikey) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.catalogDistrubutionGroups
            val distributions: Seq[Distribution] = ComponentRegistry.monitorService.datasetCatalogGroup(catalogName)
            CatalogDistrubutionGroups200(distributions)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.catalogDistrubutionGroups
        }
        val monitorcatalogs = monitorcatalogsAction { (apikey: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.monitorcatalogs
            NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.monitorcatalogs
        }
        val createTable = createTableAction { input: (File, String, String, String) =>
            val (upfile, tableName, fileType, apikey) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.createTable
            val success = DashboardRegistry.dashboardService.save(upfile,tableName,fileType)
            CreateTable200(success)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.createTable
        }
    
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
