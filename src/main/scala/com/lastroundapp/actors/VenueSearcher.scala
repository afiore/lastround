package com.lastroundapp.actors

import scala.concurrent.duration._

import akka.actor._
import akka.routing.FromConfig
import com.lastroundapp.data.Responses._
import com.lastroundapp.data._

import VenueHours.VenueOpeningHours
import com.lastroundapp.services.FoursquareClient
import FoursquareClient.VenueSearchQuery

object VenueSearcher {
  def buildPool(fsClient:FoursquareClient)(implicit system:ActorSystem): ActorRef = {
    val workerPool = system.actorOf(
      FromConfig.props(Props(classOf[VenueHoursWorker], fsClient)),
      "venue-hours-router")

    system.actorOf(
      FromConfig.props(Props(classOf[VenueSearcher], fsClient, workerPool)),
      "venue-search-router")
  }

  type SearchResult  = FoursquareResponse[List[Venue]]
  type VenueHoursMap = scala.collection.immutable.HashMap[VenueId, VenueOpeningHours]

  case class RunSearch(q:VenueSearchQuery)
  case class GotVenuesWithOpeningHours(eResult:Either[String,List[VenueWithOpeningHours]])
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
    case RunSearch(q) =>
      okOrElse(fsClient.venueSearch(q)) { vs:List[Venue] =>
        vs.foreach { venue => workerPool ! GetVenueHoursFor(venue.id, q.token) }
        context.system.scheduler.scheduleOnce(searcherTimeout(vs), self, VenueHoursTimedOut)
        context.become(handleVenueHours(sender(), vs))
      } { err =>
        sender ! GotVenuesWithOpeningHours(Left(err.message))
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
    recipient ! GotVenuesWithOpeningHours(Right(addOpeningHours(vs)))
    venueHours = new VenueHoursMap
    log.debug("Unstashing all...")
    unstashAll()
    context.become(handleRunSearch)
  }
}