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

  type SearchResult = FoursquareResponse[List[Venue]]

  case class RunSearch(q:VenueSearchQuery)
  case class GotVenueResults(eResult:Either[ApiError,List[Venue]])
  case object EndOfVenueHours
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

    def handleRunSearch: Receive = {
    case RunSearch(q) =>
      okOrElse(fsClient.venueSearch(q)) { vs:List[Venue] =>
        vs.foreach { venue => workerPool ! GetVenueHoursFor(venue.id, q.token) }
        sender ! GotVenueResults(Right(vs))

        context.system.scheduler.scheduleOnce(searcherTimeout(vs), self, VenueHoursTimedOut)
        context.become(handleVenueHours(sender(), vs.map(_.id).toSet))
      } { err =>
        sender ! GotVenueResults(Left(err))
      }

    case GotVenueHoursFor(vid, _) =>
      log.warning(s"Received outdated GotVenueHoursFor message for: $vid")
    }

    def handleVenueHours(recipient:ActorRef, vIds:Set[VenueId]): Receive = {
      case rs:RunSearch =>
        log.debug("stashing {}", rs)
        stash()

      case VenueHoursTimedOut =>
        log.warning("received VenueHoursTimedOut!")
        respondAndReset(recipient)

      case msg@GotVenueHoursFor(vId, _) =>
        val _vIds = vIds - vId
        sendVenueHoursUnlessEmpty(recipient, msg)

        if (_vIds.isEmpty)
          respondAndReset(recipient)
        else
          context.become(handleVenueHours(recipient, _vIds))
    }

    def receive = handleRunSearch

    private def searcherTimeout(vs:List[_]):FiniteDuration =
      (vs.size * Settings.venueHoursWorkerTimeout).millis

    private def sendVenueHoursUnlessEmpty(recipient: ActorRef, msg: GotVenueHoursFor): Unit = {
      log.debug("Got venue for {}", msg.vid)
      msg.vhs match {
        case Some(vhs) if vhs.isNotEmpty => recipient ! msg
        case _ =>
      }
    }

    private def respondAndReset(recipient: ActorRef) = {
      recipient ! EndOfVenueHours
      unstashAll()
      context.become(handleRunSearch)
    }
}
