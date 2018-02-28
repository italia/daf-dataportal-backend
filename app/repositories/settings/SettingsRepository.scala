package repositories.settings

import ftd_api.yaml.{Error, Settings, Success}

trait SettingsRepository {
  def saveSettings(name: String, settings: Settings): Either[Error, Success]
  def settingsByName(name: String): Either[Error, Settings]
  def getDomain(groups: List[String], isAdmin: Boolean): Seq[String]
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

