package repositories.settings

import ftd_api.yaml.{Settings, Success}

trait SettingsRepository {
  def saveSettings(name: String, settings: Settings): Success
  def settingsByName(name: String): Settings
}

object SettingsRepository {
  def apply(config: String): SettingsRepository = config match {
    case "dev" => new SettingsRepositoryDev
    case "prod" => new SettingsRepositoryProd
  }
}

trait SettingsRepositoryComponent {
  val settingsRepository: SettingsRepository
}

