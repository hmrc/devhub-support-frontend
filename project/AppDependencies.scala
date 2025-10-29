import sbt._

object AppDependencies {

  private val bootstrapVersion = "9.19.0"
  private val hmrcMongoVersion = "2.10.0"
  private val apiDomainVersion = "0.20.0"
  private val tpdDomainVersion = "0.14.0"

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-frontend-play-30"             % bootstrapVersion,
    "uk.gov.hmrc"             %% "play-frontend-hmrc-play-30"             % "12.17.0",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-30"                     % hmrcMongoVersion,
    "commons-validator"       %  "commons-validator"                      % "1.9.0",
    "uk.gov.hmrc"             %% "http-metrics"                           % "2.9.0",
    "uk.gov.hmrc"             %% "api-platform-api-domain"                % apiDomainVersion,
    "uk.gov.hmrc"             %% "api-platform-tpd-domain"                % tpdDomainVersion,
    "uk.gov.hmrc"             %% "play-conditional-form-mapping-play-30"  % "3.3.0"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"                 % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"                % hmrcMongoVersion,
    "org.jsoup"               %  "jsoup"                                  % "1.18.1",
    "uk.gov.hmrc"             %% "api-platform-test-api-domain"           % apiDomainVersion,
    "uk.gov.hmrc"             %% "api-platform-test-tpd-domain"           % tpdDomainVersion
  ).map(_ % "test")

  val it = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % "it",
    "org.seleniumhq.selenium" %  "selenium-java"     % "4.21.0" % "it",
    "org.scalatestplus"      %% "selenium-4-21"      % "3.2.19.0" % "it",
    "com.github.tomakehurst" %  "wiremock-jre8"      % "2.35.2" % "it",
    "uk.gov.hmrc"            %% "api-platform-test-api-domain" % apiDomainVersion % "it",
    "uk.gov.hmrc"            %% "api-platform-test-tpd-domain" % tpdDomainVersion % "it",
    "org.mockito"            %  "mockito-core"       % "5.14.2" % "it",
    "org.scalatestplus"      %% "mockito-4-11"       % "3.2.18.0" % "it"
  )
}
