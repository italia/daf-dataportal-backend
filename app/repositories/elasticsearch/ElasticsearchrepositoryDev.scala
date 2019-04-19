package repositories.elasticsearch
import ftd_api.yaml
import ftd_api.yaml.SearchResult

import scala.concurrent.Future

class ElasticsearchrepositoryDev extends ElasticsearchRepository {

  override def searchText(filters: yaml.Filters, username: String, groups: List[String], limit: Option[Int]): Future[List[SearchResult]] = {
    Future.successful(List[SearchResult]())
  }

  override def searchLast(username: String, groups: List[String]): Future[List[SearchResult]] = {
    Future.successful(List[SearchResult]())
  }

  override def searchLastPublic(org: Option[String]): Future[List[SearchResult]] = {
    Future.successful(List[SearchResult]())
  }

  override def searchTextPublic(filters: yaml.Filters, limit: Option[Int]): Future[List[SearchResult]] = {
    Future.successful(List[SearchResult]())
  }
}
