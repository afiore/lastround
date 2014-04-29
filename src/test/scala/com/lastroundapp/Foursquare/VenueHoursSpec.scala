package com.lastroundapp.data

import org.specs2.mutable._

import spray.json._

import VenueHours._
import Responses._

import DefaultJsonProtocol._
import VenueHoursJSONProtocol._
import FSResponseJsonProtocol._

class VenueHoursSpec extends Specification {
  def json(s:String)          = JsonParser(s)
  def jsonFile(f:String)      = json(io.Source.fromFile(f).mkString)
  def parseResponse(f:String) = jsonFile(f).convertTo[FoursquareResponse[VenueOpeningHours]]
  val venueHoursFixture       = "src/test/resources/venue-hours.json"

  "VenueHoursJSONProtocol" should {
    "deserialise venue hours data where both 'hours' and 'popular' attributes are present " in {
      parseResponse(venueHoursFixture) must beLike {
        case ResponseOK(VenueOpeningHours(h1 :: hs1, h2 :: hs2)) => ok
      }
    }
  }
}
