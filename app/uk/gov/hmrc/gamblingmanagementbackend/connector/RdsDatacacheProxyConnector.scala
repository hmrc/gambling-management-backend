package uk.gov.hmrc.gamblingmanagementbackend.connector

import com.google.inject.ImplementedBy
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future


@ImplementedBy(classOf[RdsDatacacheProxyConnectorImpl])
trait RdsDatacacheProxyConnector {

  type mgdRegNumber = String

  def getReturnSummary(mgdRegNumber: mgdRegNumber)(using
                                                   hc: HeaderCarrier
  ): Future[Option[Seq[String]]]
}
