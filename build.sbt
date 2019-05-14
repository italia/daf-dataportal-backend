import CommonBuild._
import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}
import de.heikoseeberger.sbtheader.license.Apache2_0
import de.zalando.play.generator.sbt.ApiFirstPlayScalaCodeGenerator.autoImport.playScalaAutogenerateTests
import play.sbt.routes.RoutesKeys.routesGenerator
import sbt.Keys.resolvers


organization in ThisBuild := "it.gov.daf"

name := "daf-datipubblici"

//version in ThisBuild := "1.0.1-SNAPSHOT"

//version in ThisBuild := "1.0-alpha.1"

version in ThisBuild := "2.0.10"

val isStaging = System.getProperty("STAGING") != null

val playVersion = "2.5.14"

/*
lazy val client = (project in file("client")).
  settings(Seq(
    name := "daf-dati-gov-client",
    swaggerGenerateClient := true,
    swaggerClientCodeGenClass := new it.gov.daf.swaggergenerators.DafClientGenerator,
    swaggerCodeGenPackage := "it.gov.daf.datigov",
    swaggerSourcesDir := file(s"${baseDirectory.value}/../conf"),
    libraryDependencies ++= Seq(
      "com.typesafe.play" %"play-json_2.11" % playVersion,
      "com.typesafe.play" % "play-ws_2.11" %  playVersion
    )
  )).enablePlugins(SwaggerCodegenPlugin) */

lazy val root = (project in file(".")).enablePlugins(PlayScala, ApiFirstCore, ApiFirstPlayScalaCodeGenerator, ApiFirstSwaggerParser, Jolokia)
  .settings(
    jolokiaPort := "7000",
    jolokiaProtocol := "http"
  )
 // .dependsOn(client).aggregate(client)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  filters,
  specs2 % Test,
  "org.scalacheck"          %% "scalacheck"         % "1.12.4" % Test,
  "org.specs2"              %% "specs2-scalacheck"  % "3.6" % Test,
  "me.jeffmay"              %% "play-json-tests"    % "1.3.0" % Test,
  "org.scalatestplus.play"  %% "scalatestplus-play" % "1.5.1" % Test,
  "org.mongodb" %% "casbah" % "3.1.1",
  "net.sf.opencsv" % "opencsv" % "2.3",
  "me.lessis" %% "base64" % "0.2.0",
  "it.gov.daf" %% "common" % "1.1.3",
  "com.github.cb372" %% "scalacache-guava" % "0.9.4",
  "com.chuusai" %% "shapeless" % "2.3.2",
  "com.sksamuel.avro4s" %% "avro4s-core" % "1.8.0",
  "com.sksamuel.avro4s" %% "avro4s-json" % "1.8.0",
//  "com.sksamuel.avro4s" %% "avro4s-generator" % "1.8.0"
  "com.sksamuel.elastic4s" %% "elastic4s-core" % "5.6.4",
  "com.sksamuel.exts" %% "exts" % "1.60.0",
  "com.sksamuel.elastic4s" %% "elastic4s-http" % "5.6.4",
  "org.elasticsearch.client" % "elasticsearch-rest-client" % "6.2.2"
)


resolvers ++= Seq(
  "zalando-bintray" at "https://dl.bintray.com/zalando/maven",
  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
  "jeffmay" at "https://dl.bintray.com/jeffmay/maven",
  "cloudera" at "https://repository.cloudera.com/artifactory/cloudera-repos/",
  Resolver.url("sbt-plugins", url("http://dl.bintray.com/zalando/sbt-plugins"))(Resolver.ivyStylePatterns),
  Resolver.mavenLocal,
  Resolver.bintrayRepo("jtescher", "sbt-plugin-releases")
)

resolvers ++= { if(isStaging) Seq("daf repo" at "http://nexus.teamdigitale.test:8081/repository/maven-public/")
                else Seq("daf repo" at "http://nexus.daf.teamdigitale.it:8081/repository/maven-public/")}


playScalaCustomTemplateLocation := Some(baseDirectory.value / "templates")

routesGenerator := InjectedRoutesGenerator

apiFirstParsers := Seq(ApiFirstSwaggerParser.swaggerSpec2Ast.value).flatten

playScalaAutogenerateTests := false

headers := Map(
  "sbt" -> Apache2_0("2017", "TEAM PER LA TRASFORMAZIONE DIGITALE"),
  "scala" -> Apache2_0("2017", "TEAM PER LA TRASFORMAZIONE DIGITALE"),
  "conf" -> Apache2_0("2017", "TEAM PER LA TRASFORMAZIONE DIGITALE", "#"),
  "properties" -> Apache2_0("2017", "TEAM PER LA TRASFORMAZIONE DIGITALE", "#"),
  "yaml" -> Apache2_0("2017", "TEAM PER LA TRASFORMAZIONE DIGITALE", "#")
)

dockerBaseImage := "anapsix/alpine-java:8_jdk_unlimited"
dockerCommands := dockerCommands.value.flatMap {
  case cmd@Cmd("FROM", _) => List(cmd,
    Cmd("RUN", "apk update && apk add bash krb5-libs krb5"),
    Cmd("RUN", "ln -sf /etc/krb5.conf /opt/jdk/jre/lib/security/krb5.conf")
  )
  case other => List(other)
}

dockerExposedPorts := Seq(9000, 7000)

dockerEntrypoint := {Seq(s"bin/${name.value}", "-Dconfig.file=conf/production.conf")}

dockerRepository := { if(isStaging)Option("nexus.teamdigitale.test") else Option("nexus.daf.teamdigitale.it") }


publishTo in ThisBuild := {
  val nexus = if(isStaging) {
    "http://nexus.teamdigitale.test:8081/repository/"
  } else
    { "http://nexus.daf.teamdigitale.it:8081/repository/"}

  if (isSnapshot.value)
    Some("snapshots" at nexus + "maven-snapshots/")
  else
    Some("releases"  at nexus + "maven-releases/")
}

//credentials += {if(isStaging) Credentials(Path.userHome / ".ivy2" / ".credentialsTest") else Credentials(Path.userHome / ".ivy2" / ".credentials")}
credentials += { Credentials(Path.userHome / ".ivy2" / ".credentials") }


javaOptions in Test += "-Dconfig.resource=" + System.getProperty("config.resource", "production.conf")
playScalaAutogenerateTests := false
