import play.sbt.routes.RoutesKeys.*
import sbt.{Test, *}
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}
import uk.gov.hmrc.SbtAutoBuildPlugin

lazy val appName = "state-pension"

scalaVersion := "2.13.12"

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
  .settings(inConfig(Test)(testSettings) *)
  .settings(inConfig(Test)(it.settings) *)
  .settings(
    name := appName,
    scalaSettings,
    scoverageSettings,
    defaultSettings(),
    majorVersion := 2,
    PlayKeys.playDefaultPort := 9311,
    libraryDependencies ++= AppDependencies.all,
    ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always,
    retrieveManaged := true,
    routesImport ++= Seq(
      "uk.gov.hmrc.statepension.config.Binders._",
      "uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlagName"
    )
  )



//  .settings(inConfig(Test)(testSettings): _*)
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

//lazy val itSettings = Defaults.itSettings ++ Seq(
//  fork := true,
//  parallelExecution := false,
//  unmanagedSourceDirectories := Seq(
//    baseDirectory.value / "it",
//    baseDirectory.value / "test-utils" / "src"
//  ),
//  javaOptions += "-Dconfig.file=conf/test.application.conf"
//)

lazy val it = (project in file("it"))
  .dependsOn(microservice)
  .enablePlugins(PlayScala)
  .settings(
    Test / unmanagedSourceDirectories ++= baseDirectory(
      base => Seq(
        base / "it",
        base / "test-utils" / "src"
      )).value,
    Test / parallelExecution := false,
    Test / fork := true,
    Test / javaOptions += "-Dconfig.file=conf/test.application.conf"
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
