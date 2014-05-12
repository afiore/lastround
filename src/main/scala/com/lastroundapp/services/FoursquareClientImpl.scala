package com.lastroundapp.services

import akka.actor._
import akka.event.LoggingAdapter

import scala.concurrent._
import scala.concurrent.duration._

import spray.client.pipelining.Get
import spray.http.Uri

import com.lastroundapp.Settings
import com.lastroundapp.data.Endpoints._
import com.lastroundapp.data._
import com.lastroundapp.data.Responses.FSResponseJsonProtocol

class FoursquareClientImpl(val log: LoggingAdapter) extends FoursquareClient
                                                    with LoggablePipeline {

  import VenueHours.VenueHoursJSONProtocol._
  import VenueJSONProtocol._
  import FSResponseJsonProtocol._

  override def venueSearch(ll:LatLon)(implicit ac:ActorContext, ec:ExecutionContext): VenueSearchResponse = {
    val fRes    = pipeline[VenueSearchResponse](Get(venueSearchUri(ll)))
    val timeout = Settings.venueSearcherTimeout
    Await.result(fRes, timeout.seconds)
  }

  override def venueHours(vid:VenueId)(implicit ac:ActorContext, ec:ExecutionContext): VenueHoursResponse = {
    val fVh     = pipeline[VenueHoursResponse](Get(venueHoursUri(vid)))
    val timeout = Settings.venueHoursWorkerTimeout
    Await.result(fVh, timeout.seconds)
  }

  private def venueHoursUri(vid:VenueId) =
    uriWithToken(new VenueHoursEndpoint((vid)))

  private def venueSearchUri(ll:LatLon) =
    uriWithToken(new VenueSearchEndpoint(ll))

  private def uriWithToken[T: EndpointUri](ep: T): Uri =
    toUri(new AuthenticatedEndpoint(ep))
}
