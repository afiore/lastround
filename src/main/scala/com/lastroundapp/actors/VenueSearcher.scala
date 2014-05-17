package com.lastroundapp.actors

import scala.concurrent.duration._

import akka.actor._

import com.lastroundapp.Settings
import com.lastroundapp.data.Endpoints._
import com.lastroundapp.data.Responses._
import com.lastroundapp.data._

import VenueHours.VenueOpeningHours
import com.lastroundapp.services.FoursquareClient

object VenueSearcher {
  type SearchResult  = FoursquareResponse[List[Venue]]
  type VenueHoursMap = scala.collection.immutable.HashMap[VenueId, VenueOpeningHours]

  case class RunSearch(ll:LatLon)
  case class GotVenuesWithOpeningHours(results: List[VenueWithOpeningHours])
  case object VenueHoursTimedOut
}

class VenueSearcher(
    val fsClient:FoursquareClient,
    val workerPool:ActorRef) extends Actor with ActorLogging
                                           with Stash
                                           with ResponseHandler {
  import context.dispatcher
  import VenueSearcher._
  import VenueHoursWorker._

  private var venueHours = new VenueHoursMap

  def handleRunSearch: Receive = {
    case RunSearch(ll) =>
      okOrLogError(fsClient.venueSearch(ll)) { vs =>
        vs.foreach { venue => workerPool ! GetVenueHoursFor(venue.id) }
        context.system.scheduler.scheduleOnce(searcherTimeout(vs), self, VenueHoursTimedOut)
        context.become(handleVenueHours(sender(), vs))
      }
    case GotVenueHoursFor(vid, _) =>
      log.warning(s"Received outdated GotVenueHoursFor message for: $vid")
  }

  def handleVenueHours(recipient:ActorRef, vs:List[Venue]): Receive = {
    case rs:RunSearch =>
      log.debug("stashing {}", rs)
      stash()
    case VenueHoursTimedOut =>
      log.warning("received VenueHoursTimedOut!")
      cleanupAndSendResponse(recipient, vs)
    case GotVenueHoursFor(vid, oVh) =>
      log.debug("Got venue for {}", vid)
      venueHours = venueHours + (vid -> oVh.getOrElse(VenueOpeningHours.empty))
      if (venueHoursIsComplete(vs)) cleanupAndSendResponse(recipient, vs)
  }

  def receive = handleRunSearch

  private def searcherTimeout(vs:List[Venue]):FiniteDuration =
    (vs.size * Settings.venueHoursWorkerTimeout).millis

  private def addOpeningHours(vs:List[Venue]):List[VenueWithOpeningHours] =
    vs.map { v => v.withOpeningHours(venueHours.get(v.id)) }

  private def venueHoursIsComplete(vs:List[Venue]):Boolean =
    venueHours.keys == vs.map(_.id).toSet

  private def cleanupAndSendResponse(recipient: ActorRef, vs:List[Venue]) = {
    log.info(s"Got venues: ${vs.size}, venueHours: ${venueHours.size}")
    recipient ! GotVenuesWithOpeningHours(addOpeningHours(vs))
    venueHours = new VenueHoursMap
    log.debug("Unstashing all...")
    unstashAll()
    context.become(handleRunSearch)
  }
}
