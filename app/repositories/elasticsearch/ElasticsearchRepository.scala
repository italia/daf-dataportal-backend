package repositories.elasticsearch

import ftd_api.yaml.{Filters, SearchResult}

import scala.concurrent.Future

trait ElasticsearchRepository {

  def searchText(filters: Filters, username: String, groups: List[String], limit: Option[Int]): Future[List[SearchResult]]
  def searchLast(username: String, groups: List[String]): Future[List[SearchResult]]
  def searchLastPublic(org: Option[String]): Future[List[SearchResult]]
  def searchTextPublic(filters: Filters, limit: Option[Int]): Future[List[SearchResult]]

}

object ElasticsearchRepository {
  def apply(config: String): ElasticsearchRepository = config match {
    case "dev"  => new ElasticsearchrepositoryDev
    case "prod" => new ElasticsearchrepositoryProd
  }
}

trait ElasticsearchRepositoryComponent {
  val elasticsearchRepository: ElasticsearchRepository
}