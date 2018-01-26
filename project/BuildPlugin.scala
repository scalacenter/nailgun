package nailgun.build
import ch.epfl.scala.sbt.pom.{PomBuild, SbtPomKeys}
import sbt.{Def, Keys}
import sbtdynver.DynVer
import sbtdynver.DynVerPlugin.autoImport.{dynver, dynverCurrentDate, dynverGitDescribeOutput}

object NailgunBuild extends PomBuild {
  val ourDynVerInstance = sbt.settingKey[sbtdynver.DynVer]("dynver2")
  val dynverSettings: Seq[Def.Setting[_]] = List(
    ourDynVerInstance := DynVer(Some(Keys.baseDirectory.in(sbt.ThisBuild).value)),
    dynver := ourDynVerInstance.value.version(new java.util.Date),
    dynverGitDescribeOutput :=
      ourDynVerInstance.value.getGitDescribeOutput(dynverCurrentDate.value),
  )

  override def settings: Seq[Def.Setting[_]] = super.settings ++ dynverSettings

  override def projectDefinitions(baseDirectory: sbt.File) =
    super.projectDefinitions(baseDirectory)
      .map(p => p.settings(SbtPomKeys.isJavaOnly := true))
      .map(p => p.settings(Keys.version := ourDynVerInstance.value.version(new java.util.Date)))
}
