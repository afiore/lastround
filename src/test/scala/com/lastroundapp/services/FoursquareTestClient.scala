package com.lastroundapp.services

import com.lastroundapp.data.Endpoints.{LatLon, AccessToken}
import akka.actor.ActorContext
import scala.concurrent.ExecutionContext
import com.lastroundapp.data._
import com.lastroundapp.data.Responses.{ResponseError,NotAuthorised,RateLimitExceeded, ResponseOK}
import VenueHours._
import com.lastroundapp.services.FoursquareClient.{VenueSearchQuery, DayWithTime}

object FoursquareTestClient {
  val venue1         =
    Venue(
      VenueId("venue-1"),
      "venue-1",
      Location(
        Some("Address1"),
        51.545, 0.0553, 0, None,
        "UK", "United Kingdom", None),
      None)

  val venue2         =
    Venue(
      VenueId("venue-2"),
      "venue-2",
      Location(
        Some("Address2"),
        52.545, 1.0553, 0, None,
        "UK", "United Kingdom", None),
      None)

  val vidFailure = VenueId("failing-venue")
  val vidSuccess = VenueId("success-venue")

  val ll = LatLon(45.0, 50.0)
  val venueSearchQuerySuccess = VenueSearchQuery(ll, AccessToken.default, DayWithTime.now)
  val venueSearchQueryFailure = VenueSearchQuery(ll, AccessToken("wrong-token"), DayWithTime.now)

  val venueHours1 =
    VenueOpeningHours(
      List(),
      List(
        TimeFrame(Set(Monday),
        List(OpeningTime(
          TimeOfDay(22,0),
          TimeOfDay(3,0))))))
}

class FoursquareTestClient extends FoursquareClient {
  import FoursquareTestClient._
  import VenueJSONProtocol._
  import VenueHoursJSONProtocol._

  def venueSearch(q:VenueSearchQuery)(implicit ac:ActorContext, ec:ExecutionContext): VenueSearchResponse =
    if (q == venueSearchQuerySuccess) ResponseOK(List(venue1, venue2)) else ResponseError(NotAuthorised("bad token"))

  def venueHours(vid:VenueId, token:AccessToken)(implicit ac:ActorContext, ec:ExecutionContext): VenueHoursResponse =
    if (vid == vidSuccess) ResponseOK(venueHours1) else ResponseError(RateLimitExceeded("Take it easy..."))
 }
