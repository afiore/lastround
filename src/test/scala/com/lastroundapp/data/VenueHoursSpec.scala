package com.lastroundapp.data

import org.scalatest._

import spray.json._

import VenueHours._
import Responses._

import DefaultJsonProtocol._
import VenueHoursJSONProtocol._
import FSResponseJsonProtocol._

class VenueHoursSpec extends FlatSpec with Matchers {
  def json(s:String)          = JsonParser(s)
  def jsonFile(f:String)      = json(io.Source.fromFile(f).mkString)
  def parseResponse(f:String) = jsonFile(f).convertTo[FoursquareResponse[VenueOpeningHours]]
  val filePath                = "src/test/resources/venue-hours.json"

  "VenueHoursJSONProtocol" should "de-serialise venue hours data" in {
    val ok = parseResponse(filePath) match {
      case ResponseOK(VenueOpeningHours(h1 :: hs1, h2 :: hs2)) => true
      case _                                                   => false
    }

    ok should be(true)
  }
}
