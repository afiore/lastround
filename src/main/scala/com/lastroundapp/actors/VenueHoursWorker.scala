package com.lastroundapp.actors

import akka.actor._

import com.lastroundapp.data.Responses._
import com.lastroundapp.data._
import com.lastroundapp.services.{FoursquareClient}

import VenueHours.VenueOpeningHours

object VenueHoursWorker {
  type VenueHoursResponse = FoursquareResponse[VenueOpeningHours]
  case class GetVenueHoursFor(vid:VenueId)
  case class GotVenueHoursFor(vid:VenueId, vhs:Option[VenueOpeningHours])
}

class VenueHoursWorker(val fsClient: FoursquareClient) extends Actor
                                                       with ActorLogging
                                                       with ResponseHandler {

  import VenueHoursWorker._
  import context.dispatcher

  def receive: Receive = {
    case GetVenueHoursFor(vid) =>
      okOrElse(fsClient.venueHours(vid))(vhs => sender ! GotVenueHoursFor(vid, Some(vhs))){ err =>
        log.warning(s"a Foursquare API error occurred: {}", err)
        sender ! GotVenueHoursFor(vid, None)
      }
  }
}