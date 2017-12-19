package nailgun.build
import ch.epfl.scala.sbt.pom.{PomBuild, SbtPomKeys}
import sbt.Def

object NailgunBuild extends PomBuild {
  override def projectDefinitions(baseDirectory: sbt.File) =
    super.projectDefinitions(baseDirectory).map(p => p.settings(SbtPomKeys.isJavaOnly := true))
}
