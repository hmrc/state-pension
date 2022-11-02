import play.sbt.routes.RoutesKeys._
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings}
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import scoverage.ScoverageKeys

val appName = "state-pension"

scalaVersion := "2.12.13"

val suppressedImports = Seq("-P:silencer:lineContentFilters=import _root_.play.twirl.api.TwirlFeatureImports._",
  "-P:silencer:lineContentFilters=import _root_.play.twirl.api.TwirlHelperImports._",
  "-P:silencer:lineContentFilters=import _root_.play.twirl.api.Html",
  "-P:silencer:lineContentFilters=import _root_.play.twirl.api.JavaScript",
  "-P:silencer:lineContentFilters=import _root_.play.twirl.api.Txt",
  "-P:silencer:lineContentFilters=import _root_.play.twirl.api.Xml",
  "-P:silencer:lineContentFilters=import models._",
  "-P:silencer:lineContentFilters=import controllers._",
  "-P:silencer:lineContentFilters=import play.api.i18n._",
  "-P:silencer:lineContentFilters=import views.html._",
  "-P:silencer:lineContentFilters=import play.api.templates.PlayMagic._",
  "-P:silencer:lineContentFilters=import play.api.mvc._",
  "-P:silencer:lineContentFilters=import play.api.data._")

scalacOptions ++= Seq("-P:silencer:pathFilters=routes")
scalacOptions ++= suppressedImports
scalacOptions ++= Seq(
  "-Xfatal-warnings",
  "-Xmaxerrs", "1000", // Maximum errors to print
  "-Xmaxwarns", "1000", // Maximum warnings to print
  // Suggested here https://github.com/playframework/twirl/issues/105#issuecomment-782985171
  "-Wconf:src=routes/.*:is,src=twirl/.*:is"
)

lazy val plugins: Seq[Plugins] = Seq(
  play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(plugins: _*)
  .settings(
    scalaSettings,
    scoverageSettings,
    publishingSettings,
    defaultSettings(),
    majorVersion := 2,
    PlayKeys.playDefaultPort := 9311,
    libraryDependencies ++= AppDependencies.all,
    retrieveManaged := true,
    routesImport += "uk.gov.hmrc.statepension.config.Binders._"
  )
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    IntegrationTest / unmanagedSourceDirectories := (IntegrationTest / baseDirectory) (base => Seq(base / "it")).value,
    addTestReportOption(IntegrationTest, "int-test-reports"),
    IntegrationTest / parallelExecution := false
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
