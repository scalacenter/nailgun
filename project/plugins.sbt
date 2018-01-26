val SbtPomReader = RootProject(uri("git://github.com/scalacenter/sbt-pom-reader#2a6ebd37af79b6d9c295876aa611b722ea7dcfd4"))
dependsOn(ProjectRef(SbtPomReader.build, "sbt-pom-reader"))
addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0")
addSbtPlugin("com.dwijnand" % "sbt-dynver" % "2.0.0")
