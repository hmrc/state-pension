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

import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-backend-play-26" % "3.2.0",
    "uk.gov.hmrc" %% "domain" % "5.10.0-play-26",
    "uk.gov.hmrc" %% "play-hmrc-api" % "4.1.0-play-26",
    "uk.gov.hmrc" %% "play-hal" % "2.1.0-play-26",
    "uk.gov.hmrc" %% "auth-client"  %  "3.2.0-play-26"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "3.6.0-play-25",
    "org.scalatest" %% "scalatest" % "3.0.8",
    "org.pegdown" % "pegdown" % "1.6.0",
    "com.typesafe.play" %% "play-test" % PlayVersion.current,
    "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1",
    "org.mockito" % "mockito-all" % "1.10.19",
    "com.github.tomakehurst" % "wiremock" % "2.27.2"
  ).map(_ % Test)

  val all: Seq[ModuleID] = compile ++ test

}
