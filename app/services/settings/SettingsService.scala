package services.settings

import play.api.{Configuration, Environment}
import ftd_api.yaml.{Error, Settings, Success}
import repositories.settings.{SettingsRepository, SettingsRepositoryComponent}

import scala.collection.immutable.List

trait SettingsServiceComponent {
  this: SettingsRepositoryComponent =>
  val settingsService: SettingsService

  class SettingsService {
    def saveSettings(name: String, settings: Settings): Either[Error, Success] = {
      settingsRepository.saveSettings(name, settings)
    }

    def settingsByName(name: String): Either[Error, Settings]= {
      settingsRepository.settingsByName(name)
    }

    def getDomain = {
      settingsRepository.getDomain
    }
  }
}

object SettingsRegistry extends SettingsRepositoryComponent
  with SettingsServiceComponent
{
  val conf = Configuration.load(Environment.simple())
  val app: String = conf.getString("app.type").getOrElse("prod")
  val settingsRepository =  SettingsRepository(app)
  val settingsService = new SettingsService

}