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
import com.lastroundapp.services.FoursquareClient.VenueSearchQuery

class FoursquareClientImpl(val log: LoggingAdapter) extends FoursquareClient
                                                    with LoggablePipeline {

  import VenueHours.VenueHoursJSONProtocol._
  import VenueJSONProtocol._
  import FSResponseJsonProtocol._

  override def venueSearch(q:VenueSearchQuery)(implicit ac:ActorContext, ec:ExecutionContext): VenueSearchResponse = {
    val fRes    = pipeline[VenueSearchResponse](Get(venueSearchUri(q)))
    val timeout = Settings.venueSearcherTimeout
    Await.result(fRes, timeout.millis)
  }

  override def venueHours(vid:VenueId, token: AccessToken)(implicit ac:ActorContext, ec:ExecutionContext): VenueHoursResponse = {
    val fVh     = pipeline[VenueHoursResponse](Get(venueHoursUri(vid, token)))
    val timeout = Settings.venueHoursWorkerTimeout
    Await.result(fVh, timeout.millis)
  }

  private def venueHoursUri(vid: VenueId, token: AccessToken) =
    uriWithToken(token, new VenueHoursEndpoint((vid)))

  private def venueSearchUri(q:VenueSearchQuery) =
    uriWithToken(q.token, new VenueSearchEndpoint(q.ll))

  private def uriWithToken[T: EndpointUri](token: AccessToken, ep: T): Uri =
    toUri(new AuthenticatedEndpoint(ep, token))
}
