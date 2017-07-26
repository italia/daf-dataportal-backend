import de.zalando.play.generator.sbt.ApiFirstPlayScalaCodeGenerator.autoImport.playScalaAutogenerateTests


name := """datipubblici"""

version := "0.1.18"

lazy val root = (project in file(".")).enablePlugins(PlayScala, ApiFirstCore, ApiFirstPlayScalaCodeGenerator, ApiFirstSwaggerParser)

scalaVersion := "2.11.7"

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
  "me.lessis" %% "base64" % "0.2.0"
)


resolvers ++= Seq(
  "zalando-bintray"  at "https://dl.bintray.com/zalando/maven",
  "scalaz-bintray"   at "http://dl.bintray.com/scalaz/releases",
  "jeffmay" at "https://dl.bintray.com/jeffmay/maven",
  "softprops-maven" at "http://dl.bintray.com/content/softprops/maven",
  Resolver.url("sbt-plugins", url("http://dl.bintray.com/zalando/sbt-plugins"))(Resolver.ivyStylePatterns)
)

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

apiFirstParsers := Seq(ApiFirstSwaggerParser.swaggerSpec2Ast.value).flatten

javaOptions in Test += "-Dconfig.resource=" + System.getProperty("config.resource", "production.conf")
playScalaAutogenerateTests := false
