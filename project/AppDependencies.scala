import sbt._

object AppDependencies {

  private val bootstrapVersion    = "10.8.0"
  private val hmrcMongoVersion    = "2.14.0"
  private val commonDomainVersion = "1.4.0"
  private val apiDomainVersion    = "1.8.0"
  private val tpdDomainVersion    = "1.4.0"

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-frontend-play-30"             % bootstrapVersion,
    "uk.gov.hmrc"             %% "play-frontend-hmrc-play-30"             % "13.14.0",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-30"                     % hmrcMongoVersion,
    "commons-validator"       %  "commons-validator"                      % "1.9.0",
    "uk.gov.hmrc"             %% "http-metrics"                           % "2.9.0",
    "uk.gov.hmrc"             %% "api-platform-common-domain"             % commonDomainVersion,
    "uk.gov.hmrc"             %% "api-platform-api-domain"                % apiDomainVersion,
    "uk.gov.hmrc"             %% "api-platform-tpd-domain"                % tpdDomainVersion,
    "uk.gov.hmrc"             %% "play-conditional-form-mapping-play-30"  % "3.5.0"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"                 % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"                % hmrcMongoVersion,
    "org.jsoup"               %  "jsoup"                                  % "1.22.1",
    "uk.gov.hmrc"             %% "api-platform-common-domain-fixtures"    % commonDomainVersion,
    "uk.gov.hmrc"             %% "api-platform-api-domain-fixtures"       % apiDomainVersion,
    "uk.gov.hmrc"             %% "api-platform-tpd-domain-fixtures"       % tpdDomainVersion
  ).map(_ % "test")

  val it = Seq.empty
}
