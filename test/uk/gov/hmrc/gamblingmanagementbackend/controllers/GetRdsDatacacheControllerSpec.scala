package controllers

import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.*
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import play.api.mvc.*

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.gamblingmanagementbackend.connector.RdsDatacacheProxyConnector
import uk.gov.hmrc.gamblingmanagementbackend.controllers.GetRdsDatacacheController
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.SystemMaterializer
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.shouldBe


class GetRdsDatacacheControllerSpec
  extends AnyWordSpec
    with Matchers
    with MockitoSugar {

  implicit val ec: ExecutionContext = ExecutionContext.global

  val mockConnector  = mock[RdsDatacacheProxyConnector]

  given HeaderCarrier = HeaderCarrier()

  given ActorSystem = ActorSystem("test-system")

  given Materializer = SystemMaterializer(given_ActorSystem).materializer


  val controller = new GetRdsDatacacheController(
    Helpers.stubControllerComponents(),
    mockConnector
  )


  "getReturnSummary" should {

    "return 200 OK with JSON when connector returns 3 fields" in {
      val mgdRegNumber = "55661"
      val returnsDue = "1"
      val returnsOverdue = "4"

      given HeaderCarrier = HeaderCarrier()

      when(
        mockConnector.getReturnSummary(
          org.mockito.ArgumentMatchers.eq(mgdRegNumber)
        )(using org.mockito.ArgumentMatchers.any[HeaderCarrier])
      ).thenReturn(
        Future.successful(Some(Seq(mgdRegNumber, returnsDue, returnsOverdue)))
      )

      val request = FakeRequest("POST", s"/return-summary/$mgdRegNumber")
        .withBody("")

      val result = controller.getReturnSummary(mgdRegNumber)(request)

      status(result) shouldBe OK

      contentAsJson(result) shouldBe Json.obj(
        "mgdRegNumber" -> mgdRegNumber,
        "returnsDue" -> returnsDue,
        "returnsOverdue" -> returnsOverdue
      )
    }
  }


  "return 404 NotFound when connector returns a sequence with wrong number of fields" in {
    val mgdRegNumber = "55661"

    when(
      mockConnector.getReturnSummary(
        org.mockito.ArgumentMatchers.eq(mgdRegNumber)
      )(using org.mockito.ArgumentMatchers.any[HeaderCarrier])
    ).thenReturn(
      Future.successful(Some(Seq("only-one-field")))
    )

    val request = FakeRequest("POST", s"/return-summary/$mgdRegNumber").withBody("")
    val result = controller.getReturnSummary(mgdRegNumber)(request)

    status(result) shouldBe NOT_FOUND

    (contentAsJson(result) \ "errorMessage").as[String] shouldBe
      "Expected 3 fields but found 1"
  }


  "return 404 NotFound when connector returns None" in {
    val mgdRegNumber = "55661"

    when(
      mockConnector.getReturnSummary(
        org.mockito.ArgumentMatchers.eq(mgdRegNumber)
      )(using org.mockito.ArgumentMatchers.any[HeaderCarrier])
    ).thenReturn(
      Future.successful(None)
    )

    val request = FakeRequest("POST", s"/return-summary/$mgdRegNumber").withBody("")
    val result = controller.getReturnSummary(mgdRegNumber)(request)

    status(result) shouldBe NOT_FOUND

    (contentAsJson(result) \ "errorMessage").as[String] shouldBe
      "Required fields not found"
  }


  "return 500 InternalServerError when connector throws an exception" in {
    val mgdRegNumber = "55661"

    when(
      mockConnector.getReturnSummary(
        org.mockito.ArgumentMatchers.eq(mgdRegNumber)
      )(using org.mockito.ArgumentMatchers.any[HeaderCarrier])
    ).thenReturn(
      Future.failed(new RuntimeException("Failure"))
    )

    val request = FakeRequest("POST", s"/return-summary/$mgdRegNumber").withBody("")
    val result = controller.getReturnSummary(mgdRegNumber)(request)

    status(result) shouldBe INTERNAL_SERVER_ERROR

    (contentAsJson(result) \ "errorMessage").as[String] shouldBe "Failure"
    (contentAsJson(result) \ "errorCode").as[String] shouldBe "RDS_DATACACHE_PROXY_ERROR"
  }

}

