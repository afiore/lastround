package com.lastroundapp.actors

import com.lastroundapp.actors.VenueHoursWorker.GotVenueHoursFor
import com.lastroundapp.actors.VenueSearcher.{EndOfVenueHours, GotVenueResults, RunSearch}
import com.lastroundapp.data.Responses.ParamError
import com.lastroundapp.data.VenueHours.VenueOpeningHours

import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.testkit.{TestProbe, TestActorRef, ImplicitSender, TestKit}
import spray.http._

import com.lastroundapp.services.FoursquareTestClient
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}


class ResultStreamerSpec extends TestKit(ActorSystem("test-system", akkaConfig))
     with ImplicitSender
     with WordSpecLike
     with Matchers
     with BeforeAndAfterAll {

  override def afterAll() {
    shutdown()
  }

  import FoursquareTestClient._

  val timeout = 10000.millis
  val q = venueSearchQuerySuccess
  val venueList = List(venue1, venue2)
  val vh1 = VenueOpeningHours.empty
  val responder = TestProbe()
  val searcher = TestProbe()

  def newStreamer() =
    TestActorRef(new ResultStreamer(q, searcher.ref, responder.ref))

  "ResultStreamer" should {
    "ends the stream after the specified timeout" in {
      within(timeout) {
        newStreamer()
        searcher.expectMsg(RunSearch(q))
        responder.expectMsgPF(10.millis) { case _: ChunkedResponseStart => true}
        responder.expectMsgPF(timeout) { case _: ChunkedMessageEnd => true}
      }
    }
    "streams a response chunk when the searcher sends a 'SearchVenueResults' message" in {
      within(200.millis) {
        newStreamer()
        searcher.expectMsg(RunSearch(q))
        searcher.reply(GotVenueResults(Right(venueList)))
        responder.expectMsgPF(10.millis) { case _: ChunkedResponseStart => true}
        responder.expectMsgPF(15.millis) { case _: MessageChunk => true}
      }
    }
    "ends the stream after receiving the GotVenueResult(Left(_))" in {
      within(200.millis) {
        newStreamer()
        searcher.expectMsg(RunSearch(q))
        searcher.reply(GotVenueResults(Left(ParamError("bad actor!"))))
        responder.expectMsgPF(35.millis) { case _: ChunkedResponseStart => true}
        responder.expectMsgPF(50.millis) { case _: MessageChunk => true }
        responder.expectMsgPF(75.millis) { case _: ChunkedMessageEnd => true }
      }
    }

    "ends the stream after receiving the EndOfVenueHours message" in {
      within(100.millis) {
        newStreamer()
        searcher.expectMsg(RunSearch(q))
        searcher.reply(GotVenueResults(Right(venueList)))
        searcher.reply(GotVenueHoursFor(venue1.id, Some(vh1)))
        searcher.reply(EndOfVenueHours)

        responder.expectMsgPF(10.millis) { case _: ChunkedResponseStart => true}
        responder.expectMsgPF(15.millis) { case _: MessageChunk => true}
        responder.expectMsgPF(20.millis) { case _: MessageChunk => true}
        responder.expectMsgPF(25.millis) { case _: ChunkedMessageEnd => true}
      }
    }
  }
}