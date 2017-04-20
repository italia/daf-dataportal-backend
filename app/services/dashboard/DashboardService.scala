package services.dashboard

import java.io.File

import ftd_api.yaml.Success
import play.api.{Configuration, Environment}
import repositories.dashboard.{DashboardRepository, DashboardRepositoryComponent}



/**
  * Created by ale on 16/04/17.
  */
trait DashboardServiceComponent {
  this: DashboardRepositoryComponent =>
  val dashboardService: DashboardService

  class DashboardService {
    def save(upFile :File,tableName :String) :Success = {
      val result = dashboardRepository.save(upFile,tableName)
      result
    }

    def update(upFile :File,tableName :String) :Success = {
      val result = dashboardRepository.update(upFile,tableName)
      result
    }

  }
}


object DashboardRegistry extends
  DashboardServiceComponent with
  DashboardRepositoryComponent
{
  val conf = Configuration.load(Environment.simple())
  val app: String = conf.getString("app.type").getOrElse("dev")
  val dashboardRepository =  DashboardRepository(app)
  val dashboardService = new DashboardService
}
