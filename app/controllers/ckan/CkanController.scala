package controllers.ckan

/**
  * Created by ale on 04/04/17.
  */


import javax.inject._

import play.api.mvc._
import play.api.libs.ws._

import scala.concurrent.Future
import play.api.libs.json._
import play.api.inject.ConfigurationProvider
import services.ComponentRegistry
import services.ckan.CkanRegistry




@Singleton
class CkanController @Inject() (ws: WSClient, config: ConfigurationProvider) extends Controller {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  private val CKAN_URL :String = config.get.getString("app.ckan.url").get

  private val LOCAL_URL :String = config.get.getString("app.local.url").get

  private val ENV:String = config.get.getString("app.type").get


  private def getOrgs(orgId :String): Future[List[String]] = {
      val orgs : Future[WSResponse] = ws.url(LOCAL_URL + "/ckan/organizations/" + orgId).get()
      orgs.map( item => {
          val messages  = (item.json \ "result" \\ "message").map(_.as[String]).toList
          val datasetIds :List[String]= messages.filterNot(_.isEmpty) map { message =>
            println(message)
            val datasetId = message.split("object")(1).trim
            println(datasetId)
            datasetId
          }
           datasetIds
      })
  }

  private def getOrgDatasets(datasetIds : List[String]): Future[Seq[JsValue]] = {
    val datasetResponses: Seq[Future[JsValue]] = datasetIds.map(datasetId => {
         val response = ws.url(LOCAL_URL + "/ckan/dataset/" + datasetId).get
         println(datasetId)
         response map { x =>
           println(x.json.toString)
           (x.json \ "result").getOrElse(JsNull)
         }
    })
    val datasetFuture :Future[Seq[JsValue]] = Future.sequence(datasetResponses)
    datasetFuture
  }



  def createDataset = Action.async { implicit request =>

    /*
    curl -H "Content-Type: application/json" -X POST -d @data.json http://localhost:9000/ckan/createDataset  dove data json
    contiene:

    {
      "creator_name": "test",
      "maintainer": "",
      "encoding": "UTF-8",
      "issued": "2017-06-01",
      "temporal_start": "2017-06-01",
      "private": false,
      "creation_date": "2017-06-01",
      "num_tags": 1,
      "frequency": "BIENNIAL",
      "publisher_name": "test",
      "metadata_created": "2017-06-01T12:55:19.951016",
      "creator_identifier": "12122",
      "conforms_to": "Regolamento (UE) n. 1089/2010 della Commissione del 23 novembre 2010 recante attuazione della direttiva 2007/2/CE del Parlamento europeo e del Consiglio per quanto riguarda l'interoperabilit\u00e0 dei set di dati territoriali e dei servizi di dati territoriali",
      "metadata_modified": "2017-06-01T12:57:13.553832",
      "author": "",
      "author_email": "",
      "geographical_geonames_url": "http://www.geonames.org/3175395",
      "theme": "ECON",
      "site_url": "http://192.168.1.124",
      "state": "active",
      "version": "",
      "alternate_identifier": "ISBN,TEST",
      "relationships_as_object": [],
      "creator_user_id": "db368512-e344-4711-a428-f4615287ef7c",
      "type": "dataset",
      "ksdlkn": "vvvv",
      "resources": [{
          "cache_last_updated": null,
          "package_id": "8a1a9892-2cab-4454-8bec-d68517580cc5",
          "distribution_format": "XML",
          "webstore_last_updated": null,
          "datastore_active": false,
          "id": "5a15f32b-705f-4d3a-9caa-07525307ff64",
          "size": null,
          "state": "active",
          "hash": "",
          "description": "rete viaria rete ciclabile",
          "format": "XML",
          "mimetype_inner": null,
          "url_type": null,
          "mimetype": null,
          "cache_url": null,
          "name": "rete viaria rete ciclabile",
          "created": "2017-06-01T14:57:07.665875",
          "url": "http://geoservices.buergernetz.bz.it/geoserver/p_bz-transport_network/ows?SERVICE=WMS",
          "webstore_url": null,
          "last_modified": null,
          "position": 0,
          "revision_id": "88fe8aee-1540-4038-80bc-47677640d729",
          "resource_type": null
        }
      ],
      "holder_name": "test",
      "tags": [{
          "vocabulary_id": null,
          "state": "active",
          "display_name": "test",
          "id": "6f19cf3e-0c6a-497d-919c-7953bf64c06e",
          "name": "test"
        }
      ],
      "holder_identifier": "121212",
      "fields_description": "",
      "groups": [],
      "license_id": "other-nc",
      "temporal_end": "2017-06-16",
      "maintainer_email": "",
      "relationships_as_subject": [],
      "is_version_of": "http://dati.retecivica.bz.it/it/dataset/rete-viaria-rete-ciclabile",
      "license_title": "Altro (Non Commerciale)",
      "num_resources": 1,
      "name": "test-dcatapit-api-4",
      "language": "{ENG,ITA}",
      "url": "",
      "isopen": false,
      "owner_org": "mytestorg",
      "modified": "2017-06-01",
      "publisher_identifier": "23232",
      "geographical_name": "ITA_BZO",
      "contact": "",
      "title": "test dcatapit api 2",
      "revision_id": "88fe8aee-1540-4038-80bc-47677640d729",
      "identifier": "12121212",
      "notes": "test description dcatapit api"
    }

    */

    val AUTH_TOKEN:String = config.get.getString("app.ckan.auth.token").get

    val json:JsValue = request.body.asJson.get

    if(ENV == "dev"){
      CkanRegistry.ckanService.createDataset(json)

      val isOk = Future.successful(JsString("operazione effettuata correttamente"))
      isOk map { x =>
        Ok(x)
      }
    }else{
      val response = ws.url(CKAN_URL + "/api/3/action/package_create").withHeaders("Authorization" -> AUTH_TOKEN).post(json)

      response map { x =>
        println(x.json.toString)
        Ok(x.json)
      }
    }
  }

  def createOrganization = Action.async { implicit request =>

    /*
    curl -H "Content-Type: application/json" -X POST -d @org.json http://localhost:9000/ckan/createOrganization dove org.json contiene
    {
      "description": "Azienda per il Turismo Altopiano di Pin\u00e9 e Valle di Cembra. Maggiori informazioni sul loro [sito web](http://www.visitpinecembra.it)",
      "created": "2014-04-04T12:27:29.698895",
      "title": "APT Altopiano di Pin\u00e8 e Valle di Cembra it",
      "name": "apt-altopiano-di-pine-e-valle-di-cembra2",
      "is_organization": true,
      "state": "active",
      "image_url": "http://dati.trentino.it/images/logo.png",
      "revision_id": "8d453086-2b89-4d9c-8bc2-a86b91978021",
      "type": "organization",
      "id": "232cad97-ecf2-447d-9656-63899023887t",
      "approval_status": "approved"
    }
    * */

    val AUTH_TOKEN:String = config.get.getString("app.ckan.auth.token").get
    val json:JsValue = request.body.asJson.get

    val response = ws.url(CKAN_URL + "/api/3/action/organization_create").withHeaders("Authorization" -> AUTH_TOKEN).post(json)

    response map { x =>
      println(x.json.toString)
      Ok(x.json)
    }

  }

  // added
  def getDatasetList = Action.async { implicit request =>
    val test = ws.url(CKAN_URL + "/api/3/action/package_list").get
    test map { response =>
      Ok(response.json)
    }
  }


  def getOrganizationList = Action.async { implicit request =>
    val test = ws.url(CKAN_URL + "/api/3/action/organization_list").get
    test map { response =>
     // val bodyResponse :String = response.body
      Ok(response.json)
    }
  }

  //added
  def searchDataset(q :String) = Action.async { implicit request =>

    val url = CKAN_URL + "/api/3/action/package_search?q=" + q
    val test = ws.url(url).get
    test map { response =>
      Ok(response.json)
    }
  }


  def getOganizationRevisionList(organizationId :String) = Action.async { implicit request =>
    val url = CKAN_URL + "/api/3/action/organization_revision_list?id=" + organizationId
    val test = ws.url(url).get
    test map { response =>
      // val bodyResponse :String = response.body
      Ok(response.json)
    }
  }

  def getDataset(datasetId :String) = Action.async { implicit request =>

    if(ENV == "dev"){
      val dataset = CkanRegistry.ckanService.dataset(datasetId)

      val isOk = Future.successful(dataset)
      isOk map { x =>
        Ok(x)
      }

    }else{
      val url = CKAN_URL + "/api/3/action/package_show?id=" + datasetId
      println("URL " + url)
      val test = ws.url(url).get
      test map { response =>
        // val bodyResponse :String = response.body
        Ok(response.json)
      }
    }

  }

  def getOrganizationDataset(organizationId :String) = Action.async { implicit request =>
    def isNull(v: JsValue) = v match {
      case JsNull => true
      case _ => false
    }
    for {
      datasets <- getOrgs(organizationId)
      dataset: Seq[JsValue] <- getOrgDatasets(datasets)
    } yield {
       Ok(Json.obj("result" -> Json.toJson(dataset.filterNot(isNull(_))), "success" -> JsBoolean(true)))
    }
  }

}