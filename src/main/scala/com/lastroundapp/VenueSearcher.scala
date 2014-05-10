package com.lastroundapp.services

import scala.concurrent._
import scala.concurrent.duration._

import akka.actor._
import akka.event.Logging

import spray.client.pipelining.Get

import com.lastroundapp.Settings
import com.lastroundapp.data.Endpoints._
import com.lastroundapp.data.Responses._
import com.lastroundapp.data._

import VenueHours.VenueOpeningHours

object VenueSearcher {
  type SearchResult  = FoursquareResponse[List[Venue]]
  type VenueHoursMap = scala.collection.immutable.HashMap[VenueId, VenueOpeningHours]

  case class RunSearch(ll:LatLon)
  case class GotVenuesWithOpeningHours(results: List[VenueWithOpeningHours])
  case object VenueHoursTimedout
}

class VenueSearcher(workerPool:ActorRef) extends Actor with Stash
                                         with ActorLogging
                                         with LoggablePipeline {
  import context.dispatcher
  import VenueSearcher._
  import VenueHoursWorker._
  import VenueJSONProtocol._
  import FSResponseJsonProtocol._

  private var venueHours = new VenueHoursMap

  def handleRunSearch: Receive = {
    case RunSearch(ll) => {

      okOrLogError(runSearch(ll)) { vs =>
        vs.foreach { venue => workerPool ! GetVenueHoursFor(venue.id) }
        context.system.scheduler.scheduleOnce(searcherTimeout(vs), self, VenueHoursTimedout)
        context.become(handleVenueHours(sender, vs))
      }
    }

    case GotVenueHoursFor(vid, _) =>
      log.warning(s"Received outdated GotVenueHoursFor message for: $vid")
  }

  def handleVenueHours(recipient:ActorRef, vs:List[Venue]): Receive = {
    case rs:RunSearch =>
      stash()
    case VenueHoursTimedout => {
      log.warning("received VenueHoursTimedout!")
      cleanupAndSendResponse(recipient, vs)
    }

    case GotVenueHoursFor(vid, oVh) => {
      venueHours = venueHours + (vid -> oVh.getOrElse(VenueOpeningHours.empty))

      if (venueHoursIsComplete(vs))
        cleanupAndSendResponse(recipient, vs)
    }
  }

  def receive = handleRunSearch

  private def searcherTimeout(vs:List[Venue]):FiniteDuration =
    (vs.size * Settings.venueHoursWorkerTimeout).seconds

  private def addOpeningHours(vs:List[Venue]):List[VenueWithOpeningHours] =
    vs.map { v => v.withOpeningHours(venueHours.get(v.id)) }

  private def venueHoursIsComplete(vs:List[Venue]):Boolean =
    venueHours.keys == vs.map(_.id).toSet

  private def endpointUri(ll:LatLon) =
    toUri(new AuthenticatedEndpoint(new VenueSearchEndpoint(ll)))

  private def runSearch(ll:LatLon): SearchResult = {
    val fRes    = pipeline[SearchResult](Get(endpointUri(ll)))
    val timeout = Settings.venueSearcherTimeout
    Await.result(fRes, timeout.seconds).asInstanceOf[SearchResult]
  }

  private def cleanupAndSendResponse(recipient: ActorRef, vs:List[Venue]) = {
    log.info(s"Got venues: ${vs.size}, venueHours: ${venueHours.size}")
    recipient ! GotVenuesWithOpeningHours(addOpeningHours(vs))
    venueHours = new VenueHoursMap
    context.become(handleRunSearch)
  }
}
