package repositories

import java.io.FileInputStream

import ftd_api.yaml.{BrokenLink, Catalog, Distribution}
import play.api.libs.json._
import play.Environment
import repositories.monitor.{MonitorDevRepoUtil, Repository}

import scala.collection.immutable.List


/**
  * Created by ale on 31/03/17.
  */





trait MonitorRepositoryComponent {
  val monitorRepository: Repository //= new MonitorRepository

}

