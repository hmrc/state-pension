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

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-backend-play-27" % "5.2.0",
    "uk.gov.hmrc" %% "domain" % "5.10.0-play-27",
    "uk.gov.hmrc" %% "play-hmrc-api" % "6.2.0-play-27",
    "uk.gov.hmrc" %% "play-hal" % "2.1.0-play-27",
    "uk.gov.hmrc" %% "time" % "3.19.0",
    "com.typesafe.play" %% "play-json-joda" % "2.9.2",
    "com.jsuereth" %% "scala-arm" % "2.0",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-27" % "0.49.0"
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % "3.0.9",
    "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3",
    "org.mockito" % "mockito-core" % "2.10.0",
    "com.github.tomakehurst" % "wiremock-jre8" % "2.27.2",
    "org.pegdown" % "pegdown" % "1.6.0"
  ).map(_ % Test)

  val all: Seq[ModuleID] = compile ++ test

}
