package com.lastroundapp.services

import akka.actor.{Props, ActorContext, Actor, ActorRef}
import akka.event.Logging

import com.lastroundapp.actors.{ResultStreamer, VenueSearcher}
import com.lastroundapp.auth.FoursquareOAuth
import com.lastroundapp.data.{FSToken, Endpoints}

import java.util.UUID
import org.joda.time.DateTime

import spray.http.HttpHeaders.RawHeader
import spray.http.HttpCookie
import spray.http.StatusCodes.{TemporaryRedirect, Found, BadRequest}
import spray.routing.{HttpService, RequestContext}

import Endpoints._
import com.lastroundapp.services.FoursquareClient.{VenueSearchQuery, Format}
import com.lastroundapp.data.VenueHours.DateTimeDeserialiser
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
        respondWithHeaders(RawHeader("Access-Control-Allow-Origin", "*")) {
          optionalHeaderValueByName("Accept") { accept =>
            val format = Format.fromHeaderValue(accept)
              parameters(
                'll.as[LatLon],
                'token.as[AccessToken],
                'radius.as[Int] ? 2000,
                'categories.as[Set[Category]] ? Category.defaultSet,
                'datetime.as[DateTime]) { (latLon, token, r, categories, datetime) => ctx =>

                actorRefFactory.actorOf(
                  Props(
                    classOf[ResultStreamer],
                    VenueSearchQuery(latLon, token, Radius(r), categories, datetime, format),
                    venueSearcher,
                    ctx.responder
                  ),
                  s"result-streamer-${UUID.randomUUID()}"
                )
            }
          }
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
                setCookie(HttpCookie("authToken", t, path=Some("/"))) {
                  redirect("/index.html", Found)
                }
              case None =>
                complete(BadRequest)
            }
          }
        }
      }
    } ~
    optionalCookie("authToken") {
      case Some(cookie) => {
        setCookie(cookie) {
          getFromResourceDirectory("static")
        }
      }
      case _ => getFromResourceDirectory("static")
    }
}
