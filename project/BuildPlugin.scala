package nailgun.build
import ch.epfl.scala.sbt.pom.{PomBuild, SbtPomKeys}
import sbt.{Def, Keys, ThisBuild}
import sbtdynver.DynVer
import sbtdynver.DynVerPlugin.{autoImport => DynVerKeys}

object NailgunBuild extends PomBuild {
  override def projectDefinitions(baseDirectory: sbt.File) = {
    super.projectDefinitions(baseDirectory).map { p =>
      p.settings(projectSettings)
    }
  }

  val newVersion: Def.Initialize[String] = Def.settingDyn {
    val ref = Keys.thisProjectRef.value
    Def.setting(Keys.version.in(ThisBuild).in(ref).value)
  }

  val dynverInstance: Def.Initialize[DynVer] = Def.settingDyn {
    val ref = Keys.thisProjectRef.value
    Def.setting(DynVer(Some((Keys.baseDirectory in ThisBuild in ref).value)))
  }

  val projectSettings: Seq[Def.Setting[_]] = List(
    SbtPomKeys.isJavaOnly := true,
    // Necessary because of weird settings init of sbt-pom-reader
    Keys.version := newVersion.value,
    DynVerKeys.dynverInstance := dynverInstance.value
  )
}
