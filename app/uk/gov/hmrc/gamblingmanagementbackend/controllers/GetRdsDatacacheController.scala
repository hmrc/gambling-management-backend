/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.gamblingmanagementbackend.controllers

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

import play.api.mvc.Action
import play.api.mvc.ControllerComponents
import play.api.mvc.Results.InternalServerError
import play.api.mvc.Results.{NotFound, Ok}
import uk.gov.hmrc.gamblingmanagementbackend.connector.RdsDatacacheProxyConnector
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import play.api.libs.json.Json

@Singleton()
class GetRdsDatacacheController @Inject() (
                                            val cc: ControllerComponents,
                                            RdsDatacacheProxyConnector: RdsDatacacheProxyConnector
                                          )(using ExecutionContext)
 {
   given headerCarrier(using request: RequestHeader): HeaderCarrier =
     HeaderCarrierConverter.fromRequest(request)

  final def getReturnSummary(mgdRegNumber: String): Action[String] =
      
      RdsDatacacheProxyConnector
        .getReturnSummary(mgdRegNumber)
        .map {
          case Some(Seq(mgdRegNumber, returnsDue, returnsOverdue)) =>
            Ok(
              Json.obj(
                "mgdRegNumber" -> mgdRegNumber,
                "returnsDue" -> returnsDue,
                "returnsOverdue" -> returnsOverdue
              )
            )

          case Some(other) =>
            NotFound(
              Json.obj(
                "errorMessage" -> s"Expected 3 fields but found ${other.size}",
                "errorCode" -> "TBD"
              )
            )

          case None =>
            NotFound(
              Json.obj(
                "errorMessage" -> "Required fields not found",
                "errorCode" -> "TBD"
              )
            )
        }
        .recover { case e =>
          InternalServerError(
            Json.obj(
              "errorMessage" -> e.getMessage,
              "errorCode" -> "rdsDATACACHE_PROXY_ERROR"
            )
          )
        }
    }