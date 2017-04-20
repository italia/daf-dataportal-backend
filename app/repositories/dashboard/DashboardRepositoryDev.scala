package repositories.dashboard

import java.io.File
import java.util.Date

import ftd_api.yaml.Success
import java.nio.file.Files
import java.nio.file.StandardCopyOption


/**
  * Created by ale on 14/04/17.
  */
class DashboardRepositoryDev extends DashboardRepository{

  def save(upFile :File,tableName :String) :Success = {
    val message = s"Table created  $tableName"
    val fileName = new Date().getTime() + ".txt"
    val currentPath =  new java.io.File(".").getCanonicalPath
    val copyFile = new File(System.getProperty("user.home") + "/metabasefile/" + tableName)
    copyFile.mkdirs()
    val copyFilePath = copyFile.toPath
    Files.copy(upFile.toPath, copyFilePath, StandardCopyOption.REPLACE_EXISTING)
    Success(Some(message), Some("Good!!"))
  }

  def update(upFile :File,tableName :String) :Success = {
    val message = s"Table created  $tableName"
    val fileName = new Date().getTime() + ".txt"
    val copyFile = new File(System.getProperty("user.home") + "/metabasefile/" + tableName)
    copyFile.mkdirs()
    val copyFilePath = copyFile.toPath
    Files.copy(upFile.toPath, copyFilePath, StandardCopyOption.REPLACE_EXISTING)
    Success(Some(message), Some("Good!!"))
  }
}
