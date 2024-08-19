import sbt._

object AppDependencies {

  private val bootstrapVersion = "8.4.0"
  private val hmrcMongoVersion = "1.7.0"
  private val apiDomainVersion = "0.16.0"
  private val appDomainVersion = "0.55.0"
  private val tpdDomainVersion = "0.5.0"

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-frontend-play-30"             % bootstrapVersion,
    "uk.gov.hmrc"             %% "play-frontend-hmrc-play-30"             % "10.6.0",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-30"                     % hmrcMongoVersion,
    "uk.gov.hmrc"             %% "emailaddress-play-30"                   % "4.0.0",
    "uk.gov.hmrc"             %% "http-metrics"                           % "2.8.0",
    "uk.gov.hmrc"             %% "api-platform-api-domain"                % apiDomainVersion,
    "uk.gov.hmrc"             %% "api-platform-application-domain"        % appDomainVersion,
    "uk.gov.hmrc"             %% "api-platform-tpd-domain"                % tpdDomainVersion
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"                 % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"                % hmrcMongoVersion,
    "org.jsoup"               %  "jsoup"                                  % "1.13.1",
    "uk.gov.hmrc"             %% "api-platform-test-api-domain"           % apiDomainVersion,
    "uk.gov.hmrc"             %% "api-platform-test-tpd-domain"           % tpdDomainVersion
  ).map(_ % "test")

  val it = Seq.empty
}
