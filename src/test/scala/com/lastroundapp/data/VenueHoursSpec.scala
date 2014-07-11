package com.lastroundapp.data

import org.scalatest._
import org.joda.time.DateTime
import Inside._

import spray.json._

import VenueHours._
import Responses._

import VenueHoursJSONProtocol._
import FSResponseJsonProtocol._

class VenueHoursSpec extends FlatSpec with Matchers {
  // JSON serialisation
  def json(s:String)          = JsonParser(s)
  def jsonFile(f:String)      = json(io.Source.fromFile(f).mkString)
  def parseResponse(f:String) = jsonFile(f).convertTo[FoursquareResponse[VenueOpeningHours]]
  val filePath                = "src/test/resources/venue-hours.json"

  // Opening Times
  val tonightAt4Am     = DateTime.now.plusDays(1).withTime(4,0, 0,0)
  val tomorrowAt8Am    = DateTime.now.plusDays(1).withTime(8,0, 0,0)
  val saturdayMidnight = DateTime.now.withDayOfWeek(6).withTime(0,0, 0,0)
  val tonightAt2_40AM  = DateTime.now.plusDays(1).withTime(2,40, 0,0)
  val doorsOpen        = DateTime.now.withTime(22,31, 0,0)
  val mondayNight      = DateTime.now.withDayOfWeek(1).withTime(20,30, 0,0)
  val wednesdayNight   = DateTime.now.withDayOfWeek(3).withTime(23,30, 0,0)

  val closeAt3AM       = OpeningTime(TimeOfDay(22,30), TimeOfDay(3,0), true)
  val openInTheMorning = OpeningTime(TimeOfDay(9,0), TimeOfDay(13,30))
  val openOnWeekEnd    = TimeFrame(Set(Friday, Saturday), List(closeAt3AM))
  val openOnWednesday  = TimeFrame(Set(Wednesday), List(closeAt3AM))

  val venueHours =
    VenueOpeningHours(
      List(openOnWeekEnd),
      List(openOnWednesday))

  "VenueHoursJSONProtocol" should "de-serialise venue hours data" in {
    inside (parseResponse(filePath)) { case ResponseOK(VenueOpeningHours(hs, pops)) =>
      hs shouldBe 'nonEmpty
      pops shouldBe 'nonEmpty
    }
  }
  "OpeningTime#openOn" should "return true when supplied datetime is before" in {
    closeAt3AM.openOn(tonightAt2_40AM) shouldBe true
    closeAt3AM.openOn(doorsOpen) shouldBe true
  }
  "OpeningTime#openOn" should "return false when suppled datetime is after" in {
    closeAt3AM.openOn(tonightAt4Am) shouldBe false
    closeAt3AM.openOn(tomorrowAt8Am) shouldBe false
  }
  "TimeFrame#openingTimeAfter" should "return none when daytime does not match" in {
    openOnWeekEnd.openingTimeAfter(mondayNight) should equal (None)
  }
  "TimeFrame#openingTimeAfter" should "return the opening time when daytime matches" in {
    openOnWeekEnd.openingTimeAfter(saturdayMidnight) should equal (Some(closeAt3AM))
  }
  "VenueOpeningHours#closingTimeAfter" should "return some closing time when the supplied datetime matches" in {
    venueHours.closingTimeAfter(saturdayMidnight) should equal (Some(ClosingTime(TimeOfDay(3,0), false)))
    venueHours.closingTimeAfter(wednesdayNight) should equal (Some(ClosingTime(TimeOfDay(3,0), true)))
  }
  "VenueOpeningHours#closingTimeAfter" should "return none when the supplied datetime doesn't match" in {
    venueHours.closingTimeAfter(mondayNight) should equal (None)
  }
}
