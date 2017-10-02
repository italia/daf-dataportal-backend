
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

import it.gov.daf.common.authentication.Authentication
import org.pac4j.play.store.PlaySessionStore
import services.ComponentRegistry
import services.dashboard.DashboardRegistry
import play.api.Configuration
import it.gov.daf.catalogmanager.utilities.WebServiceUtil

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
        val stories = storiesAction {  _ =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.stories
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)
            Stories200(DashboardRegistry.dashboardService.stories(credentials._1.get))
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
            val credentials = WebServiceUtil.readCredentialFromRequest(currentRequest)
            Storiesbyid200(DashboardRegistry.dashboardService.storyById(credentials._1.get, story_id))
            //Storiesbyid200(DashboardRegistry.dashboardService.storyById("ale", story_id))
            // ----- End of unmanaged code area for action  Ftd_apiYaml.storiesbyid
        }
        val deletestory = deletestoryAction { (story_id: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.deletestory
            Deletestory200(DashboardRegistry.dashboardService.deleteStory(story_id))
           // Delete
            // ----- End of unmanaged code area for action  Ftd_apiYaml.deletestory
        }
        val dashboards = dashboardsAction {  _ =>  
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
    
    }
}
