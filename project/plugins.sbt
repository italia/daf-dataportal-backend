resolvers += Resolver.url("sbt-plugins", url("http://dl.bintray.com/zalando/sbt-plugins"))(Resolver.ivyStylePatterns)

resolvers += "zalando-bintray"  at "https://dl.bintray.com/zalando/maven"

resolvers += "scalaz-bintray"   at "http://dl.bintray.com/scalaz/releases"

addSbtPlugin("com.typesafe.play" % "sbt-plugin"       % "2.5.14")

addSbtPlugin("de.zalando" % "sbt-api-first-hand" % "0.2.3")

//addSbtPlugin("de.zalando" % "sbt-api-first-hand" % "0.2.13")
