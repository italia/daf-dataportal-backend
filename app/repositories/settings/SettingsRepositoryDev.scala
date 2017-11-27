package repositories.settings

import ftd_api.yaml.{Settings, Success}

class SettingsRepositoryDev extends SettingsRepository {


  override def saveSettings(name: String, settings: Settings): Success = {
    Success(None, None)
  }

  override def settingsByName(name: String): Settings = {
       Settings(None, None, None, None, None, None, None, None, None, None, None, None, None, None)
  }
}
