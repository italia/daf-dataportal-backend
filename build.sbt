name := """TestDatiPubblici"""
organization := "it.governo.teamdigitale"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies += filters
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
libraryDependencies += "io.swagger" %% "swagger-play2" % "1.5.3"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "it.governo.teamdigitale.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "it.governo.teamdigitale.binders._"
