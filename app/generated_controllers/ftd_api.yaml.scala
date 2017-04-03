
import play.api.mvc.{Action, Controller}

import play.api.data.validation.Constraint

import play.api.inject.{ApplicationLifecycle,ConfigurationProvider}

import de.zalando.play.controllers._

import PlayBodyParsing._

import PlayValidations._

import scala.util._

import javax.inject._

import play.api.libs.json._
import     play.api.libs.json.JsNumber

/**
 * This controller is re-generated after each change in the specification.
 * Please only place your hand-written code between appropriate comments in the body of the controller.
 */

package ftd_api.yaml {

    class Ftd_apiYaml @Inject() (lifecycle: ApplicationLifecycle, config: ConfigurationProvider) extends Ftd_apiYamlBase {
        // ----- Start of unmanaged code area for constructor Ftd_apiYaml

        // ----- End of unmanaged code area for constructor Ftd_apiYaml
        val catalogDistributionLicense = catalogDistributionLicenseAction { (catalogName: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.catalogDistributionLicense
            NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.catalogDistributionLicense
        }
        val catalogDatasetCount = catalogDatasetCountAction { (catalogName: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.catalogDatasetCount
            NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.catalogDatasetCount
        }
        val allDistributionLiceses = allDistributionLicesesAction {  _ =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.allDistributionLiceses
            NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.allDistributionLiceses
        }
        val catalogDistrubutionGroups = catalogDistrubutionGroupsAction { (catalogName: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.catalogDistrubutionGroups
            NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.catalogDistrubutionGroups
        }
        val allDistributionGroups = allDistributionGroupsAction {  _ =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.allDistributionGroups
            NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.allDistributionGroups
        }
        val allDatasets = allDatasetsAction {  _ =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.allDatasets
            NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.allDatasets
        }
        val getcatalogs = getcatalogsAction {  _ =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.getcatalogs
            println("Ciao bella ...")
            Getcatalogs200(Seq(Catalog(Option("ale")),Catalog(Option("ciao"))))
         //   NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.getcatalogs
        }
        val allDistributionFormats = allDistributionFormatsAction {  _ =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.allDistributionFormats
            NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.allDistributionFormats
        }
        val allBrokenLinks = allBrokenLinksAction {  _ =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.allBrokenLinks
            NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.allBrokenLinks
        }
        val catalogDistrubutionFormat = catalogDistrubutionFormatAction { (catalogName: String) =>  
            // ----- Start of unmanaged code area for action  Ftd_apiYaml.catalogDistrubutionFormat
            NotImplementedYet
            // ----- End of unmanaged code area for action  Ftd_apiYaml.catalogDistrubutionFormat
        }
    
    }
}
