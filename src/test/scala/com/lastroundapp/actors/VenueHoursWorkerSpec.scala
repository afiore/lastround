package com.lastroundapp.actors

import com.lastroundapp.data.VenueHours.{Monday, ClosingTime}
import org.joda.time.DateTime

import scala.concurrent.duration._

import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import akka.actor.ActorSystem

import org.scalatest.{WordSpecLike, BeforeAndAfterAll, Matchers}

import com.lastroundapp.services.FoursquareTestClient

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
  val venueClosingDateTime = DateTime.now.withDayOfWeek(Monday).withTime(23,30,0,0)
  val worker = TestActorRef(new VenueHoursWorker(fsClient))
  val closingTime = ClosingTime(venueClosingDateTime, true)

  "VenueHoursWorker" when {
    "Foursquare responds with success" when {
      "supplied date matches opening time" should {
        "respond with a non-blank GotVenueHoursFor" in  {
          within(1.second) {
            worker ! GetVenueHoursFor(vidSuccess, queryWithMatchingTime)
            expectMsgType[GotVenueHoursFor] should be(GotVenueHoursFor(vidSuccess, Some(closingTime)))
          }
        }
      }
      "supplied date does not match opening times" should {
        "respond with a blank venue hours" in {
          within(1.second) {
            worker ! GetVenueHoursFor(vidSuccess, queryWithNonMatchingTime)
            expectMsgType[GotVenueHoursFor] should be(GotVenueHoursFor(vidSuccess, None))
          }
        }
      }
    }
    "Foursquare responds with failure" should {
      "respond with a blank GotVenueHours" in {
        within(1.second) {
          worker ! GetVenueHoursFor(vidFailure, queryWithMatchingTime)
          expectMsgType[GotVenueHoursFor] should be(GotVenueHoursFor(vidFailure, None))
        }
      }
    }
  }
}
