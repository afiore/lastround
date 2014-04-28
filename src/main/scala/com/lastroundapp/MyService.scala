package com.lastroundapp

import scala.concurrent._
import ExecutionContext.Implicits.global

import spray.http._
import HttpMethods._
import HttpHeaders._

import akka.actor.Actor
import spray.routing.HttpService

import com.lastroundapp.auth._
import com.lastroundapp.data._

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class MyServiceActor extends Actor with MyService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  val actorRefFactory = context
  val oauth           = new FoursquareOAuth()(context.system)
  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive:Receive = runRoute(myRoute)
}


// this trait defines our service behavior independently from the service actor
trait MyService extends HttpService {
  import com.lastroundapp.data.FoursquareJsonProtocol._

  val oauth: FoursquareOAuth

  val myRoute =
    path("signin") {
      redirect(Foursquare.authUri, StatusCodes.TemporaryRedirect)
    } ~
    path("foursquare" / "login") {
      get {
        parameter('code) { code => {
          detach() {
            onSuccess(oauth.getAccessToken(code)) { token =>
              token match {
                case Some(FSToken(t)) => complete(s"Got $t")
                case None             => complete("Couldn't get token!")
              }
            }
          }
        }}
      }
    }
}
