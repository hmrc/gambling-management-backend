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
package uk.gov.hmrc.gamblingmanagementbackend.connectors

import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.*
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.Configuration
import play.api.libs.json.Json
import uk.gov.hmrc.gamblingmanagementbackend.connector.RdsDatacacheProxyConnectorImpl
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import org.scalatest.wordspec.AnyWordSpec
import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.{Materializer, SystemMaterializer}
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class RdsDatacacheProxyConnectorSpec
  extends AnyWordSpec
    with Matchers {

  given HeaderCarrier = HeaderCarrier()

  given ExecutionContext = scala.concurrent.ExecutionContext.global

  given ActorSystem = ActorSystem("test-actor-system")
  given Materializer = SystemMaterializer(given_ActorSystem).materializer

  val mockHttp: HttpClientV2 = mock[HttpClientV2]


  val config: Configuration = Configuration(
    ConfigFactory.parseString(
      """
        |  microservice {
        |    services {
        |      rds-datacache-proxy {
        |        protocol = http
        |        host     = foo.bar.com
        |        port = 5566
        |        context-path = "/foo-proxy"
        |      }
        |   }
        |}
        |""".stripMargin
    )
  )


  val connector =
    new RdsDatacacheProxyConnectorImpl(
      http = mockHttp,
      servicesConfig = new ServicesConfig(config),
      configuration = config,
      actorSystem = given_ActorSystem
    )


  def givenGetReturnSummaryByRegNumberReturns(response: HttpResponse): Unit = {

    val requestBuilder = mock[RequestBuilder]

    when(
      mockHttp.get(
        org.mockito.ArgumentMatchers.eq(
          new java.net.URL("http://foo.bar.com:5566/foo-proxy/")
        )
      )(any[HeaderCarrier])
    ).thenReturn(requestBuilder)

    when(
      requestBuilder.execute[HttpResponse]
    ).thenReturn(Future.successful(response))
  }


  "getReturnSummary" should {

    "return None if the service returns 404 status" in {
      givenGetReturnSummaryByRegNumberReturns(HttpResponse(404))

      await(connector.getReturnSummary("999")) shouldBe None
    }

    "return details for the given reg number" in {
      givenGetReturnSummaryByRegNumberReturns(
        HttpResponse(
          status = 200,
          body = Json.obj("returnSummary" -> "returnDetails").toString()
        )
      )

      await(connector.getReturnSummary("999")) shouldBe Some("returnDetails")
    }

    "throw an exception if the service returns 500 status" in {
      givenGetReturnSummaryByRegNumberReturns(HttpResponse(500))

      an[Exception] should be thrownBy {
        await(connector.getReturnSummary("999"))
      }
    }
  }
}