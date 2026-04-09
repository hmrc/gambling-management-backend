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

package uk.gov.hmrc.gamblingmanagementbackend.connector

import com.google.inject.ImplementedBy
import org.apache.pekko.actor.ActorSystem
import play.api.Configuration
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URL
import javax.inject.Inject
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json.JsObject

@ImplementedBy(classOf[RdsDatacacheProxyConnectorImpl])
trait RdsDatacacheProxyConnector {
  type MgdRegNumber   = String

  /** Get return summary
   *
   * @param MgdRegNumber the registration number
   * @return
   *   Return Summary
   */
  def getReturnSummary(mgdRegNumber: MgdRegNumber)(using
                                                   hc: HeaderCarrier
  ): Future[Option[String]]


class RdsDatacacheProxyConnectorImpl @Inject() (
                                                 http: HttpClientV2,
                                                 configuration: Configuration,
                                                 servicesConfig: ServicesConfig,
                                                 val actorSystem: ActorSystem
                                               )(using
                                                 ExecutionContext
                                               ) extends RdsDatacacheProxyConnector
{

  val baseUrl: String = servicesConfig.baseUrl("rds-datacache-proxy")

  val contextPath: String = servicesConfig
    .getConfString("rds-datacache-proxy.context-path", "rds-datacache-proxy")

  final def getReturnSummary(mgdRegNumber: MgdRegNumber)(using
                                                         hc: HeaderCarrier
  ): Future[Option[String]] =
      http
        .get(URL(s"$baseUrl$contextPath/mgd/reg-number"))
        .execute[HttpResponse]
    ).flatMap(response =>
      if response.status == 200
      then
        Future {
          response.json
            .asOpt[JsObject]
            .map { obj =>
              Seq("mgdRegNumber", "returnsDue", "returnsOverdue")
                .flatMap(key => obj.value.get(key).flatMap(_.asOpt[String]))
            }
        }
      else if response.status == 404
      then Future.successful(None)
      else
        Future.failed(
          Exception(
            s"Request to GET $baseUrl$contextPath/mgd/reg-number failed because of $response ${response.body}"
          )
        )
    )

}