package com.lastroundapp.actors

import com.lastroundapp.data.Settings
import com.lastroundapp.data.Events._
import com.lastroundapp.services.FoursquareClient
import scala.concurrent.duration._
import scala.language.implicitConversions._

import akka.actor._
import com.lastroundapp.actors.VenueHoursWorker.GotVenueHoursFor
import com.lastroundapp.data.VenueHours.VenueHoursFor
import com.lastroundapp.services.FoursquareClient.VenueSearchQuery
import spray.can.Http
import spray.http._
import spray.http.MediaTypes._
import spray.json._

class ResultStreamer(
    q: VenueSearchQuery,
    venueSearcher: ActorRef,
    responder: ActorRef) extends Actor with ActorLogging {

  import VenueSearcher._
  import ServerEventConversions._

  import com.lastroundapp.data.VenueJSONProtocol._
  import com.lastroundapp.data.Responses.FSResponseJsonProtocol._
  import com.lastroundapp.data.VenueHours.VenueHoursJSONProtocol._
  import com.lastroundapp.services.FoursquareClient.{Format, JsonStream, EventStream}

  import ServerEventJsonProtocol._

  private case object ResponderTimedOut
  implicit val ec = context.dispatcher

  venueSearcher ! RunSearch(q)

  responder ! ChunkedResponseStart(HttpResponse(entity = HttpEntity(q.format.contentType, firstResponseChunk)))
  context.system.scheduler.scheduleOnce(Settings.streamerTimeout.millis, self, ResponderTimedOut)

  def receive = {
    case ResponderTimedOut =>
      responder ! ChunkedMessageEnd
      context.stop(self)

    case GotVenueResults(Right(vh)) =>
      responder ! jsonChunk(vh)

    case GotVenueResults(Left(err)) =>
      responder ! jsonChunk(err)
      responder ! ChunkedMessageEnd
      context.stop(self)

    case GotVenueHoursFor(vid, Some(vhs)) =>
      log.info("Got venueHours {}", vid)
      responder ! jsonChunk(VenueHoursFor(vid, vhs))

    case EndOfVenueHours =>
      responder ! ChunkedMessageEnd
      context.stop(self)

    case ev: Http.ConnectionClosed =>
      log.debug("connection closed")
      context.stop(self)
  }

  private def firstResponseChunk =
    if (q.format == EventStream) "\nretry: 60000\n\n" else "\n\n"

  private def jsonChunk[T: JsonFormat](serverEvent: ServerEvent[T]): MessageChunk =
    MessageChunk(serializeEvent(serverEvent, q.format))

  private def serializeEvent[T: JsonFormat](e: ServerEvent[T], format: Format) = format match {
    case JsonStream  => e.toJson.compactPrint ++ format.lineSeparator
    case EventStream => e.asHtml5Event ++ format.lineSeparator
  }
}
