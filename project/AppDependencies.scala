import sbt._

object AppDependencies {

  private val bootstrapVersion = "9.18.0"
  private val hmrcMongoVersion = "2.7.0"
  private val apiDomainVersion = "0.19.1"
  private val tpdDomainVersion = "0.13.0"

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-frontend-play-30"             % bootstrapVersion,
    "uk.gov.hmrc"             %% "play-frontend-hmrc-play-30"             % "12.8.0",
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

  val it = Seq.empty
}
