package services.settings

import play.api.{Configuration, Environment}
import ftd_api.yaml.{Success, Settings}
import repositories.settings.{SettingsRepository, SettingsRepositoryComponent}

trait SettingsServiceComponent {
  this: SettingsRepositoryComponent =>
  val settingsService: SettingsService

  class SettingsService {
    def saveSettings(name: String, settings: Settings): Success = {
      settingsRepository.saveSettings(name, settings)
    }

    def settingsByName(name: String): Settings = {
      settingsRepository.settingsByName(name)
    }
  }
}

object SettingsRegistry extends SettingsRepositoryComponent
  with SettingsServiceComponent
{
  val conf = Configuration.load(Environment.simple())
//  val app: String = conf.getString("app.type").getOrElse("dev")
  val settingsRepository =  SettingsRepository("prod")
  val settingsService = new SettingsService

}