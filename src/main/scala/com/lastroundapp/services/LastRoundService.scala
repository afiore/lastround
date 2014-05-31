package com.lastroundapp.services

import akka.actor.{ActorContext, Actor, ActorRef}
import akka.event.Logging

import com.lastroundapp.actors.VenueSearcher
import com.lastroundapp.auth.FoursquareOAuth
import com.lastroundapp.data.{FSToken, Endpoints}

import spray.http.StatusCodes.{TemporaryRedirect, BadRequest}
import spray.routing.HttpService

import Endpoints._

class LastRoundActor (val venueSearcher:ActorRef) extends Actor with LastRoundService {
  val actorRefFactory = context
  val log             = Logging.getLogger(context.system, "FoursquareOAuth")
  val oauth           = new FoursquareOAuth(log)
  def receive: Receive = runRoute(myRoute)
}

trait LastRoundService extends HttpService {
  val oauth: FoursquareOAuth
  val context: ActorContext
  val venueSearcher: ActorRef

  implicit val ec = context.dispatcher

  val myRoute =
    path("signin") {
      redirect(toUri(new AuthEndpoint), TemporaryRedirect)
    } ~
    path("signin" / "foursquare") {
      get {
        parameter('code) { code =>
          detach() {
            onSuccess(oauth.getAccessToken(code)(context, ec)) { token =>
              token match {
                case Some(FSToken(t)) =>
                  complete(s"Got $t")
                case None =>
                  complete(BadRequest)
              }
            }
          }
        }
      }
    }
}
