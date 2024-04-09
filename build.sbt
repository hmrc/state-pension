import play.sbt.routes.RoutesKeys._
import sbt.Test
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings}
import uk.gov.hmrc.SbtAutoBuildPlugin
import scoverage.ScoverageKeys

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

lazy val microservice = (project in file("."))
  .enablePlugins(plugins: _*)
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
  .settings(inConfig(Test)(testSettings): _*)

// Remove unnecessary bits

//  .configs(IntegrationTest)
//  .settings(inConfig(IntegrationTest)(itSettings): _*)
//  .settings(
//    addTestReportOption(IntegrationTest, "int-test-reports")
//  )

lazy val testSettings: Seq[Def.Setting[_]] = Seq(
  fork := true,
  unmanagedSourceDirectories += baseDirectory.value / "test-utils" / "src",
  Test / javaOptions += "-Dconfig.file=conf/test.application.conf"
)

lazy val it: Project = (project in file("it"))
  .dependsOn(microservice)
  .settings(
    Test / unmanagedSourceDirectories ++= baseDirectory(base => Seq(base / "it")).value,
    addTestReportOption(Test, "int-test-reports"),
    Test / parallelExecution := false
  )

//lazy val itSettings = Defaults.itSettings ++ Seq(
//  fork := true,
//  parallelExecution := false,
//  unmanagedSourceDirectories := Seq(
//    baseDirectory.value / "it",
//    baseDirectory.value / "test-utils" / "src"
//  ),
//  javaOptions += "-Dconfig.file=conf/test.application.conf"
//)

lazy val scoverageSettings: Seq[Def.Setting[_]] = {
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;uk.gov.hmrc.statepension.views.*;.*(AuthService|BuildInfo|Routes).*;",
    ScoverageKeys.coverageMinimumStmtTotal := 87.91,
    ScoverageKeys.coverageMinimumBranchTotal := 83.82,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}
