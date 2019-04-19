package services.elasticsearch

import ftd_api.yaml.{Filters, SearchResult}
import play.api.{Configuration, Environment}
import repositories.elasticsearch.{ElasticsearchRepository, ElasticsearchRepositoryComponent}

import scala.concurrent.Future

trait ElasticsearchServiceComponent {
  this: ElasticsearchRepositoryComponent =>
  val elasticsearchService: ElasticsearchService

  class ElasticsearchService {

    def searchText(filters: Filters, username: String, groups: List[String], limit: Option[Int]): Future[List[SearchResult]] = {
      elasticsearchRepository.searchText(filters, username, groups, limit)
    }

    def searchLast(username: String, groups: List[String]): Future[List[SearchResult]] = {
      elasticsearchRepository.searchLast(username, groups)
    }
    def searchLastPublic(org: Option[String]): Future[List[SearchResult]] = {
      elasticsearchRepository.searchLastPublic(org)
    }

    def searchTextPublic(filters: Filters, limit: Option[Int]): Future[List[SearchResult]] = {
      elasticsearchRepository.searchTextPublic(filters, limit)
    }
  }

}


object ElasticsearchRegistry extends ElasticsearchServiceComponent with ElasticsearchRepositoryComponent {
  val conf = Configuration.load(Environment.simple())
  val app: String = conf.getString("app.type").getOrElse("dev")
  val elasticsearchRepository =  ElasticsearchRepository(app)
  val elasticsearchService = new ElasticsearchService
}