package com.lastroundapp.actors

import org.joda.time.DateTime

import scala.concurrent.duration._

import akka.testkit.{TestProbe, ImplicitSender, TestActorRef, TestKit}
import akka.actor.ActorSystem

import org.scalatest.{WordSpecLike, BeforeAndAfterAll, Matchers}
import com.lastroundapp.services.FoursquareTestClient
import com.lastroundapp.data.Endpoints.AccessToken
import com.lastroundapp.data.VenueHours._
import com.lastroundapp.data.Responses.NotAuthorised
import com.lastroundapp.data.VenueHours.OpeningTime
import com.lastroundapp.data.VenueHours.TimeFrame
import com.lastroundapp.data.VenueId

class VenueSearcherSpec(_system: ActorSystem) extends TestKit(_system)
                                              with ImplicitSender
                                              with WordSpecLike
                                              with Matchers
                                              with BeforeAndAfterAll {

  import VenueHoursWorker._
  import FoursquareTestClient._
  import VenueSearcher._

  def this() = this(ActorSystem("test-system"))

  override def afterAll() {
    TestKit.shutdownActorSystem(_system)
  }

  val fsClient = new FoursquareTestClient()
  val closingTime = ClosingTime(DateTime.now.withDayOfWeek(Monday), false)
  val vh2 = VenueOpeningHours(
    List.empty,
    List(
      TimeFrame(Set(Monday),
        List(
          OpeningTime(
            TimeOfDay(22, 0),
            TimeOfDay(6, 0))))))

  val gotVenues = GotVenueResults(Right(List(venue1, venue2)))

  implicit val workerPool = TestProbe()

  def newSearcher(implicit workerPool: TestProbe) = {
    TestActorRef(new VenueSearcher(fsClient, workerPool.ref))
  }

  "VenueSearcher" when {
    "a search hasn't started yet" should {
      "ignore messages other than RunSearch" in {
        within(200.millis) {
          newSearcher ! GotVenueHoursFor(VenueId("venue-1"), None)
          expectNoMsg()
        }
      }
    }
  }
  "VenueSearcher" when {
    "handling runSearch" should {
      "return blank venue hours when worker requests timeout" in {
        within(500.millis) {
          newSearcher ! RunSearch(querySuccess)
          expectMsg(gotVenues)
          expectMsg(EndOfVenueHours)
        }
      }
      "return a left value when Foursquare responds with a failure" in {
        within(500.millis) {
          newSearcher ! RunSearch(queryFailure)
          expectMsg(GotVenueResults(Left(NotAuthorised("bad token"))))
        }
      }
      "forward GotVenueHours messages only when venueHours match the supplied datetime" in {
        within(500.millis) {
          val workerPool = TestProbe()
          val gotVh1 = GotVenueHoursFor(VenueId("venue-1"), Some(closingTime))
          val gotVh2 = GotVenueHoursFor(VenueId("venue-2"), None)
          // ^ blank GotVenueHours messages should be filtered out

          newSearcher(workerPool) ! RunSearch(querySuccess)

          workerPool.expectMsg(200.millis, GetVenueHoursFor(VenueId("venue-1"), querySuccess))

          workerPool.reply(gotVh1)
          workerPool.reply(gotVh2)

          expectMsg(gotVenues)
          expectMsg(gotVh1)
          expectMsg(EndOfVenueHours)
        }
      }
    }
  }
}

