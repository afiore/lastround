package com.lastroundapp.actors

import scala.concurrent.duration._

import akka.testkit.{TestProbe, ImplicitSender, TestActorRef, TestKit}
import akka.actor.ActorSystem

import org.scalatest.{WordSpecLike, BeforeAndAfterAll, Matchers}
import org.scalatest.mock.MockitoSugar
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
                                              with MockitoSugar
                                              with BeforeAndAfterAll {

  import VenueHoursWorker._
  import FoursquareTestClient._
  import VenueSearcher._

  def this() = this(ActorSystem("test-system"))

  override def afterAll() {
    TestKit.shutdownActorSystem(_system)
  }

  val fsClient  = new FoursquareTestClient()
  val vh1       = VenueOpeningHours.empty
  val vh2       = VenueOpeningHours(
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

  "VenueSearcher" should {
    "ignore messages other than RunSearch when a search hasn't started yet" in {
      within(200.millis) {
        newSearcher ! GotVenueHoursFor(VenueId("venue-1"), None)
        expectNoMsg()
      }
    }
    "return blank venue hours when worker requests time out" in {
      within(500.millis) {
        newSearcher ! RunSearch(venueSearchQuerySuccess)
        expectMsg(gotVenues)
        expectMsg(EndOfVenueHours)
      }
    }
    "return a left value when Foursquare API responds with a failure" in {
      within(500.millis) {
        newSearcher ! RunSearch(venueSearchQueryFailure)
        expectMsg(GotVenueResults(Left(NotAuthorised("bad token"))))
      }
    }
    "stash incoming RunSearch while handing GotVenueWithOpeningHours" in {
      /*
      within(2.seconds) {
        val searcher = newSearcher
        searcher ! RunSearch(venueSearchQuerySuccess)
        searcher ! RunSearch(venueSearchQuerySuccess)
        expectMsgAllOf(venuesWithNoHours, venuesWithNoHours)
      }
      */
      pending
    }
    "respond to a RunSearch message with GotVenueWithOpeningHours" in  {
      within(500.millis) {
        val workerPool = TestProbe()
        val gotVh1     = GotVenueHoursFor(VenueId("venue-1"), Some(vh1))
        val gotVh2     = GotVenueHoursFor(VenueId("venue-2"), Some(vh2))

        newSearcher(workerPool) ! RunSearch(venueSearchQuerySuccess)

        workerPool.expectMsg(200.millis, GetVenueHoursFor(VenueId("venue-1"), AccessToken.default))
        workerPool.expectMsg(200.millis, GetVenueHoursFor(VenueId("venue-2"), AccessToken.default))

        workerPool.reply(gotVh1)
        workerPool.reply(gotVh2)

        expectMsg(gotVenues)
        expectMsg(gotVh2) //blank venue hours are supposed to be filtered out
        expectMsg(EndOfVenueHours)
      }
    }
  }
}
