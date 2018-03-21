/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.statepension.domain.nps

object Country {

  final val GREAT_BRITAIN = 1
  final val ISLE_OF_MAN = 7
  final val ENGLAND = 114
  final val SCOTLAND = 115
  final val WALES = 116
  final val NORTHERN_IRELAND = 8
  final val NOT_SPECIFIED = 0

  def isAbroad(countryCode: Int): Boolean = {
    countryCode match {
      case NOT_SPECIFIED => false
      case GREAT_BRITAIN => false
      case ENGLAND => false
      case SCOTLAND => false
      case WALES => false
      case NORTHERN_IRELAND => false
      case ISLE_OF_MAN => false
      case _ => true
    }
  }

}
