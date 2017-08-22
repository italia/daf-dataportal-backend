
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

import scala.math.BigInt
import play.api.libs.json.JsNumber
import services.ComponentRegistry
import services.dashboard.DashboardRegistry
import play.api.libs.json.JsValue
import play.libs.Json
import services.ckan.CkanRegistry
import play.api.libs.json.{JsError,JsResult,JsSuccess}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.{JsArray,JsString}
import scala.concurrent.Future
import utils.SecurePasswordHashing

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
        val searchdataset = searchdatasetAction { input: (DistributionLabel, DistributionLabel, ResourceSize) =>
            val (q, sort, rows) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.searchdataset
            val datasetsFuture: Future[JsResult[Seq[Dataset]]] = CkanRegistry.ckanService.searchDatasets(input)
            val eitherDatasets: Future[Either[String, Seq[Dataset]]] = datasetsFuture.map(result => {
                result match {
                    case s: JsSuccess[Seq[Dataset]] => Right(s.get)
                    case e: JsError => Left("error, no datasets")
                }
            })
            // Getckandatasetbyid200(dataset)
            eitherDatasets.flatMap {
                case Right(dataset) => Searchdataset200(dataset)
                case Left(error) => Searchdataset401(Error(None,Option(error),None))
            }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.searchdataset
        }
        val getckanorganizationbyid = getckanorganizationbyidAction { (org_id: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.getckanorganizationbyid
            val orgFuture: Future[JsResult[Organization]] = CkanRegistry.ckanService.getOrganization(org_id)
            val eitherOrg: Future[Either[String, Organization]] = orgFuture.map(result => {
                result match {
                    case s: JsSuccess[Organization] => Right(s.get)
                    case e: JsError => Left("error no organization with that id")
                }
            })

            eitherOrg.flatMap {
                case Right(organization) => Getckanorganizationbyid200(organization)
                case Left(error) => Getckanorganizationbyid401(Error(None,Option(error),None))
            }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.getckanorganizationbyid
        }
        val getckandatasetList = getckandatasetListAction {  _ =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.getckandatasetList
            val eitherOut: Future[Either[Error, Seq[String]]] = CkanRegistry.ckanService.getDatasets().map(result =>{
                result match {
                    case s: JsArray => Right(s.as[Seq[String]])
                    case _ => Left(GENERIC_ERROR)
                }
            })

            eitherOut.flatMap {
                case Right(list) => GetckandatasetList200(list)
                case Left(error) => GetckandatasetList401(error)
            }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.getckandatasetList
        }
        val testmulti = testmultiAction {  _ =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.testmulti
            NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.testmulti
        }
        val dashboardTables = dashboardTablesAction { (apikey: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.dashboardTables
            val tables = DashboardRegistry.dashboardService.tables()
            DashboardTables200(tables)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.dashboardTables
        }
        val dashboardIframes = dashboardIframesAction { (apikey: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.dashboardIframes
            val iframes = DashboardRegistry.dashboardService.iframes("ale.ercolani@gmail.com")
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
        val getckanuserList = getckanuserListAction {  _ =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.getckanuserList
            NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.getckanuserList
        }
        val verifycredentials = verifycredentialsAction { (credentials: Credentials) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.verifycredentials
            //NotImplementedYet
            //val jsonv : JsValue = ResponseWrites.DatasetWrites.writes(dataset)
            CkanRegistry.ckanService.verifyCredentials(credentials) match {
                case true => Verifycredentials200(Success(Some("Success"), Some("User verified")))
                case _ =>  Verifycredentials401(Error(None,Some("Wrong Username or Password"),None))
            }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.verifycredentials
        }
        val createckandataset = createckandatasetAction { (dataset: Dataset) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.createckandataset
            val jsonv : JsValue = ResponseWrites.DatasetWrites.writes(dataset)
            CkanRegistry.ckanService.createDataset(jsonv)flatMap {
                case "true" => Createckandataset200(Success(Some("Success"), Some("dataset created")))
                case _ =>  Createckandataset401(Error(None,Some("An Error occurred"),None))
            }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.createckandataset
        }
        val getckandatasetListWithRes = getckandatasetListWithResAction { input: (ResourceSize, ResourceSize) =>
            val (limit, offset) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.getckandatasetListWithRes
            val datasetsFuture: Future[JsResult[Seq[Dataset]]] = CkanRegistry.ckanService.getDatasetsWithRes(input)
            val eitherDatasets: Future[Either[String, Seq[Dataset]]] = datasetsFuture.map(result => {
                result match {
                    case s: JsSuccess[Seq[Dataset]] => Right(s.get)
                    case e: JsError => Left("error, no datasets")
                }
            })
            // Getckandatasetbyid200(dataset)
            eitherDatasets.flatMap {
                case Right(dataset) => GetckandatasetListWithRes200(dataset)
                case Left(error) => GetckandatasetListWithRes401(Error(None,Option(error),None))
            }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.getckandatasetListWithRes
        }
        val getckanuserorganizationList = getckanuserorganizationListAction { (username: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.getckanuserorganizationList
            //NotImplementedYet
            val orgsFuture: Future[JsResult[Seq[Organization]]] = CkanRegistry.ckanService.getUserOrganizations(username)
            val eitherOrgs: Future[Either[String, Seq[Organization]]] = orgsFuture.map(result => {
                result match {
                    case s: JsSuccess[Seq[Organization]] => Right(s.get)
                    case e: JsError => Left("error, no organization")
                }
            })
            // Getckandatasetbyid200(dataset)
            eitherOrgs.flatMap {
                case Right(orgs) => GetckanuserorganizationList200(orgs)
                case Left(error) => GetckanuserorganizationList401(Error(None,Option(error),None))
            }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.getckanuserorganizationList
        }
        val allDistributionFormats = allDistributionFormatsAction { (apikey: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.allDistributionFormats
            //NotImplementedYet
            val distributions: Seq[Distribution] = ComponentRegistry.monitorService.allDistributionFormat()
            AllDistributionFormats200(distributions)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.allDistributionFormats
        }
        val createckanorganization = createckanorganizationAction { (organization: Organization) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.createckanorganization
            val jsonv : JsValue = ResponseWrites.OrganizationWrites.writes(organization)

            CkanRegistry.ckanService.createOrganization(jsonv)flatMap {
                case "true" => Createckanorganization200(Success(Some("Success"), Some("organization created")))
                case _ =>  Createckanorganization401(GENERIC_ERROR)
            }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.createckanorganization
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
        val updateckanorganization = updateckanorganizationAction { input: (String, Organization) =>
            val (org_id, organization) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.updateckanorganization
            //NotImplementedYet
            val jsonv : JsValue = ResponseWrites.OrganizationWrites.writes(organization)

            CkanRegistry.ckanService.updateOrganization(org_id,jsonv)flatMap {
                case "true" => Updateckanorganization200(Success(Some("Success"), Some("organization updated")))
                case _ =>  Updateckanorganization401(GENERIC_ERROR)
            }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.updateckanorganization
        }
        val getckanuser = getckanuserAction { (username: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.getckanuser
            //NotImplementedYet
            val userResult: JsResult[User] = CkanRegistry.ckanService.getMongoUser(username)
            val eitherUser: Either[String, User] = userResult match {
                    case s: JsSuccess[User] => Right(s.get)
                    case e: JsError => Left("error no user with that name")
                }


            eitherUser match {
                case Right(user) => Getckanuser200(user)
                case Left(error) => Getckanuser401(Error(None,Option(error),None))
            }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.getckanuser
        }
        val savedashboard = savedashboardAction { input: (String, String, Dashboard) =>
            val (user, dashboard, layout) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.savedashboard
            NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.savedashboard
        }
        val allBrokenLinks = allBrokenLinksAction { (apikey: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.allBrokenLinks
            val allBrokenLinks = ComponentRegistry.monitorService.allBrokenLinks()
            AllBrokenLinks200(allBrokenLinks)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.allBrokenLinks
        }
        val createckanuser = createckanuserAction { (user: User) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.createckanuser
            //NotImplementedYet
            val jsonv : JsValue = ResponseWrites.UserWrites.writes(user)
            CkanRegistry.ckanService.createUser(jsonv)flatMap {
                case "true" => Createckanuser200(Success(Some("Success"), Some("user created")))
                case _ =>  Createckanuser401(GENERIC_ERROR)
            }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.createckanuser
        }
        val updateTable = updateTableAction { input: (File, String, String, String) =>
            val (upfile, tableName, fileType, apikey) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.updateTable
            val success = DashboardRegistry.dashboardService.update(upfile,tableName, fileType)
            UpdateTable200(success)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.updateTable
        }
        val getckandatasetbyid = getckandatasetbyidAction { (dataset_id: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.getckandatasetbyid
            val datasetFuture: Future[JsResult[Dataset]] = CkanRegistry.ckanService.testDataset(dataset_id)
            val eitherDataset: Future[Either[String, Dataset]] = datasetFuture.map(result => {
                result match {
                    case s: JsSuccess[Dataset] => Right(s.get)
                    case e: JsError => Left("error no dataset with that id")
                }
            })

            eitherDataset.flatMap {
                case Right(dataset) => Getckandatasetbyid200(dataset)
                case Left(error) => Getckandatasetbyid401(Error(None,Option(error),None))
            }
           // NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.getckandatasetbyid
        }
        val catalogDistrubutionGroups = catalogDistrubutionGroupsAction { input: (String, String) =>
            val (catalogName, apikey) = input
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.catalogDistrubutionGroups
            val distributions: Seq[Distribution] = ComponentRegistry.monitorService.datasetCatalogGroup(catalogName)
            CatalogDistrubutionGroups200(distributions)
            // ----- End of unmanaged code area for action  Ftd_apiYaml.catalogDistrubutionGroups
        }
        val getckanorganizationList = getckanorganizationListAction {  _ =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.getckanorganizationList
            val eitherOut: Future[Either[Error, Seq[String]]] = CkanRegistry.ckanService.getOrganizations().map(result =>{
                result match {
                    case s: JsArray => Right(s.as[Seq[String]])
                    case _ => Left(GENERIC_ERROR)
                }
            })

            eitherOut.flatMap {
                case Right(list) => GetckanorganizationList200(list)
                case Left(error) => GetckanorganizationList401(error)
            }
            // ----- End of unmanaged code area for action  Ftd_apiYaml.getckanorganizationList
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
    
     // Dead code for absent methodFtd_apiYaml.getmonitorCatalogs
     /*
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.getmonitorCatalogs
            val catalogs = ComponentRegistry.monitorService.allCatalogs()
            GetmonitorCatalogs200(catalogs)
            //   NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.getmonitorCatalogs
     */

    
     // Dead code for absent methodFtd_apiYaml.testauto
     /*
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.testauto
            NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.testauto
     */

    
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
