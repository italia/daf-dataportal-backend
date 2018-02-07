package repositories.settings

import ftd_api.yaml.{Error, Settings, Success}

class SettingsRepositoryDev extends SettingsRepository {


  override def saveSettings(name: String, settings: Settings): Either[Error, Success] = {
    Right(Success(None, None))
  }

  override def settingsByName(name: String) = {
       Right(Settings(None, None, None, None, None, None, None, None, None, None, None, None, None, None, None))
  }

  override def getDomain(groups: List[String]): Seq[String] = Seq("")
}
