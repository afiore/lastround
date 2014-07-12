package com.lastroundapp.actors

import akka.actor._

import com.lastroundapp.data.Responses._
import com.lastroundapp.data._
import com.lastroundapp.services.FoursquareClient.VenueSearchQuery
import com.lastroundapp.services.{FoursquareClient}

import com.lastroundapp.data.VenueHours.{ClosingTime, VenueOpeningHours}
import com.lastroundapp.data.Endpoints.AccessToken
import org.joda.time.DateTime

object VenueHoursWorker {
  type VenueHoursResponse = FoursquareResponse[VenueOpeningHours]
  case class GetVenueHoursFor(vid:VenueId, q: VenueSearchQuery)
  case class GotVenueHoursFor(vid:VenueId, vhs: Option[ClosingTime]) {
    def isBlank = vhs.isEmpty
  }
}

class VenueHoursWorker(val fsClient: FoursquareClient) extends Actor
                                                       with ActorLogging
                                                       with ResponseHandler {
  import VenueHoursWorker._
  import context.dispatcher

  def receive: Receive = {
    case GetVenueHoursFor(vid, q: VenueSearchQuery) =>
      okOrElse(fsClient.venueHours(vid, q.token)) { vhs =>
        sender ! GotVenueHoursFor(vid, vhs.closingTimeAfter(q.dateTime))
      } { err =>
        log.warning(s"a Foursquare API error occurred: {}", err)
        sender ! GotVenueHoursFor(vid, None)
      }
  }
}
