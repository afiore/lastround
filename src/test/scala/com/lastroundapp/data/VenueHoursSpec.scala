package com.lastroundapp.data

import org.scalatest._
import org.scalatest.OptionValues._

import org.joda.time.DateTime
import Inside._

import spray.json._

import VenueHours._
import Responses._

import VenueHoursJSONProtocol._
import FSResponseJsonProtocol._

class VenueHoursSpec extends WordSpec with Matchers {
  trait Json {
    def json(s:String)          = JsonParser(s)
    def jsonFile(f:String)      = json(io.Source.fromFile(f).mkString)
    def parseResponse(f:String) = jsonFile(f).convertTo[FoursquareResponse[VenueOpeningHours]]
    val filePath                = "src/test/resources/venue-hours.json"
  }

  implicit class DateTimeHelper(dt: DateTime) {
    def at(hs: Int, ms: Int): DateTime = dt.withTime(hs, ms, 0, 0)
    def midnight = dt.at(0, 0)
  }

  trait OpeningTimes {
    val today            = DateTime.now
    val tomorrow         = today.plusDays(1)
    val monday           = today.withDayOfWeek(1)
    val wednesday        = today.withDayOfWeek(3)
    val saturday         = today.withDayOfWeek(6)

    val closeAt3AM       = OpeningTime(TimeOfDay(22,30), TimeOfDay(3,0), true)
    val openInTheMorning = OpeningTime(TimeOfDay(9,0), TimeOfDay(13,30))
    val openOnWeekEnd    = TimeFrame(Set(Friday, Saturday), List(closeAt3AM))
    val openOnWednesday  = TimeFrame(Set(Wednesday), List(closeAt3AM))

    val venueHours =
      VenueOpeningHours(
        List(openOnWeekEnd),
        List(openOnWednesday))

    def closingTime(hs: Int, ms: Int)        = ClosingTime(TimeOfDay(hs, ms), false)
    def inferedClosingTime(hs: Int, ms: Int) = ClosingTime(TimeOfDay(hs, ms), true)
  }

  // Opening Times

  "VenueHoursJSONProtocol" should {
    "de-serialise venue hours data" in new Json {
      inside (parseResponse(filePath)) { case ResponseOK(VenueOpeningHours(hs, pops)) =>
        hs shouldBe 'nonEmpty
        pops shouldBe 'nonEmpty
      }
    }
  }
  "OpeningTime#openOn" should {
    "return true when supplied datetime is before" in new OpeningTimes {
      closeAt3AM.openOn(today.at(2,30)) shouldBe true
      closeAt3AM.openOn(today.at(22,30)) shouldBe true
    }
    "return false when suppled datetime is after" in new OpeningTimes {
      closeAt3AM.openOn(today.at(4,0)) shouldBe false
      closeAt3AM.openOn(tomorrow.at(8,0)) shouldBe false
    }
    "return some closing time when datetime matches" in new OpeningTimes {
      venueHours.closingTimeAfter(saturday.midnight).value should equal (closingTime(3,0))
      venueHours.closingTimeAfter(wednesday.midnight).value should equal (inferedClosingTime(3,0))
    }
    "return none when datetime doesn't match" in new OpeningTimes {
      venueHours.closingTimeAfter(monday.at(20,30)) should equal (None)
    }
  }
}
