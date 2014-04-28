package com.lastroundapp.services

import scala.concurrent._
import scala.concurrent.duration._

import akka.actor._
import akka.event.Logging

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

class VenueSearcher extends Actor {
  import VenueSearcher._
  import context.dispatcher

  val log = Logging(context.system, this)

  def receive: Receive = {
    case RunSearch(ll) => {
      val fRes = pipeline(Get(endpointUri(ll)))
      val res  = Await.result(fRes, 5.seconds).asInstanceOf[SearchResult]
      sender ! GotResult(res)
    }
  }

  private def endpointUri(ll:LatLon) =
    toUri(new AuthenticatedEndpoint(new VenueSearchEndpoint(ll)))

  private val logRequest: HttpRequest => HttpRequest =
    { r => log.info(s"Issuing request: $r"); r }

  private val logResponse: SearchResult => SearchResult =
    { sr => log.info(s"Got response: $sr"); sr }

  private val pipeline =
    (encode(Gzip)
      ~> addHeader(Accept(`application/json`))
      ~> logRequest
      ~> sendReceive
      ~> decode(Deflate)
      ~> unmarshal[SearchResult]
      ~> logResponse)
}
