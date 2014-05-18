package com.lastroundapp.services

import com.lastroundapp.data.{FSToken, VenueId, VenueHoursResponse, VenueSearchResponse}
import com.lastroundapp.data.Endpoints.{AccessToken, LatLon}
import akka.actor.ActorContext
import scala.concurrent.ExecutionContext
import com.lastroundapp.services.FoursquareClient.VenueSearchQuery

object FoursquareClient {
  case class VenueSearchQuery(ll:LatLon, token:AccessToken)
}

abstract class FoursquareClient {
  def venueSearch(q:VenueSearchQuery)(implicit ac:ActorContext, ec:ExecutionContext): VenueSearchResponse
  def venueHours(vid:VenueId, token:AccessToken)(implicit ac:ActorContext, ec:ExecutionContext): VenueHoursResponse
}