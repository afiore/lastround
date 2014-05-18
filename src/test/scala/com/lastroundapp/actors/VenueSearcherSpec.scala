package com.lastroundapp.actors

import scala.concurrent.duration._

import akka.testkit.{TestProbe, ImplicitSender, TestActorRef, TestKit}
import akka.actor.{Props, ActorSystem}

import org.scalatest.{Pending, WordSpecLike, BeforeAndAfterAll, Matchers}
import org.scalatest.mock.MockitoSugar
import com.lastroundapp.services.FoursquareTestClient
import com.lastroundapp.data.Endpoints.{AccessToken, LatLon}
import com.lastroundapp.data.{VenueWithOpeningHours, VenueId}
import com.lastroundapp.data.VenueHours.VenueOpeningHours
import com.lastroundapp.services.FoursquareClient.VenueSearchQuery

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

  val ll                = mock[LatLon]
  val q                 = VenueSearchQuery(ll, AccessToken.default)
  val vh1               = mock[VenueOpeningHours]
  val vh2               = mock[VenueOpeningHours]
  val venuesWithNoHours =
    GotVenuesWithOpeningHours(List(
      VenueWithOpeningHours(venue1, None),
      VenueWithOpeningHours(venue2, None)))

  val fsClient            = new FoursquareTestClient()
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
        newSearcher ! RunSearch(q)
        expectMsg(venuesWithNoHours)
      }
    }
    "stash incoming RunSearch while handing GotVenueWithOpeningHours" in {
      //within(2.seconds) {
      //  val searcher = newSearcher
      //  searcher ! RunSearch(ll)
      //  searcher ! RunSearch(ll)
      //  expectMsgAllOf(venuesWithNoHours, venuesWithNoHours)
      //}
      pending
    }
    "respond to a RunSearch message with GotVenueWithOpeningHours" in  {
      within(500.millis) {
        val workerPool = TestProbe()
        newSearcher(workerPool) ! RunSearch(q)

        workerPool.expectMsg(200.millis, GetVenueHoursFor(VenueId("venue-1"), AccessToken.default))
        workerPool.expectMsg(200.millis, GetVenueHoursFor(VenueId("venue-2"), AccessToken.default))
        workerPool.reply(GotVenueHoursFor(VenueId("venue-1"), Some(vh1)))
        workerPool.reply(GotVenueHoursFor(VenueId("venue-2"), Some(vh2)))

        expectMsg(GotVenuesWithOpeningHours(
          List(VenueWithOpeningHours(venue1, Some(vh1)),
               VenueWithOpeningHours(venue2, Some(vh2)))))
      }
    }
  }
}
