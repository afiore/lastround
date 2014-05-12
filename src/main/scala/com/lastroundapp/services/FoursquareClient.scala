package com.lastroundapp.services

import com.lastroundapp.data.{VenueId, VenueHoursResponse, VenueSearchResponse}
import com.lastroundapp.data.Endpoints.LatLon
import akka.actor.ActorContext
import scala.concurrent.ExecutionContext

abstract class FoursquareClient {
  def venueSearch(ll:LatLon)(implicit ac:ActorContext, ec:ExecutionContext): VenueSearchResponse
  def venueHours(vid:VenueId)(implicit ac:ActorContext, ec:ExecutionContext): VenueHoursResponse
}