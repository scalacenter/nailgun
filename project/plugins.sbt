val SbtPomReader = RootProject(uri("git://github.com/scalacenter/sbt-pom-reader#e5338649ba278173e26c38cfd4dea5270c15346c"))
dependsOn(ProjectRef(SbtPomReader.build, "sbt-pom-reader"))
