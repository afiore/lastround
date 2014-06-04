package com.lastroundapp.actors

import scala.concurrent.duration._
import akka.actor._
import spray.can.Http
import spray.http._
import spray.http.MediaTypes._
import spray.http.ChunkedResponseStart
import spray.http.HttpResponse
import spray.json._

import com.lastroundapp.services.FoursquareClient.VenueSearchQuery

class ResultStreamer(
    q: VenueSearchQuery,
    venueSearcher: ActorRef,
    responder: ActorRef) extends Actor with ActorLogging {

  import VenueSearcher._
  import DefaultJsonProtocol._
  import com.lastroundapp.data.VenueHours.VenueHoursJSONProtocol._

  private case object ResponderTimedOut
  sealed case class ServerSentEvent[T: JsonFormat](evType:String, data:T) {
    def serialize:String = {
      s"""event: $evType
        |data: ${data.toJson.compactPrint}
        |
      """.stripMargin
    }
  }

  val EventStreamType = register(
    MediaType.custom(
      mainType = "text",
      subType  = "stream",
      compressible = true,
      binary = false,
      fileExtensions = Seq()
    ))

  //private val header = (1 to 1024).map(_ => "\uFEFF").mkString("")

  implicit val ec = context.dispatcher

  venueSearcher ! RunSearch(q)

  responder ! ChunkedResponseStart(HttpResponse(entity = HttpEntity(EventStreamType, "\n\n")))
  context.system.scheduler.scheduleOnce(30.seconds, self, ResponderTimedOut)

  def receive = {
    case ResponderTimedOut =>
      responder ! ChunkedMessageEnd
      context.stop(self)

    case GotVenuesWithOpeningHours(Right(vh)) =>
      responder ! MessageChunk(ServerSentEvent("result", vh).serialize)
      responder ! ChunkedMessageEnd
      context.stop(self)

    case GotVenuesWithOpeningHours(Left(msg)) =>
      responder ! MessageChunk(s"Got a problem: ${msg}")
      responder ! ChunkedMessageEnd
      context.stop(self)

    case ev: Http.ConnectionClosed =>
      log.debug("connection closed")
      context.stop(self)
  }
}