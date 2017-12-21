val SbtPomReader = RootProject(uri("git://github.com/scalacenter/sbt-pom-reader#213c1ee0e6541ca11c2e7ac50783830baaca363c"))
dependsOn(ProjectRef(SbtPomReader.build, "sbt-pom-reader"))
