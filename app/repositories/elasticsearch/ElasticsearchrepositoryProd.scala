package repositories.elasticsearch

import com.sksamuel.elastic4s.http.ElasticDsl.{boolQuery, existsQuery, highlight, matchQuery, missingAgg, must, search, should, termQuery, termsAgg, termsQuery}
import com.sksamuel.elastic4s.searches.queries.{BoolQueryDefinition, QueryDefinition}
import ftd_api.yaml.{Dashboard, Datastory, Filters, SearchResult, UserStory}
import play.api.Logger
import play.api.libs.json.{JsLookupResult, JsValue, Json}
import utils.ConfigReader
import com.sksamuel.elastic4s.http.search.{SearchHit, SearchResponse}
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.searches.SearchDefinition
import org.elasticsearch.index.query.Operator

import scala.concurrent.Future
import scala.util.Try
import scala.concurrent.ExecutionContext.Implicits._

class ElasticsearchrepositoryProd extends ElasticsearchRepository {

  private val elasticsearchUrl = ConfigReader.getElasticsearchUrl
  private val elasticsearchPort = ConfigReader.getElasticsearchPort
  private val elasticsearchMaxResult = ConfigReader.getElastcsearchMaxResult

  def searchText(filters: Filters, username: String, groups: List[String], limit: Option[Int]): Future[List[SearchResult]] = {
    Logger.logger.debug(s"elasticsearchUrl: $elasticsearchUrl elasticsearchPort: $elasticsearchPort")

    val client = HttpClient(ElasticsearchClientUri(elasticsearchUrl, elasticsearchPort))
    val index = "ckan"
    val fieldDatasetDcatName = "dcatapit.name"
    val fieldDatasetDcatTitle = "dcatapit.title"
    val fieldDatasetDcatNote = "dcatapit.notes"
    val fieldDatasetDcatTheme = "dcatapit.theme"
    val fieldDatasetDataFieldName = "dataschema.avro.fields.name"
    val fieldDsTitle = "title"
    val fieldDsSub = "subtitle"
    val fieldDsWdgetTitle = "widgets.title"
    val fieldDsWdgetText = "widgets.text"
    val fieldDataset = List(fieldDatasetDcatName, fieldDatasetDcatTitle, fieldDatasetDcatNote,
      fieldDatasetDataFieldName, fieldDatasetDcatTheme, "dcatapit.privatex", "dcatapit.modified", "dcatapit.owner_org", "operational.input_src")
    val fieldsOpenData = List("name", "title", "notes", "organization.name", "theme", "modified")
    val fieldDatastory: List[String] = listFields("Datastory")
    val fieldToReturn: List[String] = fieldDataset ++ fieldDatastory ++ fieldsOpenData

    val listFieldSearch: List[String] = List(fieldDatasetDcatName, fieldDatasetDcatTitle, fieldDatasetDcatNote, fieldDatasetDcatTheme,
      fieldDatasetDataFieldName, fieldDsTitle, fieldDsSub, fieldDsWdgetTitle, fieldDsWdgetText, "dcatapit.owner_org", "org") ::: fieldsOpenData

    val searchText: Boolean = filters.text match {
      case Some("") => false
      case Some(_) => true
      case None => false
    }

    val searchType: String = filters.index match {
      case Some(List()) => ""
      case Some(x) => x.mkString(",")
      case None => ""
    }

    val order: String = filters.order match {
      case Some(o) => o
      case _ => "desc"
    }

    val limitParam: Int = limit match {
      case Some(0) => elasticsearchMaxResult
      case Some(x)  => x
      case None     => 1000
    }

    def queryElasticsearch(limitResult: Int, searchTypeInQuery: String): SearchDefinition = {
      search(index).types(searchTypeInQuery).query(
        boolQuery()
          must(
          should(
            textStringQuery(filters.text, listFieldSearch.filterNot(f => f.equals(fieldDatasetDcatTheme) || f.equals("theme")))
          ),
          must(
            createFilterOrg(filters.org) ::: createFilterStatus(filters.status) :::
              createThemeFilter(filters.theme) ::: ownerQueryString(filters.owner) ::: sharedWithMe(filters.sharedWithMe, groups)
          ),
          should(
            should(
              must(termQuery("dcatapit.privatex", true), matchQuery("operational.acl.groupName", groups.mkString(" ")).operator("OR")),
              must(termQuery("dcatapit.privatex", true), termQuery("dcatapit.author", username))
            ),
            termQuery("dcatapit.privatex", false),
            must(termQuery("status", 0), termQuery("user", username)),
            must(termQuery("status", 1), matchQuery("org", groups.mkString(" ")).operator("OR")),
            termQuery("status", 2),
            termQuery("private", false)
          )
        )
      ).limit(limitResult)
    }

    val query = queryElasticsearch(limitParam, searchType).sourceInclude(fieldToReturn)
      .aggregations(
        termsAgg("type", "_type"),
        termsAgg("status_ds", "status"),
        termsAgg("status_cat", "dcatapit.privatex"),
        termsAgg("status_ext", "private"),
        termsAgg("org_ds", "org.keyword"),
        termsAgg("org_cat", "dcatapit.owner_org.keyword").size(1000),
        termsAgg("org_ext", "organization.name.keyword").size(1000),
        termsAgg("cat_cat", "dcatapit.theme.keyword").size(1000),
        termsAgg("cat_ext", "theme.keyword").size(1000)
      )
      .highlighting(listFieldSearch
        .filterNot(s => s.equals("org") || s.equals("dcatapit.owner_org"))
        .map(x => highlight(x).preTag("<span style='background-color:#0BD9D3'>").postTag("</span>")
          .fragmentSize(70))
      )

    val queryAggregationNoCat: SearchDefinition = queryElasticsearch(0, "ext_opendata")
      .fetchSource(false)
      .aggregations(
        missingAgg("no_category", "theme.keyword")
      )

    val responseFutureQuery: Future[SearchResponse] = client.execute(query)
    val futureSearchResults: Future[List[SearchResult]] = wrapResponse(responseFutureQuery, order, searchText, filters.date)

    val futureMapAggregation: Future[Map[String, AnyRef]] = responseFutureQuery.map(r => r.aggregations)

    val responseFutureQueryNoCat: Option[Future[Map[String, AnyRef]]] = searchType match {
      case "" => Some(client.execute(queryAggregationNoCat).map(q => q.aggregations))
      case "ext_opendata" => Some(client.execute(queryAggregationNoCat).map(q => q.aggregations))
      case _ => None
    }

    val futureAggregationResults: Future[List[SearchResult]] = createAggregationResponse(futureMapAggregation, responseFutureQueryNoCat)

    val response: Future[List[SearchResult]] = for{
      searchResult <- futureSearchResults
      aggregationResult <- futureAggregationResults
    } yield searchResult ::: aggregationResult

    response onComplete{ _ => client.close()}

    response
  }

  def searchTextPublic(filters: Filters, limit: Option[Int]): Future[List[SearchResult]] = {
    Logger.logger.debug(s"elasticsearchUrl: $elasticsearchUrl elasticsearchPort: $elasticsearchPort")

    val client = HttpClient(ElasticsearchClientUri(elasticsearchUrl, elasticsearchPort))
    val index = "ckan"
    val fieldDatasetDcatName = "dcatapit.name"
    val fieldDatasetDcatTitle = "dcatapit.title"
    val fieldDatasetDcatNote = "dcatapit.notes"
    val fieldDatasetDcatTheme = "dcatapit.theme"
    val fieldDatasetDataFieldName = "dataschema.avro.fields.name"
    val fieldUsDsTitle = "title"
    val fieldUsDsSub = "subtitle"
    val fieldDsWdgetTitle = "widgets.title"
    val fieldDsWdgetText = "widgets.text"
    val fieldDataset = List(fieldDatasetDcatName, fieldDatasetDcatTitle, fieldDatasetDcatNote,
      fieldDatasetDataFieldName, fieldDatasetDcatTheme, "dcatapit.privatex", "dcatapit.modified", "dcatapit.owner_org", "operational.input_src")
    val fieldsOpenData = List("name", "title", "notes", "organization.name", "theme", "modified")
    val fieldDatastory: List[String] = listFields("Datastory")
    val fieldToReturn = fieldDataset ++ fieldDatastory ++ fieldsOpenData

    val listFieldSearch = List(fieldDatasetDcatName, fieldDatasetDcatTitle, fieldDatasetDcatNote, fieldDatasetDcatTheme,
      fieldDatasetDataFieldName, fieldUsDsTitle, fieldUsDsSub, fieldDsWdgetTitle, fieldDsWdgetText, "dcatapit.owner_org", "org") ::: fieldsOpenData

    val searchText = filters.text match {
      case Some("") => false
      case Some(_) => true
      case None => false
    }

    val searchType = filters.index match {
      case Some(List()) => ""
      case Some(x) => x.mkString(",")
      case None => ""
    }

    val order: String = filters.order match {
      case Some(o) => o
      case _ => "desc"
    }

    val limitParam = limit match {
      case Some(0) => elasticsearchMaxResult
      case Some(x)  => x
      case None     => 1000
    }

    def queryElasticsearch(limitResult: Int, searchTypeInQuery: String) = {
      search(index).types(searchTypeInQuery).query(
        boolQuery()
          .must(
            should(
              textStringQuery(filters.text, listFieldSearch.filterNot(f => f.equals(fieldDatasetDcatTheme) || f.equals("theme")))
            ),
            must(
              createFilterOrg(filters.org) ::: createFilterStatus(filters.status) :::
                createThemeFilter(filters.theme)
            ),
            should(
              termQuery("dcatapit.privatex", false),
              termQuery("status", 2),
              termQuery("private", false)
            )
          )
      ).limit(limitResult)
    }

    val query = queryElasticsearch(limitParam, searchType).sourceInclude(fieldToReturn)
      .aggregations(
        termsAgg("type", "_type"),
        termsAgg("status_ds", "status"),
        termsAgg("status_cat", "dcatapit.privatex"),
        termsAgg("status_ext", "private"),
        termsAgg("org_ds", "org.keyword").size(1000),
        termsAgg("org_cat", "dcatapit.owner_org.keyword").size(1000),
        termsAgg("org_ext", "organization.name.keyword").size(1000),
        termsAgg("cat_cat", "dcatapit.theme.keyword").size(1000),
        termsAgg("cat_ext", "theme.keyword").size(1000)
      )
      .highlighting(listFieldSearch
        .filterNot(s => s.equals("org") || s.equals("dcatapit.owner_org"))
        .map(x => highlight(x).preTag("<span style='background-color:#0BD9D3'>").postTag("</span>")
          .fragmentSize(70))
      )

    val queryAggregationNoCat = queryElasticsearch(0, "ext_opendata")
      .fetchSource(false)
      .aggregations(
        missingAgg("no_category", "theme.keyword")
      )

    val responseFutureQuery: Future[SearchResponse] = client.execute(query)
    val futureSearchResults = wrapResponse(responseFutureQuery, order, searchText, filters.date)

    val futureMapAggregation: Future[Map[String, AnyRef]] = responseFutureQuery.map(r => r.aggregations)

    val responseFutureQueryNoCat: Option[Future[Map[String, AnyRef]]] = searchType match {
      case "" => Some(client.execute(queryAggregationNoCat).map(q => q.aggregations))
      case "ext_opendata" => Some(client.execute(queryAggregationNoCat).map(q => q.aggregations))
      case _ => None
    }


    val futureAggregationResults = createAggregationResponse(futureMapAggregation, responseFutureQueryNoCat)

    val response = for{
      searchResult <- futureSearchResults
      aggregationResult <- futureAggregationResults
    } yield searchResult ::: aggregationResult

    response onComplete{ _ => client.close()}

    response
  }

  private def textStringQuery(text: Option[String], listFields: List[String]) = {
    def setOperator(str: String) = {
      if(str.charAt(0) == '\"' && str.last == '\"') "AND"
      else "OR"
    }
    text match {
      case Some("") => List()
      case Some(searchString) => {
        listFields
          .map(field =>
            matchQuery(field, searchString)
              .operator(setOperator(searchString))) ::: themeQueryString(searchString)
      }
      case None => List()
    }
  }

  private def createAggregationResponse(resAggr: Future[Map[String, AnyRef]], responseNoCat: Option[Future[Map[String, AnyRef]]]) = {
    val  futureMapAggr: Future[Map[String, Map[String, Int]]] = resAggr
      .map(mapAggr =>
        mapAggr
          .map{ elem =>
            val name = elem._1
            val valueMap = elem._2.asInstanceOf[Map[String, Any]]("buckets").asInstanceOf[List[Map[String, AnyVal]]]
              .map(elemMap =>
                wrapAggrResp(elemMap.values.toList)
              ).map(v => v._1 -> v._2).toMap
            name -> valueMap
          }
      )

    val futureMapNoCat: Future[Map[String, Int]] = responseNoCat match {
      case Some(futureRespNoCat) => futureRespNoCat.map(mapNoCat => mapNoCat.map(elem => (elem._1, elem._2.asInstanceOf[Map[String, Int]]("doc_count"))))
      case None => Future(Map[String, Int]())
    }

    val futureRespAggr = for{
      futureThemeAggr <- parseAggrTheme(futureMapAggr, futureMapNoCat)
      futureStatusAggr <- parseAggrStatus(futureMapAggr)
      futureOrgAggr <- parseAggrOrg(futureMapAggr)
      futureTypeAggr <- parseAggrType(futureMapAggr)
    }yield List(futureTypeAggr, futureStatusAggr, futureOrgAggr, futureThemeAggr)

    futureRespAggr
  }

  private def parseAggrType(futureMapAggr: Future[Map[String, Map[String, Int]]]) = {
    futureMapAggr.map(mapAggr => SearchResult(Some("type"), Some("{" + mergeAggr(mapAggr("type").toList).mkString(",") + "}"), None))
  }

  private def parseAggrOrg(futureMapAggr: Future[Map[String, Map[String, Int]]]) = {
    val futureListAggrOrg = for{
      orgCat <- futureMapAggr.map(mapAggr => mapAggr("org_cat").toList)
      orgExt <- futureMapAggr.map(mapAggr => mapAggr("org_ext").toList)
      orgStDash <- futureMapAggr.map(mapAggr => mapAggr("org_ds").toList)
    } yield orgCat ::: orgExt ::: orgStDash

    futureListAggrOrg.map(listAggrOrg => SearchResult(Some("organization"), Some("{" + mergeAggr(listAggrOrg).mkString(",") + "}"), None))
  }

  private def parseAggrStatus(futureMapAggr: Future[Map[String, Map[String, Int]]]): Future[SearchResult] = {
    def convertAggrDataset(mapStatus: Map[String, Int]) = {
      val countTrue: Int = mapStatus.getOrElse("true", 0)
      val countFalse: Int = mapStatus.getOrElse("false", 0)
      Map("0" -> countTrue, "1" -> countTrue, "2" -> countFalse)
    }

    val futureListStatus: Future[List[(String, Int)]] = for {
      aggrCat <- futureMapAggr.map(mapAggr => convertAggrDataset(mapAggr("status_cat")).toList)
      aggrExt <- futureMapAggr.map(mapAggr => convertAggrDataset(mapAggr("status_ext")).toList)
      aggrDash <- futureMapAggr.map(mapAggr => mapAggr("status_ds").toList)
    }yield aggrCat ::: aggrExt ::: aggrDash

    futureListStatus.map(listStatus => SearchResult(Some("status"), Some("{" + mergeAggr(listStatus).mkString(",") + "}"), None))
  }

  private def parseAggrTheme(futureMapAggr: Future[Map[String, Map[String, Int]]], futureMapNoCat: Future[Map[String, Int]]): Future[SearchResult] = {
    def extractAggregationTheme(name: String, count: Int): List[(String, Int)] = {
      if(name.contains("theme")){
        val themes: String = (Json.parse(name) \\ "theme").repr.map(theme => theme.toString().replace("\"", "")).mkString(",")
        themes.split(",").map(t => (t, count)).toList
      }
      else List((name, count))
    }

    val futureListThemeOpen: Future[List[(String, Int)]] = futureMapAggr.map(mapAggr => mapAggr("cat_cat").toList.flatMap(elem => extractAggregationTheme(elem._1, elem._2)))
    val futureCountNoThemeOpen: Future[Int] = futureListThemeOpen.map(listThemeOpen => listThemeOpen.filter(elem => elem._1.equals("[]") || elem._1.equals("")).map(elem => elem._2).sum)
    val futureListNoTheme: Future[List[(String, Int)]] = for{
      mapThemeNoCat <- futureMapNoCat
      countNoThemeOpen <- futureCountNoThemeOpen
    }  yield List(("no_category", mapThemeNoCat.values.sum + countNoThemeOpen))

    val futureListThemes: Future[List[(String, Int)]] = for {
      mapThemeDaf <- futureMapAggr.map(mapAggr => mapAggr("cat_cat").toList)
      listNoTheme <- futureListNoTheme
      listThemeOpen <- futureListThemeOpen.map(listThemeExt => listThemeExt.filterNot(elem => elem._1.equals("[]") || elem._1.equals("")))
    }yield listThemeOpen ::: mapThemeDaf ::: listNoTheme

    futureListThemes.map(listThemes => SearchResult(Some("category"), Some("{" + mergeAggr(listThemes.filterNot(elem => elem._2 == 0)).mkString(",") + "}"), None))
  }

  private def mergeAggr(listAggr: List[(String, Int)]) = {
    listAggr.groupBy(_._1).toList.sortBy(_._1).map{case (k, v) => s""""$k": "${v.map(_._2).sum}""""}
  }

  private def themeQueryString(search: String): List[QueryDefinition] = {
    val mapTheme: Map[String, String] = Map(
      "agricoltura" -> "AGRI",
      "economia" -> "ECON",
      "educazione" -> "EDUC",
      "energia" -> "ENER",
      "ambiente" -> "ENVI",
      "governo" -> "GOVE",
      "sanita" -> "HEAL",
      "internazionale" -> "INTR",
      "giustizia" -> "JUST",
      "regione" -> "REGI",
      "societa" -> "SOCI",
      "tecnologia" -> "TECH",
      "trasporto" -> "TRAN"
    )

    val searchTheme: List[String] = search.split(" ").map(s =>
      mapTheme.getOrElse(s, "")
    ).toList.filterNot(s => s.equals(""))

    if(searchTheme.nonEmpty)
      searchTheme.flatMap(s => List(termQuery("dcatapit.theme", s), matchQuery("theme", s)))
    else
      List()
  }

  private def createFilterStatus(status: Option[Seq[String]]): List[BoolQueryDefinition] = {
    status match {
      case Some(List()) => List()
      case Some(x) => {
        val statusDataset: Seq[Boolean] = x.map {
          case "2" => false
          case _ => true
        }
        List(should(
          statusDataset.flatMap(s => List(termsQuery("dcatapit.privatex", s), termQuery("private", s))).toList :::
            x.flatMap(s => List(termsQuery("status", s))).toList)
        )
      }
      case _ => List()
    }
  }

  private def createFilterOrg(inputOrgFilter: Option[Seq[String]]) = {
    val datasetOrg = "dcatapit.owner_org"
    val dashNstorOrg = "org"
    val opendataOrg = "organization.name.keyword"

    inputOrgFilter match {
      case Some(Seq()) => List()
      case Some(org) => {
        List(
          should(
            org.flatMap(nameOrg =>
              List(
                matchQuery(datasetOrg, nameOrg).operator("AND"),
                matchQuery(dashNstorOrg, nameOrg).operator("AND"),
                matchQuery(opendataOrg, nameOrg).operator("AND")
              )
            )
          )
        )
      }
      case _ => List()
    }
  }

  private def ownerQueryString(owner: Option[String]): List[BoolQueryDefinition] = {
    owner match {
      case Some("") => List()
      case Some(ownerName) =>
        List(should(
          must(termQuery("dcatapit.author", ownerName)),
          must(termQuery("user", ownerName))
        ))
      case None => List()

    }
  }

  private def sharedWithMe(sharedWithMeFilter: Option[Boolean], groups: List[String]) ={
    sharedWithMeFilter match {
      case Some(true) => {
        List(should(
          must(matchQuery("operational.acl.groupName", groups.mkString(" ")).operator("OR"))
        )
        )
      }
      case _ => List()
    }
  }

  private def createThemeFilter(theme: Option[Seq[String]]): List[BoolQueryDefinition] = {
    val result: List[BoolQueryDefinition] = theme match {
      case Some(Seq()) => List()
      case Some(t) => t.map(s =>
        if(s.equals("no_category")){
          should(
            must(matchQuery("_type", "ext_opendata"), boolQuery().not(existsQuery("theme.keyword"))),
            matchQuery("theme.keyword", ""),
            matchQuery("theme.keyword", "[]")
          )
        }
        else should(matchQuery("theme", s).operator(Operator.AND), matchQuery("dcatapit.theme.keyword", s).operator(Operator.AND))
      ).toList
      case _ => List()
    }
    List(should(result))
  }

  private def filterDate(futureTupleSearchResults: Future[List[(String, SearchResult)]], timestamp: String): Future[List[(String, SearchResult)]] = {
    val start: String = timestamp.split(" ")(0)
    val end: String = timestamp.split(" ")(1)
    futureTupleSearchResults.map(tupleSearchResults => tupleSearchResults.filter(result => (result._1 >= start) && (result._1 <= end)))
  }

  private def themeFormatter(json: JsValue): String = {
    val themeJson: String = (json \ "theme").getOrElse(Json.parse("no_category")).toString()
    val themeString: String = if(themeJson.contains("theme")){
      val listThemes: Array[String] = themeJson.toString.replace("\"[", "").replace("]\"", "").replace("},", "} # ").split(" # ")
      listThemes.map(elem => (Json.parse(elem.trim.replace("\\", "")) \ "theme").get).toList.map(s => s.toString().replace("\"", "")).mkString(",")
    } else themeJson
    if(themeString.equals("null") || themeString.equals("\"[]\"") || themeString.equals("\"\"")) "no_category"
    else themeString
  }

  private def createSearchResult(source: SearchHit, search: Boolean): SearchResult = {
    val sourceResponse: String = source.`type` match {
      case "ext_opendata" =>  {
        val theme: String = themeFormatter(Json.parse(source.sourceAsString))
        val sourceMap: Map[String, AnyRef] = source.sourceAsMap.updated("theme", theme)
        val res: String = sourceMap.map(elem =>
          elem._2 match {
            case map: Map[String, AnyRef] =>
              val value: String = map
                .map(child => s""""${child._1}":"${Try(child._2.toString.replaceAll("\"", "")).getOrElse("")}"""").mkString(",")
              s""""${elem._1}":{$value}"""
            case _ => s""""${elem._1}":"${Try(elem._2.toString.replaceAll("\"", "")).getOrElse("")}""""
          }
        ).mkString(",").replaceAll("\n", "").replaceAll("\r", "").replaceAll("\t", "")
        "{" + res + "}"
      }
      case _ => source.sourceAsString
    }
    val highlight: Some[String] = source.highlight match {
      case mapHighlight: Map[String, Seq[String]] => {
        Some(
          "{" +
            mapHighlight.map(x =>
              x._1 match {
                case "widgets" => s""""${x._1}": "${(Json.parse(source.sourceAsString) \ "widgets").get.toString()}""""
                case _ => s""""${x._1}": "${x._2.mkString("...").replace("\"", "\\\"").replace("\n", "")}""""
              }
            ).mkString(",")
            + "}"
        )
      }
      case _ => Some("{}")
    }

    SearchResult(Some(source.`type`), Some(sourceResponse), highlight)
  }

  private def wrapResponse(query: Future[SearchResponse], order: String, search: Boolean, timestamp: Option[String]): Future[List[SearchResult]] = {
    val futureTupleSearchResult: Future[List[(String, SearchResult)]] = query.map(q =>
      q.hits.hits.map(source =>
        createSearchResult(source, search)
      ).toList.map(searchResult => (extractDate(searchResult.`type`.get, searchResult.source.get), searchResult))
    )

    val res: Future[List[(String, SearchResult)]] = timestamp match {
      case Some("") => futureTupleSearchResult
      case Some(x) => filterDate(futureTupleSearchResult, x)
      case None => futureTupleSearchResult
    }

    val result: Future[List[(String, SearchResult)]] = order match {
      case "score" => res
      case "a-z" | "z-a" => orderByName(res, order)
      case "asc" => res.map(r => r.sortWith(_._1 < _._1))
      case _ => res.map(r => r.sortWith(_._1 > _._1))
    }

    val toReturn: Future[List[SearchResult]] = result.map(r => r.map(elem => elem._2))
    toReturn onComplete (r => Logger.logger.debug(s"found ${r.getOrElse(List()).size}"))
    toReturn
  }

  private def orderByName(futureListSearchResult: Future[List[(String, SearchResult)]], order: String) = {
    futureListSearchResult.map{ listSearchResult =>
      val listTuple: List[(String, String, SearchResult)] = listSearchResult.map{ searchResult =>
        val title: String = searchResult._2.`type` match {
          case Some("catalog_test") => (Json.parse(searchResult._2.source.get) \ "dcatapit" \ "title").get.toString()
          case _ => (Json.parse(searchResult._2.source.get) \ "title").get.toString()
        }
        (title, searchResult._1, searchResult._2)
      }
      order match {
        case "a-z" => listTuple.sortWith(_._1 < _._1).map(t => (t._2, t._3))
        case "z-a" => listTuple.sortWith(_._1 > _._1).map(t => (t._2, t._3))
      }
    }
  }

  private def extractDate(typeDate: String, source: String): String = {
    val result: JsLookupResult = typeDate match {
      case "catalog_test" => Json.parse(source) \ "dcatapit" \ "modified"
      case "ext_opendata" => Json.parse(source.replace("\\", "\\\"")) \ "modified"
      case _ => Json.parse(source) \ "timestamp"
    }
    val date: String = result.getOrElse(Json.parse("23/07/2017")).toString().replaceAll("\"", "") match {
      case "" => "23/07/2017"
      case x: String => x
    }
    parseDate(date)
  }

  private def parseDate(date: String): String = {
    if(date.contains("/")){
      val day: String = date.split("/")(0)
      val month: String = date.split("/")(1)
      val year: String = date.split("/")(2)
      s"""$year-$month-$day"""
    }
    else if(date.contains("T")){
      date.substring(0, date.indexOf("T"))
    }
    else date
  }

  private def wrapAggrResp(listCount: List[AnyVal]): (String, Int) = {
    if(listCount.length == 2) (listCount.head.toString, listCount(1).asInstanceOf[Int])
    else {
      val key: String = listCount(1).toString
      val count: Int = listCount(2).asInstanceOf[Int]
      (key, count)
    }
  }

  private def listFields(obj: String): List[String] = {
    import scala.reflect.runtime.universe
    import scala.reflect.runtime.universe._

    val objType: universe.Type = obj match {
      case "Datastory" => typeOf[Datastory]
    }

    objType.members.collect{
      case m: MethodSymbol if m.isCaseAccessor => m.name.toString
    }.toList
  }

  def searchLast(username: String, groups: List[String]): Future[List[SearchResult]] = {
    Logger.logger.debug(s"elasticsearchUrl: $elasticsearchUrl elasticsearchPort: $elasticsearchPort")

    val client = HttpClient(ElasticsearchClientUri(elasticsearchUrl, elasticsearchPort))
    val fieldsDataset = List("dcatapit.name", "dcatapit.title", "dcatapit.modified", "dcatapit.privatex",
      "dcatapit.theme", "dcatapit.owner_org", "dcatapit.author", "operational.input_src")
    val fieldsOpenData = List("name", "title", "notes", "organization.name", "theme", "modified")
    val fieldsDatastory: List[String] = listFields("Datastory")

    val queryDataset: SearchDefinition = queryHome("catalog_test", username, groups)
    val queryOpendata: SearchDefinition = queryHome("ext_opendata", "", Nil)
    val queryDatastory: SearchDefinition = queryHome("datastory", username, groups)
      .sortByFieldDesc("timestamp")
      .size(3)
    val queryAggr: SearchDefinition = queryHome("", username, groups)

    val resultFutureDataset: Future[SearchResponse] = executeQueryHome(client, queryDataset, fieldsDataset)
    val resultFutureOpendata: Future[SearchResponse] = executeQueryHome(client, queryOpendata, fieldsOpenData)
    val resultFutureDatastory: Future[SearchResponse] = executeQueryHome(client, queryDatastory, fieldsDatastory)
    val resultFutureAggr: Future[SearchResponse] = executeAggrQueryHome(client, queryAggr)

    val result: Future[List[SearchResult]] = for{
      resDataset <- extractAllLastDataset(wrapResponseHome(resultFutureDataset), wrapResponseHome(resultFutureOpendata))
      resDatastory <- wrapResponseHome(resultFutureDatastory)
      resAggr <- wrapAggrResponseHome(resultFutureAggr)
    } yield resDataset ::: resDatastory ::: resAggr

    result onComplete{r => client.close(); Logger.logger.debug(s"found ${r.getOrElse(List()).size} results")}

    result
  }

  def searchLastPublic(org: Option[String]): Future[List[SearchResult]] = {
    Logger.logger.debug(s"elasticsearchUrl: $elasticsearchUrl elasticsearchPort: $elasticsearchPort")
    Logger.logger.debug(s"organization: $org")

    val client = HttpClient(ElasticsearchClientUri(elasticsearchUrl, elasticsearchPort))
    val fieldsDataset = List("dcatapit.name", "dcatapit.title", "dcatapit.modified", "dcatapit.privatex",
      "dcatapit.theme", "dcatapit.owner_org", "dcatapit.author", "operational.input_src")
    val fieldsOpenData = List("name", "title", "notes", "organization.name", "theme", "modified")
    val fieldsDatastory: List[String] = listFields("Datastory")

    val queryDataset: SearchDefinition = queryHomePublic("catalog_test", org)
    val queryOpendata: SearchDefinition = queryHomePublic("ext_opendata", org)
    val queryDatastory: SearchDefinition = queryHomePublic("datastory", org)
      .sortByFieldDesc("timestamp")
      .size(3)
    val queryAggr: SearchDefinition = queryHomePublic("", org)

    val resultFutureDataset: Future[SearchResponse] = executeQueryHome(client, queryDataset, fieldsDataset)
    val resultFutureOpendata: Future[SearchResponse] = executeQueryHome(client, queryOpendata, fieldsOpenData)
    val resultFutureDatastory: Future[SearchResponse] = executeQueryHome(client, queryDatastory, fieldsDatastory)
    val resultFutureAggr: Future[SearchResponse] = executeAggrQueryHome(client, queryAggr)

    val result: Future[List[SearchResult]] = for{
      resDataset <- extractAllLastDataset(wrapResponseHome(resultFutureDataset), wrapResponseHome(resultFutureOpendata))
      resDatastory <- wrapResponseHome(resultFutureDatastory)
      resAggr <- wrapAggrResponseHome(resultFutureAggr)
    }yield resDataset ::: resDatastory ::: resAggr

    result onComplete {r => client.close(); Logger.logger.debug(s"found ${r.getOrElse(List()).size} results")}
    result
  }

  private def queryHome(typeElastic: String, username: String, groups: List[String]): SearchDefinition = {
    search("ckan").types(typeElastic).query(
      boolQuery()
        .must(
          should(
            should(
              must(matchQuery("operational.acl.groupName", groups.mkString(" "))),
              must(termQuery("dcatapit.privatex", true), termQuery("dcatapit.author", username))
            ),
            termQuery("dcatapit.privatex", false),
            must(termQuery("status", 0), termQuery("user", username)),
            must(termQuery("status", 1), matchQuery("org", groups.mkString(" "))),
            termQuery("status", 2),
            termQuery("private", false)
          )
        )
    )
  }

  private def queryHomePublic(typeElastic: String, org: Option[String]): SearchDefinition = {
    def createTermQueryPublicHomeOrg(org: String): BoolQueryDefinition = {
      should(
        must(termQuery("dcatapit.owner_org", org), termQuery("dcatapit.privatex", false)),
        must(termQuery("org", org), termQuery("status", 2)),
        must(termQuery("organization.name", org), termQuery("private", false))
      )
    }
    def createTermQueryPublicHomeNoOrg: BoolQueryDefinition = {
      should(
        termQuery("dcatapit.privatex", false),
        termQuery("status", 2),
        termQuery("private", false)
      )
    }

    search("ckan").types(typeElastic).query(
      boolQuery()
        .must(
          org match {
            case Some(organization) => createTermQueryPublicHomeOrg(organization)
            case _ => createTermQueryPublicHomeNoOrg
          }
        )
    )
  }

  private def extractLastDataset(seqDatasetDaf: Seq[SearchResult]): List[(String, SearchResult)] = {
    seqDatasetDaf.map(
      d => (extractDate(d.`type`.get, d.source.get), d)
    ).toList.sortWith(_._1 > _._1).take(3)
  }

  private def extractAllLastDataset(seqDatasetDaf: Future[Seq[SearchResult]], seqDatasetOpen: Future[List[SearchResult]]): Future[List[SearchResult]] = {
    val listDaf: Future[List[(String, SearchResult)]] = seqDatasetDaf map (daf => extractLastDataset(daf))
    val listOpen: Future[List[(String, SearchResult)]] = seqDatasetOpen map (open => extractLastDataset(open))
    val listDataset: Future[List[(String, SearchResult)]] = mergeListFuture(listDaf, listOpen)
    val result: Future[List[SearchResult]] = listDataset map (l => l.sortWith(_._1 > _._1).take(3).map(elem => elem._2))
    result
  }

  private def mergeListFuture(listDaf: Future[List[(String, SearchResult)]], listOpen: Future[List[(String, SearchResult)]]): Future[List[(String, SearchResult)]] = {
    for{
      daf <- listDaf
      open <- listOpen
    } yield daf ::: open
  }

  private def executeAggrQueryHome(client: HttpClient, query: SearchDefinition): Future[SearchResponse] = {
    client.execute(query.aggregations(termsAgg("type", "_type")))
  }

  private def executeQueryHome(client: HttpClient, query: SearchDefinition, field: List[String]): Future[SearchResponse] = {
    client.execute(query.sourceInclude(field))
  }

  private def wrapAggrResponseHome(query: Future[SearchResponse]): Future[List[SearchResult]] = {
    query map(q => q.aggregations.map{ elem =>
      val name: String = elem._1
      val valueMap: Map[String, Int] = elem._2.asInstanceOf[Map[String, Any]]("buckets").asInstanceOf[List[Map[String, AnyVal]]]
        .map(bucket => wrapAggrResp(bucket.values.toList)).map(v => v._1 -> v._2).toMap
      name -> valueMap
    }.map(elem =>SearchResult(Some(elem._1),Some("{" + elem._2.map(v => s""""${v._1}":"${v._2}"""").mkString(",") + "}"), None)).toList
      )
  }

  private def wrapResponseHome(query: Future[SearchResponse]): Future[List[SearchResult]] = {
    query map (q => q.hits.hits.map(source =>
      createSearchResult(source, search = false)
    ).toList)
  }

}
