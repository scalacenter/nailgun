package nailgun.build
import ch.epfl.scala.sbt.pom.{PomBuild, SbtPomKeys}
import sbt.{Compile, Def, Keys, file, url, Developer, Global}
import sbtdynver.DynVerPlugin.{autoImport => DynVerKeys}

import ch.epfl.scala.sbt.release.{AutoImported => ReleaseEarlyNamespace}
import com.typesafe.sbt.SbtPgp.{autoImport => PgpKeys}
import sbtdynver.GitDescribeOutput

object NailgunBuild extends PomBuild {
  override def projectDefinitions(baseDirectory: sbt.File) = {
    super.projectDefinitions(baseDirectory).map { p =>
      p.settings(projectSettings)
    }
  }

  private def GitHub(org: String, project: String): java.net.URL =
    url(s"https://github.com/$org/$project")
  private def GitHubDev(handle: String, fullName: String, email: String) =
    Developer(handle, fullName, email, url(s"https://github.com/$handle"))

  private final val ThisRepo = GitHub("scalacenter", "bloop")
  val projectSettings: Seq[Def.Setting[_]] = List(
    SbtPomKeys.isJavaOnly := true,
    Keys.version := DynVerKeys.dynverGitDescribeOutput.value
      .version(DynVerKeys.dynverCurrentDate.value),
    Keys.crossPaths := false,
    Keys.startYear := Some(2017),
    Keys.autoAPIMappings := true,
    Keys.publishMavenStyle := true,
    Keys.homepage := Some(ThisRepo),
    Keys.licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    Keys.developers := List(
      GitHubDev("mlamb", "Marty Lamb", "mlabm@martiansoftware.com"),
      GitHubDev("jvican", "Jorge Vicente Cantero", "jorge@vican.me"),
      GitHubDev("Duhemm", "Martin Duhem", "mnduhem@gmail.com"),
    ),
    Keys.organization := "ch.epfl.scala",
    ReleaseEarlyNamespace.releaseEarlyWith := ReleaseEarlyNamespace.SonatypePublisher,
    PgpKeys.pgpPublicRing in Global := {
      if (Keys.insideCI.value) file("/drone/.gnupg/pubring.asc")
      else PgpKeys.pgpPublicRing.in(Global).value
    },
    PgpKeys.pgpSecretRing in Global := {
      if (Keys.insideCI.value) file("/drone/.gnupg/secring.asc")
      else PgpKeys.pgpSecretRing.in(Global).value
    },
    Keys.publishArtifact in (Compile, Keys.packageDoc) := {
      val output = DynVerKeys.dynverGitDescribeOutput.value
      val version = Keys.version.value
      publishDocAndSourceArtifact(output, version)
    },
    Keys.publishArtifact in (Compile, Keys.packageSrc) := {
      val output = DynVerKeys.dynverGitDescribeOutput.value
      val version = Keys.version.value
      publishDocAndSourceArtifact(output, version)
    },
  )

  /**
   * This setting figures out whether the version is a snapshot or not and configures
   * the source and doc artifacts that are published by the build.
   *
   * Snapshot is a term with no clear definition. In this code, a snapshot is a revision
   * that is dirty, e.g. has time metadata in its representation. In those cases, the
   * build will not publish doc and source artifacts by any of the publishing actions.
   */
  def publishDocAndSourceArtifact(info: Option[GitDescribeOutput], version: String): Boolean = {
    val isStable = info.map(_.dirtySuffix.value.isEmpty)
    !isStable.map(stable => !stable || version.endsWith("-SNAPSHOT")).getOrElse(false)
  }
}
