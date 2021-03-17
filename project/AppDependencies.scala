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
    "uk.gov.hmrc" %% "bootstrap-backend-play-27" % "4.1.0",
    "uk.gov.hmrc" %% "domain" % "5.10.0-play-27",
    "uk.gov.hmrc" %% "play-hmrc-api" % "6.2.0-play-27",
    "uk.gov.hmrc" %% "play-hal" % "2.1.0-play-27",
    "uk.gov.hmrc" %% "auth-client" % "5.1.0-play-27",
    "uk.gov.hmrc" %% "time" % "3.19.0",
    "com.typesafe.play" %% "play-json-joda" % "2.9.2",
    "com.jsuereth" %% "scala-arm" % "2.0"
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % "3.0.8",
    "com.typesafe.play" %% "play-test" % "2.8.7",
    "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3",
    "org.mockito" % "mockito-all" % "1.10.19",
    "com.github.tomakehurst" % "wiremock-jre8" % "2.27.2",
    "org.pegdown" % "pegdown" % "1.6.0"
  ).map(_ % Test)

  val all: Seq[ModuleID] = compile ++ test

}
