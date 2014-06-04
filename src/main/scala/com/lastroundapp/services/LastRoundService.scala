package com.lastroundapp.services

import akka.actor.{Props, ActorContext, Actor, ActorRef}
import akka.event.Logging

import com.lastroundapp.actors.{ResultStreamer, VenueSearcher}
import com.lastroundapp.auth.FoursquareOAuth
import com.lastroundapp.data.{FSToken, Endpoints}

import java.util.UUID

import spray.http.StatusCodes.{TemporaryRedirect, BadRequest}
import spray.routing.HttpService

import Endpoints._
import com.lastroundapp.services.FoursquareClient.VenueSearchQuery
import spray.httpx.encoding.Gzip

class LastRoundActor (val venueSearcher:ActorRef) extends Actor with LastRoundService {
  def actorRefFactory  = context
  val log              = Logging.getLogger(context.system, "FoursquareOAuth")
  val oauth            = new FoursquareOAuth(log)
  def receive: Receive = runRoute(route)
}

trait LastRoundService extends HttpService {
  val oauth: FoursquareOAuth
  val context: ActorContext
  val venueSearcher: ActorRef

  implicit val ec = context.dispatcher

  val route =
    path("search" / "open-venues") {
      get {
        parameters('ll.as[LatLon], 'token.as[AccessToken]) { (latLon, token) => request =>
          actorRefFactory.actorOf(Props(
            classOf[ResultStreamer],
            VenueSearchQuery(latLon, token),
            venueSearcher,
            request.responder),
            s"result-streamer-${UUID.randomUUID()}")
        }
      }
    } ~
    path("signin") {
      redirect(toUri(new AuthEndpoint), TemporaryRedirect)
    } ~
    path("signin" / "foursquare") {
      get {
        parameter('code) { code =>
          detach() {
            onSuccess(oauth.getAccessToken(code)(context, ec)) {
              case Some(FSToken(t)) =>
                complete(s"Got $t")
              case None =>
                complete(BadRequest)
            }
          }
        }
      }
    } ~
    pathPrefix("filez") {
      getFromResourceDirectory("static")
    }
}
