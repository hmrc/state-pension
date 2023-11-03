/*
 * Copyright 2015 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

import play.sbt.PlayImport._
import sbt._

object AppDependencies {
  val bootstrapVersion = "7.21.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %%  "bootstrap-backend-play-28"     % bootstrapVersion,
    "uk.gov.hmrc"       %%  "domain"                        % "8.3.0-play-28",
    "uk.gov.hmrc"       %%  "play-hmrc-api"                 % "7.2.0-play-28",
    "uk.gov.hmrc"       %%  "play-hal"                      % "3.4.0-play-28",
    "uk.gov.hmrc"       %%  "mongo-feature-toggles-client"  % "0.4.0",
    "commons-codec"     %   "commons-codec"                 % "1.15"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %%  "bootstrap-test-play-28"  % bootstrapVersion,
    "org.mockito"             %   "mockito-core"            % "4.7.0",
    "com.github.tomakehurst"  %   "wiremock-jre8"           % "2.26.3",
    "org.pegdown"             %   "pegdown"                 % "1.6.0",
    "uk.gov.hmrc"             %%  "play-hal"                % "3.4.0-play-28"
  ).map(_ % "test,it")

  val all: Seq[ModuleID] = compile ++ test

}