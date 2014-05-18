package com.lastroundapp.actors

import scala.concurrent.duration._

import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import akka.actor.ActorSystem

import org.scalatest.{WordSpecLike, BeforeAndAfterAll, Matchers}

import com.lastroundapp.services.FoursquareTestClient
import com.lastroundapp.data.Endpoints.AccessToken

class VenueHoursWorkerSpec extends TestKit(ActorSystem("test-system", akkaConfig))
                           with ImplicitSender
                           with WordSpecLike
                           with Matchers
                           with BeforeAndAfterAll {

  import VenueHoursWorker._
  import FoursquareTestClient._

  override def afterAll() {
    shutdown()
  }

  val fsClient = new FoursquareTestClient
  val worker = TestActorRef(new VenueHoursWorker(fsClient))

  "VenueHoursWorker" should {
     "respond with a non-blank GotVenueHoursFor when client responds successfully" in  {
       within(1.second) {
         worker ! GetVenueHoursFor(vidSuccess, AccessToken.default)
         expectMsgType[GotVenueHoursFor] should be(GotVenueHoursFor(vidSuccess, Some(venueHours1)))
       }
     }
     "respond with a blank 'GotVenueHoursFor' when client responds with failure" in {
        within(1.second) {
          worker ! GetVenueHoursFor(vidFailure, AccessToken.default)
          expectMsgType[GotVenueHoursFor] should be(GotVenueHoursFor(vidFailure, None))
        }
     }
  }
}
