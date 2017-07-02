
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

import play.api.libs.json.JsNumber
import services.ComponentRegistry
import services.dashboard.DashboardRegistry
import play.api.libs.json.JsValue
import play.libs.Json
import services.ckan.CkanRegistry

/**
 * This controller is re-generated after each change in the specification.
 * Please only place your hand-written code between appropriate comments in the body of the controller.
 */

package ftd_api.yaml {
    // ----- Start of unmanaged code area for package Ftd_apiYaml
        
    // ----- End of unmanaged code area for package Ftd_apiYaml
    class Ftd_apiYaml @Inject() (
        // ----- Start of unmanaged code area for injections Ftd_apiYaml

        // ----- End of unmanaged code area for injections Ftd_apiYaml
        val messagesApi: MessagesApi,
        lifecycle: ApplicationLifecycle,
        config: ConfigurationProvider
    ) extends Ftd_apiYamlBase {
        // ----- Start of unmanaged code area for constructor Ftd_apiYaml

        // ----- End of unmanaged code area for constructor Ftd_apiYaml
        val catalogDistributionLicense = catalogDistributionLicenseAction { input: (String, String) =>
            val (catalogName, apikey) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.catalogDistributionLicense
            val distributions: Seq[Distribution] = ComponentRegistry.monitorService.datasetCatalogLicenses(catalogName)
            CatalogDistributionLicense200(distributions)
            //NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.catalogDistributionLicense
        }
        val dashboardTables = dashboardTablesAction { (apikey: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.dashboardTables
            val tables = DashboardRegistry.dashboardService.tables()
            DashboardTables200(tables)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.dashboardTables
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
        val createckandataset = createckandatasetAction { (dataset: Dataset) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.createckandataset
            val jsonv : JsValue = ResponseWrites.DatasetWrites.writes(dataset)
            CkanRegistry.ckanService.createDataset(jsonv)
            Createckandataset200( dataset )
            //println(" --> jsonv= "+jsonv)
            //val appo : String = (Json.toJson(dataset)).toString
            //val jsonv : JsValue = play.api.libs.json.Json.parse( appo )
            // ----- End of unmanaged code area for action  Ftd_apiYaml.createckandataset
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
        val getmonitorCatalogs = getmonitorCatalogsAction { (apikey: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.getmonitorCatalogs
            val catalogs = ComponentRegistry.monitorService.allCatalogs()
            GetmonitorCatalogs200(catalogs)
            //   NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.getmonitorCatalogs
        }
        val createTable = createTableAction { input: (File, String, String, String) =>
            val (upfile, tableName, fileType, apikey) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.createTable
            val success = DashboardRegistry.dashboardService.save(upfile,tableName,fileType)
            CreateTable200(success)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.createTable
        }
    
     // Dead code for absent methodFtd_apiYaml.createckandatase
     /*
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.createckandatase
            val jsonval: JsValue = play.api.libs.json.Json.parse( Json toJson dataset )
            ComponentRegistry.monitorService.createDataset(jsonval)
            Createckandataset200()
            // ----- End of unmanaged code area for action  Ftd_apiYaml.createckandataset
     */

    
    }
}
