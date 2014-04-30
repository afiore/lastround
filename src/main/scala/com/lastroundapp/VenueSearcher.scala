package com.lastroundapp.services

import scala.concurrent._
import scala.concurrent.duration._

import akka.actor._

import spray.http._
import spray.httpx.encoding.{Gzip, Deflate}
import spray.client.pipelining._

import HttpMethods._
import HttpHeaders._
import MediaTypes._

import com.lastroundapp.data.Endpoints._
import com.lastroundapp.data.Responses._
import com.lastroundapp.data._

import VenueJSONProtocol._
import FSResponseJsonProtocol._

//import akka.util.duration._
//import akka.util.Timeout

object VenueSearcher {
  type SearchResult = FoursquareResponse[List[Venue]]

  case class RunSearch(ll:LatLon)
  case class GotResult(venueRes:SearchResult)
}

class VenueSearcher extends Actor with ActorLogging
                                  with LoggeedPipeline[VenueSearcher.SearchResult] {
  import VenueSearcher._
  import context.dispatcher

  def receive: Receive = {
    case RunSearch(ll) => {
      val fRes = pipeline[SearchResult](Get(endpointUri(ll)))
      val res  = Await.result(fRes, 5.seconds).asInstanceOf[SearchResult]
      sender ! GotResult(res)
    }
  }

  private def endpointUri(ll:LatLon) =
    toUri(new AuthenticatedEndpoint(new VenueSearchEndpoint(ll)))
}
