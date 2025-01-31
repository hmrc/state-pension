
import play.sbt.routes.RoutesKeys.*
import sbt.Test
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.addTestReportOption
import uk.gov.hmrc.{DefaultBuildSettings, SbtAutoBuildPlugin}

lazy val appName = "state-pension"

ThisBuild / scalaVersion := "3.6.3"
ThisBuild / majorVersion := 2

scalacOptions ++= Seq(
  "-Xfatal-warnings",
//  "-Xmaxerrs", "1000", // Maximum errors to print
//  "-Xmaxwarns", "1000", // Maximum warnings to print
  // Suggested here https://github.com/playframework/twirl/issues/105#issuecomment-782985171
  "-Wconf:msg=Flag -deprecation set repeatedly:s",
  "-Wconf:msg=Flag -unchecked set repeatedly:s",
  "-Wconf:msg=Flag -encoding set repeatedly:s"
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
    ScoverageKeys.coverageMinimumStmtTotal := 93,
    ScoverageKeys.coverageMinimumBranchTotal := 83,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}
