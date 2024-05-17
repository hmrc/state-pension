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
  val bootstrapVersion = "8.5.0"
  val playVersion = "play-30"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %%  s"bootstrap-backend-$playVersion"             % bootstrapVersion,
    "uk.gov.hmrc"       %%  s"domain-$playVersion"                        % "9.0.0",
    "uk.gov.hmrc"       %%  s"play-hmrc-api-$playVersion"                 % "8.0.0",
    "uk.gov.hmrc"       %%  s"play-hal-$playVersion"                      % "4.0.0",
    "uk.gov.hmrc"       %%  s"mongo-feature-toggles-client-$playVersion"  % "1.3.0",
    "commons-codec"     %   "commons-codec"                               % "1.16.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %%  s"bootstrap-test-$playVersion"  % bootstrapVersion,
    "org.mockito"             %   "mockito-core"                  % "4.7.0",
    "org.pegdown"             %   "pegdown"                       % "1.6.0",
    "de.flapdoodle.embed"     % "de.flapdoodle.embed.mongo"       % "3.5.3",
    "uk.gov.hmrc"             %%  s"play-hal-$playVersion"        % "4.0.0"
  ).map(_ % Test)

  val all: Seq[ModuleID] = compile ++ test

}