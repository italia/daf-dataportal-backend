import java.io.FileInputStream
import org.specs2.mutable.Specification
import play.api.{Application, Configuration, Environment}
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsString, JsValue, Json}
import play.api.libs.ws.WSResponse
import play.api.test.{WithServer, WsTestClient}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Created by andrea on 05/07/17.
  */
class FrontendServerSpecs extends Specification{

  def application: Application = GuiceApplicationBuilder().build()

  private def readJsonFile(path: String):JsValue = {
    val streamDataset = new FileInputStream(Environment.simple().getFile(path))
    try {
      Json.parse(streamDataset)
    }catch {
      case tr: Throwable => tr.printStackTrace(); JsString("Empty file")
    }finally {
      streamDataset.close()
    }
  }

  "The frontend-server" should {

    "Call /ckan/datasets return ok status" in
      new WithServer(app = application, port = 9000) {
        WsTestClient.withClient { implicit client =>
          val response: WSResponse = Await.result[WSResponse](client.
            url("http://localhost:9000/ckan/datasets").
            get, Duration.Inf)
          //println(response.status)
          response.status must be equalTo Status.OK
        }
      }

  }

}
