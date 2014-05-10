package com.lastroundapp.services

import scala.concurrent._
import scala.concurrent.duration._

import akka.actor._
import akka.event.Logging

import spray.httpx.encoding.{Gzip, Deflate}
import spray.client.pipelining._

import com.lastroundapp.data.Endpoints._
import com.lastroundapp.data.Responses._
import com.lastroundapp.data._

import VenueHours.VenueOpeningHours
import VenueJSONProtocol._
import FSResponseJsonProtocol._

object VenueSearcher {
  type SearchResult = FoursquareResponse[List[Venue]]

  case class RunSearch(ll:LatLon)
  case class VenueHoursFor(vid:VenueId, vh:Option[VenueOpeningHours])
  case class GotResult(venueRes:SearchResult)
}

class VenueSearcher extends Actor with ActorLogging
                                  with LoggablePipeline {
  import VenueSearcher._
  import context.dispatcher
  import spray.httpx.unmarshalling.Deserializer.fromFunction2Converter

  def receive: Receive = {
    case RunSearch(ll) => {
      val res = runSearch(ll)
      sender ! GotResult(res)
    }

    case VenueHoursFor(vid, vhs) => {
    }
  }

  private def runSearch(ll:LatLon): SearchResult = {
    val fRes = pipeline(Get(endpointUri(ll)))
    Await.result(fRes, 5.seconds).asInstanceOf[SearchResult]
  }

  private def endpointUri(ll:LatLon) =
    toUri(new AuthenticatedEndpoint(new VenueSearchEndpoint(ll)))
}
