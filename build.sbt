import play.sbt.routes.RoutesKeys.*
import sbt.Test
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings}
import uk.gov.hmrc.{DefaultBuildSettings, SbtAutoBuildPlugin}
import scoverage.ScoverageKeys

import scala.sys.process.*

lazy val appName = "state-pension"

ThisBuild / scalaVersion := "2.13.12"
ThisBuild / majorVersion := 2

scalacOptions ++= Seq(
  "-Xfatal-warnings",
  "-Xmaxerrs", "1000", // Maximum errors to print
  "-Xmaxwarns", "1000", // Maximum warnings to print
  // Suggested here https://github.com/playframework/twirl/issues/105#issuecomment-782985171
  "-Wconf:src=routes/.*:is,src=twirl/.*:is"
)

lazy val plugins: Seq[Plugins] = Seq(
  play.sbt.PlayScala, SbtAutoBuildPlugin, SbtDistributablesPlugin
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(plugins *)
  .settings(
    scalaSettings,
    scoverageSettings,
    defaultSettings(),
    PlayKeys.playDefaultPort := 9311,
    libraryDependencies ++= AppDependencies.all,
    ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always,
    retrieveManaged := true,
    routesImport ++= Seq(
      "uk.gov.hmrc.statepension.config.Binders._",
      "uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlagName"
    )
  )
  .settings(inConfig(Test)(testSettings) *)

lazy val testSettings: Seq[Def.Setting[_]] = Seq(
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

lazy val scoverageSettings: Seq[Def.Setting[_]] = {
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;uk.gov.hmrc.statepension.views.*;.*(AuthService|BuildInfo|Routes).*;",
    ScoverageKeys.coverageMinimumStmtTotal := 87.91,
    ScoverageKeys.coverageMinimumBranchTotal := 83.82,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}
