import com.typesafe.sbt.digest.Import._
import com.typesafe.sbt.web.Import._
import uk.gov.hmrc.DefaultBuildSettings

Global / bloopAggregateSourceDependencies := true
Global / bloopExportJarClassifiers := Some(Set("sources"))

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.7.4"

ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

lazy val microservice = Project("devhub-support-frontend", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    pipelineStages := Seq(digest)
  )
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test
  )
  .settings(
    Test / unmanagedSourceDirectories += baseDirectory.value / "test-utils",
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-eT")
  )
  .settings(CodeCoverageSettings.settings: _*)
    .settings(
      routesImport ++= Seq(
        "uk.gov.hmrc.devhubsupportfrontend.controllers.models.*",
        "uk.gov.hmrc.apiplatform.modules.common.domain.models.*",
    )
  )
  .settings(
    TwirlKeys.templateImports ++= Seq(
      "uk.gov.hmrc.hmrcfrontend.views.html.components.*",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers.*",
      "uk.gov.hmrc.apiplatform.modules.common.domain.models.*",
      "uk.gov.hmrc.devhubsupportfrontend.config.AppConfig",
      "uk.gov.hmrc.devhubsupportfrontend.controllers"
    )
  )
  .settings(
    scalacOptions ++= Seq(
      "-Wconf:msg=unused import&src=views/.*:s",
      "-Wconf:msg=unused implicit parameter&src=views/.*:s",
      "-Wconf:msg=unused explicit parameter&src=views/.*:s",
      // https://www.scala-lang.org/2021/01/12/configuring-and-suppressing-warnings.html
      // suppress warnings in generated routes files
      "-Wconf:msg=unused import&src=routes/.*:s",
      "-Wconf:msg=unused pattern variable&src=routes/.*:s",
      "-Wconf:msg=unused private member&src=routes/.*:s",
      "-Wconf:msg=unused import&src=html/.*:s"
    )
  )

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.it)

commands ++= Seq(
  Command.command("cleanAll") { state => "clean" :: "it/clean" :: state },
  Command.command("fmtAll") { state => "scalafmtAll" :: "it/scalafmtAll" :: state },
  Command.command("fixAll") { state => "scalafixAll" :: "it/scalafixAll" :: state },
  Command.command("testAll") { state => "test" :: "it/test" :: state },
  Command.command("run-all-tests") { state => "testAll" :: state },
  Command.command("clean-and-test") { state => "cleanAll" :: "compile" :: "run-all-tests" :: state },
  Command.command("pre-commit") { state => "cleanAll" :: "fmtAll" :: "fixAll" :: "coverage" :: "testAll" :: "coverageOff" :: "coverageAggregate" :: state }
)
