package it.gov.daf.datipubblici.client

import it.gov.daf.datipubblici._

import it.gov.daf.datipubblici.json._

import play.api.libs.ws._

import play.api.libs.json._

import javax.inject._

import play.api.libs.concurrent.Execution.Implicits._

class Ftd_apiClient @Inject() (WS: WSClient) (baseUrl: String) {
  def createckandataset(Authorization: String, dataset: Dataset) = {
    WS.url(s"$baseUrl/dati-gov/v1/ckan/create/dataset").withHeaders((this._render_header_params("Authorization" -> Some(Authorization)): _*)).post(Json.toJson(dataset)).map({ resp =>
      if ((resp.status >= 200) && (resp.status <= 299)) Json.parse(resp.body).as[Success]
      else throw new java.lang.RuntimeException("unexpected response status: " + resp.status + " " + resp.body.toString)
    })
  }
  def createckanorganization(Authorization: String, organization: Organization) = {
    WS.url(s"$baseUrl/dati-gov/v1/ckan/create/organization").withHeaders((this._render_header_params("Authorization" -> Some(Authorization)): _*)).post(Json.toJson(organization)).map({ resp =>
      if ((resp.status >= 200) && (resp.status <= 299)) Json.parse(resp.body).as[Success]
      else throw new java.lang.RuntimeException("unexpected response status: " + resp.status + " " + resp.body.toString)
    })
  }
  def createckanuser(Authorization: String, user: User) = {
    WS.url(s"$baseUrl/dati-gov/v1/ckan/create/user").withHeaders((this._render_header_params("Authorization" -> Some(Authorization)): _*)).post(Json.toJson(user)).map({ resp =>
      if ((resp.status >= 200) && (resp.status <= 299)) Json.parse(resp.body).as[Success]
      else throw new java.lang.RuntimeException("unexpected response status: " + resp.status + " " + resp.body.toString)
    })
  }
  def getckandatasetList(Authorization: String) = {
    WS.url(s"$baseUrl/dati-gov/v1/ckan/datasets").withHeaders((this._render_header_params("Authorization" -> Some(Authorization)): _*)).get().map({ resp =>
      if ((resp.status >= 200) && (resp.status <= 299)) Json.parse(resp.body).as[List[String]]
      else throw new java.lang.RuntimeException("unexpected response status: " + resp.status + " " + resp.body.toString)
    })
  }
  def getckandatasetbyid(Authorization: String, dataset_id: String) = {
    WS.url(s"$baseUrl/dati-gov/v1/ckan/datasets/$dataset_id").withHeaders((this._render_header_params("Authorization" -> Some(Authorization)): _*)).get().map({ resp =>
      if ((resp.status >= 200) && (resp.status <= 299)) Json.parse(resp.body).as[Dataset]
      else throw new java.lang.RuntimeException("unexpected response status: " + resp.status + " " + resp.body.toString)
    })
  }
  def getckandatasetListWithRes(Authorization: String, limit: Option[Int], offset: Option[Int]) = {
    WS.url(s"$baseUrl/dati-gov/v1/ckan/datasetsWithResources" + this._render_url_params("limit" -> limit, "offset" -> offset)).withHeaders((this._render_header_params("Authorization" -> Some(Authorization)): _*)).get().map({ resp =>
      if ((resp.status >= 200) && (resp.status <= 299)) Json.parse(resp.body).as[List[Dataset]]
      else throw new java.lang.RuntimeException("unexpected response status: " + resp.status + " " + resp.body.toString)
    })
  }
  def testmulti(Authorization: String) = {
    WS.url(s"$baseUrl/dati-gov/v1/ckan/multi").withHeaders((this._render_header_params("Authorization" -> Some(Authorization)): _*)).get().map({ resp =>
      if ((resp.status >= 200) && (resp.status <= 299)) Json.parse(resp.body).as[DatasetTest]
      else throw new java.lang.RuntimeException("unexpected response status: " + resp.status + " " + resp.body.toString)
    })
  }
  def getckanorganizationbyid(Authorization: String, org_id: String) = {
    WS.url(s"$baseUrl/dati-gov/v1/ckan/organization/$org_id").withHeaders((this._render_header_params("Authorization" -> Some(Authorization)): _*)).get().map({ resp =>
      if ((resp.status >= 200) && (resp.status <= 299)) Json.parse(resp.body).as[Organization]
      else throw new java.lang.RuntimeException("unexpected response status: " + resp.status + " " + resp.body.toString)
    })
  }
  def getckanorganizationList(Authorization: String) = {
    WS.url(s"$baseUrl/dati-gov/v1/ckan/organizations").withHeaders((this._render_header_params("Authorization" -> Some(Authorization)): _*)).get().map({ resp =>
      if ((resp.status >= 200) && (resp.status <= 299)) Json.parse(resp.body).as[List[String]]
      else throw new java.lang.RuntimeException("unexpected response status: " + resp.status + " " + resp.body.toString)
    })
  }
  def searchdataset(Authorization: String, q: Option[String], sort: Option[String], rows: Option[Int]) = {
    WS.url(s"$baseUrl/dati-gov/v1/ckan/searchDataset" + this._render_url_params("q" -> q, "sort" -> sort, "rows" -> rows)).withHeaders((this._render_header_params("Authorization" -> Some(Authorization)): _*)).get().map({ resp =>
      if ((resp.status >= 200) && (resp.status <= 299)) Json.parse(resp.body).as[List[Dataset]]
      else throw new java.lang.RuntimeException("unexpected response status: " + resp.status + " " + resp.body.toString)
    })
  }
  def updateckanorganization(Authorization: String, org_id: String, organization: Organization) = {
    WS.url(s"$baseUrl/dati-gov/v1/ckan/update/organization/$org_id").withHeaders((this._render_header_params("Authorization" -> Some(Authorization)): _*)).put(Json.toJson(organization)).map({ resp =>
      if ((resp.status >= 200) && (resp.status <= 299)) Json.parse(resp.body).as[Success]
      else throw new java.lang.RuntimeException("unexpected response status: " + resp.status + " " + resp.body.toString)
    })
  }
  def getckanuser(Authorization: String, username: String) = {
    WS.url(s"$baseUrl/dati-gov/v1/ckan/user/$username").withHeaders((this._render_header_params("Authorization" -> Some(Authorization)): _*)).get().map({ resp =>
      if ((resp.status >= 200) && (resp.status <= 299)) Json.parse(resp.body).as[User]
      else throw new java.lang.RuntimeException("unexpected response status: " + resp.status + " " + resp.body.toString)
    })
  }
  def getckanuserorganizationList(Authorization: String, username: String) = {
    WS.url(s"$baseUrl/dati-gov/v1/ckan/userOrganizations/$username").withHeaders((this._render_header_params("Authorization" -> Some(Authorization)): _*)).get().map({ resp =>
      if ((resp.status >= 200) && (resp.status <= 299)) Json.parse(resp.body).as[List[Organization]]
      else throw new java.lang.RuntimeException("unexpected response status: " + resp.status + " " + resp.body.toString)
    })
  }
  def getckanuserList(Authorization: String) = {
    WS.url(s"$baseUrl/dati-gov/v1/ckan/users").withHeaders((this._render_header_params("Authorization" -> Some(Authorization)): _*)).get().map({ resp =>
      if ((resp.status >= 200) && (resp.status <= 299)) Json.parse(resp.body).as[List[User]]
      else throw new java.lang.RuntimeException("unexpected response status: " + resp.status + " " + resp.body.toString)
    })
  }
  def verifycredentials(Authorization: String, credentials: Credentials) = {
    WS.url(s"$baseUrl/dati-gov/v1/ckan/verifyCredentials").withHeaders((this._render_header_params("Authorization" -> Some(Authorization)): _*)).post(Json.toJson(credentials)).map({ resp =>
      if ((resp.status >= 200) && (resp.status <= 299)) Json.parse(resp.body).as[Success]
      else throw new java.lang.RuntimeException("unexpected response status: " + resp.status + " " + resp.body.toString)
    })
  }
  def createTable(Authorization: String, tableName: String, fileType: String, apikey: String) = {
    WS.url(s"$baseUrl/dati-gov/v1/dashboard/create/$tableName/$fileType" + this._render_url_params("apikey" -> Some(apikey))).withHeaders((this._render_header_params("Authorization" -> Some(Authorization)): _*)).post().map({ resp =>
      if ((resp.status >= 200) && (resp.status <= 299)) Json.parse(resp.body).as[Success]
      else throw new java.lang.RuntimeException("unexpected response status: " + resp.status + " " + resp.body.toString)
    })
  }
  def dashboardIframes(Authorization: String, apikey: String) = {
    WS.url(s"$baseUrl/dati-gov/v1/dashboard/iframes" + this._render_url_params("apikey" -> Some(apikey))).withHeaders((this._render_header_params("Authorization" -> Some(Authorization)): _*)).get().map({ resp =>
      if ((resp.status >= 200) && (resp.status <= 299)) Json.parse(resp.body).as[List[DashboardIframes]]
      else throw new java.lang.RuntimeException("unexpected response status: " + resp.status + " " + resp.body.toString)
    })
  }
  def savedashboard(Authorization: String, user: String, dashboard: String, layout: Dashboard) = {
    WS.url(s"$baseUrl/dati-gov/v1/dashboard/save/$user/$dashboard").withHeaders((this._render_header_params("Authorization" -> Some(Authorization)): _*)).post(Json.toJson(layout)).map({ resp =>
      if ((resp.status >= 200) && (resp.status <= 299)) Json.parse(resp.body).as[List[Success]]
      else throw new java.lang.RuntimeException("unexpected response status: " + resp.status + " " + resp.body.toString)
    })
  }
  def dashboardTables(Authorization: String, apikey: String) = {
    WS.url(s"$baseUrl/dati-gov/v1/dashboard/tables" + this._render_url_params("apikey" -> Some(apikey))).withHeaders((this._render_header_params("Authorization" -> Some(Authorization)): _*)).get().map({ resp =>
      if ((resp.status >= 200) && (resp.status <= 299)) Json.parse(resp.body).as[List[Catalog]]
      else throw new java.lang.RuntimeException("unexpected response status: " + resp.status + " " + resp.body.toString)
    })
  }
  def updateTable(Authorization: String, tableName: String, fileType: String, apikey: String) = {
    WS.url(s"$baseUrl/dati-gov/v1/dashboard/update/$tableName/$fileType" + this._render_url_params("apikey" -> Some(apikey))).withHeaders((this._render_header_params("Authorization" -> Some(Authorization)): _*)).put().map({ resp =>
      if ((resp.status >= 200) && (resp.status <= 299)) Json.parse(resp.body).as[Success]
      else throw new java.lang.RuntimeException("unexpected response status: " + resp.status + " " + resp.body.toString)
    })
  }
  def monitorcatalogs(Authorization: String, apikey: String) = {
    WS.url(s"$baseUrl/dati-gov/v1/monitor/catalogs" + this._render_url_params("apikey" -> Some(apikey))).withHeaders((this._render_header_params("Authorization" -> Some(Authorization)): _*)).get().map({ resp =>
      if ((resp.status >= 200) && (resp.status <= 299)) Json.parse(resp.body).as[List[Catalog]]
      else throw new java.lang.RuntimeException("unexpected response status: " + resp.status + " " + resp.body.toString)
    })
  }
  def catalogBrokenLinks(Authorization: String, catalogName: String, apikey: String) = {
    WS.url(s"$baseUrl/dati-gov/v1/monitor/catalogs/$catalogName/broken_links" + this._render_url_params("apikey" -> Some(apikey))).withHeaders((this._render_header_params("Authorization" -> Some(Authorization)): _*)).get().map({ resp =>
      if ((resp.status >= 200) && (resp.status <= 299)) Json.parse(resp.body).as[List[BrokenLink]]
      else throw new java.lang.RuntimeException("unexpected response status: " + resp.status + " " + resp.body.toString)
    })
  }
  def catalogDatasetCount(Authorization: String, catalogName: String, apikey: String) = {
    WS.url(s"$baseUrl/dati-gov/v1/monitor/catalogs/$catalogName/count" + this._render_url_params("apikey" -> Some(apikey))).withHeaders((this._render_header_params("Authorization" -> Some(Authorization)): _*)).get().map({ resp =>
      if ((resp.status >= 200) && (resp.status <= 299)) Json.parse(resp.body).as[List[Distribution]]
      else throw new java.lang.RuntimeException("unexpected response status: " + resp.status + " " + resp.body.toString)
    })
  }
  def catalogDistrubutionFormat(Authorization: String, catalogName: String, apikey: String) = {
    WS.url(s"$baseUrl/dati-gov/v1/monitor/catalogs/$catalogName/distribution_formats" + this._render_url_params("apikey" -> Some(apikey))).withHeaders((this._render_header_params("Authorization" -> Some(Authorization)): _*)).get().map({ resp =>
      if ((resp.status >= 200) && (resp.status <= 299)) Json.parse(resp.body).as[List[Distribution]]
      else throw new java.lang.RuntimeException("unexpected response status: " + resp.status + " " + resp.body.toString)
    })
  }
  def catalogDistrubutionGroups(Authorization: String, catalogName: String, apikey: String) = {
    WS.url(s"$baseUrl/dati-gov/v1/monitor/catalogs/$catalogName/distribution_groups" + this._render_url_params("apikey" -> Some(apikey))).withHeaders((this._render_header_params("Authorization" -> Some(Authorization)): _*)).get().map({ resp =>
      if ((resp.status >= 200) && (resp.status <= 299)) Json.parse(resp.body).as[List[Distribution]]
      else throw new java.lang.RuntimeException("unexpected response status: " + resp.status + " " + resp.body.toString)
    })
  }
  def catalogDistributionLicense(Authorization: String, catalogName: String, apikey: String) = {
    WS.url(s"$baseUrl/dati-gov/v1/monitor/catalogs/$catalogName/distribution_licenses" + this._render_url_params("apikey" -> Some(apikey))).withHeaders((this._render_header_params("Authorization" -> Some(Authorization)): _*)).get().map({ resp =>
      if ((resp.status >= 200) && (resp.status <= 299)) Json.parse(resp.body).as[List[Distribution]]
      else throw new java.lang.RuntimeException("unexpected response status: " + resp.status + " " + resp.body.toString)
    })
  }
  def allBrokenLinks(Authorization: String, apikey: String) = {
    WS.url(s"$baseUrl/dati-gov/v1/monitor/dati_gov/broken_links" + this._render_url_params("apikey" -> Some(apikey))).withHeaders((this._render_header_params("Authorization" -> Some(Authorization)): _*)).get().map({ resp =>
      if ((resp.status >= 200) && (resp.status <= 299)) Json.parse(resp.body).as[List[BrokenLink]]
      else throw new java.lang.RuntimeException("unexpected response status: " + resp.status + " " + resp.body.toString)
    })
  }
  def allDatasets(Authorization: String, apikey: String) = {
    WS.url(s"$baseUrl/dati-gov/v1/monitor/dati_gov/count" + this._render_url_params("apikey" -> Some(apikey))).withHeaders((this._render_header_params("Authorization" -> Some(Authorization)): _*)).get().map({ resp =>
      if ((resp.status >= 200) && (resp.status <= 299)) Json.parse(resp.body).as[List[Distribution]]
      else throw new java.lang.RuntimeException("unexpected response status: " + resp.status + " " + resp.body.toString)
    })
  }
  def allDistributionFormats(Authorization: String, apikey: String) = {
    WS.url(s"$baseUrl/dati-gov/v1/monitor/dati_gov/distribution_formats" + this._render_url_params("apikey" -> Some(apikey))).withHeaders((this._render_header_params("Authorization" -> Some(Authorization)): _*)).get().map({ resp =>
      if ((resp.status >= 200) && (resp.status <= 299)) Json.parse(resp.body).as[List[Distribution]]
      else throw new java.lang.RuntimeException("unexpected response status: " + resp.status + " " + resp.body.toString)
    })
  }
  def allDistributionGroups(Authorization: String, apikey: String) = {
    WS.url(s"$baseUrl/dati-gov/v1/monitor/dati_gov/distribution_groups" + this._render_url_params("apikey" -> Some(apikey))).withHeaders((this._render_header_params("Authorization" -> Some(Authorization)): _*)).get().map({ resp =>
      if ((resp.status >= 200) && (resp.status <= 299)) Json.parse(resp.body).as[List[Distribution]]
      else throw new java.lang.RuntimeException("unexpected response status: " + resp.status + " " + resp.body.toString)
    })
  }
  def allDistributionLiceses(Authorization: String, apikey: String) = {
    WS.url(s"$baseUrl/dati-gov/v1/monitor/dati_gov/distribution_licenses" + this._render_url_params("apikey" -> Some(apikey))).withHeaders((this._render_header_params("Authorization" -> Some(Authorization)): _*)).get().map({ resp =>
      if ((resp.status >= 200) && (resp.status <= 299)) Json.parse(resp.body).as[List[Distribution]]
      else throw new java.lang.RuntimeException("unexpected response status: " + resp.status + " " + resp.body.toString)
    })
  }
  private def _render_url_params(pairs: (String, Option[Any])*) = {
    val parts = pairs.collect({
      case (k, Some(v)) => k + "=" + v
    })
    if (parts.nonEmpty) parts.mkString("?", "&", "")
    else ""
  }
  private def _render_header_params(pairs: (String, Option[Any])*) = {
    pairs.collect({
      case (k, Some(v)) => k -> v.toString
    })
  }
}