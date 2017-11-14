val SbtPomReader = RootProject(uri("git://github.com/sbt/sbt-pom-reader#555125e54b6035fd5df0dcdda18e8d31bcdbefec"))
dependsOn(ProjectRef(SbtPomReader.build, "sbt-pom-reader"))
