val SbtPomReader = RootProject(uri("git://github.com/scalacenter/sbt-pom-reader#a9a775d019119d67559482b690481d46c7104a8c"))
dependsOn(ProjectRef(SbtPomReader.build, "sbt-pom-reader"))
