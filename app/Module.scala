import com.google.inject.{AbstractModule, Singleton}
import it.gov.daf.common.sso.client.LoginClientRemote
import it.gov.daf.common.sso.common.{CacheWrapper, LoginClient}
import play.api.{Configuration, Environment, Logger}

import scala.sys.process._

@Singleton
class Module(environment: Environment, configuration: Configuration) extends AbstractModule {

  def configure() = {

    Logger.debug("Datipubblici lib v.2.0.0-SNAPSHOT-3.2")

    Logger.debug("executing module..")

    Logger.debug("executing module..")

    Logger.debug("--env--")
    Logger.debug("ls -l".!! )
    Logger.debug("-------")
    Logger.debug("ls -l conf".!! )
    Logger.debug("-------")

    bind(classOf[LoginClient]).to(classOf[LoginClientRemote])// for the initialization of SecuredInvocationManager

    val cacheWrapper = new CacheWrapper( Option(30L), Option(0L) )// cookie 30 min, credential not needed
    bind(classOf[CacheWrapper]).toInstance(cacheWrapper)

    val securityManHost :Option[String] = configuration.getString("security.manager.host")
    require(!securityManHost.isEmpty)

    val loginClientRemote = new LoginClientRemote(securityManHost.get)
    bind(classOf[LoginClientRemote]).toInstance(loginClientRemote)

  }

}
