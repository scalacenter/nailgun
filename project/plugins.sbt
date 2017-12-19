val SbtPomReader = RootProject(uri("git://github.com/scalacenter/sbt-pom-reader#2a6ebd37af79b6d9c295876aa611b722ea7dcfd4"))
dependsOn(ProjectRef(SbtPomReader.build, "sbt-pom-reader"))
