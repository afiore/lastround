package com.lastroundapp.services

import scala.concurrent._
import scala.concurrent.duration._

import akka.actor._

import spray.client.pipelining.Get

import com.lastroundapp.Settings
import com.lastroundapp.data.Endpoints._
import com.lastroundapp.data.Responses._
import com.lastroundapp.data._

import VenueHours.VenueOpeningHours

object VenueHoursWorker {
  type VenueHoursResponse = FoursquareResponse[VenueOpeningHours]
  case class GetVenueHoursFor(vid:VenueId)
  case class GotVenueHoursFor(vid:VenueId, vhs:Option[VenueOpeningHours])
}

class VenueHoursWorker extends Actor with ActorLogging
                               with LoggablePipeline {

  import VenueHoursWorker._
  import context.dispatcher
  import VenueHours.VenueHoursJSONProtocol._
  import FSResponseJsonProtocol._

  def receive: Receive = {
    case GetVenueHoursFor(vid) =>
      okOrElse(fetchVenueHours(vid))(vhs => sender ! GotVenueHoursFor(vid, Some(vhs))){ err =>
        log.warning(s"a Foursquare API error occurred: $err")
        sender ! GotVenueHoursFor(vid, None)
      }
  }

  private def fetchVenueHours(vid:VenueId):VenueHoursResponse = {
    val fVh     = pipeline[VenueHoursResponse](Get(endpointUri(vid)))
    val timeout = Settings.venueHoursWorkerTimeout
    Await.result(fVh, timeout.seconds)
  }

  private def endpointUri(vid:VenueId) =
    toUri(new AuthenticatedEndpoint(new VenueHoursEndpoint(vid)))

}
