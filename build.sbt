
import play.sbt.routes.RoutesKeys.*
import sbt.Test
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.addTestReportOption
import uk.gov.hmrc.{DefaultBuildSettings, SbtAutoBuildPlugin}

lazy val appName = "state-pension"

ThisBuild / scalaVersion := "3.7.3"
ThisBuild / majorVersion := 2

ThisBuild / scalacOptions ++= Seq(
  "-Wconf:src=routes/.*:s",
  "-Wconf:msg=Flag.*repeatedly:s",
  "-Wconf:msg=unused.import&src=html/.*:s",
  "-Wconf:msg=unused.import&src=txt/.*:s",
  "-feature",
  "-deprecation",
  "-Werror"
)

lazy val plugins: Seq[Plugins] = Seq(
  play.sbt.PlayScala, SbtAutoBuildPlugin, SbtDistributablesPlugin
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(plugins *)
  .settings(
    scoverageSettings,
    PlayKeys.playDefaultPort := 9311,
    libraryDependencies ++= AppDependencies.all,
    retrieveManaged := true,
    routesImport ++= Seq(
      "uk.gov.hmrc.statepension.config.Binders._"
    )
  )
  .settings(inConfig(Test)(testSettings) *)

lazy val testSettings: Seq[Def.Setting[?]] = Seq(
  fork := true,
  unmanagedSourceDirectories += baseDirectory.value / "test-utils" / "src",
  Test / javaOptions += "-Dconfig.file=conf/test.application.conf"
)

lazy val it: Project = (project in file("it"))
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") //allows the reusing of test code and dependencies
  .settings(
    Test / unmanagedSourceDirectories ++= baseDirectory(base => Seq(base / "it")).value,
    DefaultBuildSettings.itSettings(),
    addTestReportOption(Test, "int-test-reports"),
    Test / parallelExecution := false
  )

val excludedPackages = Seq(
  "<empty>",
  "Reverse.*",
  "uk.gov.hmrc.statepension.views.*",
  ".*(AuthService|BuildInfo|Routes).*",
  ".*CheckPensionController.*",
  ".*DashboardController.*",
  ".*PolicyDecisions.*",
  ".*LiabilityType.*",
).mkString(";")

lazy val scoverageSettings: Seq[Def.Setting[?]] = {
  Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages,
    ScoverageKeys.coverageMinimumStmtTotal := 85,
    ScoverageKeys.coverageMinimumBranchTotal := 75,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}
