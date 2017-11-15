package nailgun.build
import com.typesafe.sbt.pom.SbtPomKeys
import sbt.Def

object NailgunBuild extends com.typesafe.sbt.pom.PomBuild {
  override def projectDefinitions(baseDirectory: sbt.File) =
    super.projectDefinitions(baseDirectory).map(p => p.settings(SbtPomKeys.isJavaOnly := true))
}
