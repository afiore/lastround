package com.lastroundapp.data

import org.scalatest._
import spray.json._

import Responses._
import VenueJSONProtocol._
import FSResponseJsonProtocol._

class VenueSpec extends FlatSpec with Matchers with Inside {

  def json(s:String)          = JsonParser(s)
  def jsonFile(f:String)      = json(io.Source.fromFile(f).mkString)
  def parseResponse(f:String) = jsonFile(f).convertTo[FoursquareResponse[List[Venue]]]

  val venue = Venue(
        VenueId("530f1956498e369520f124d7"),
        "weplaLab1393498453299",
        Location(Some("Address1"), 44.3, 37.2, 0, None, "RU", "Russia", None),
        None)

  val validVenueJson =
    """
    {
      "id": "530f1956498e369520f124d7",
      "name": "weplaLab1393498453299",
      "location": {
        "address": "Address1",
        "crossStreet": "undefined",
        "lat": 44.3,
        "lng": 37.2,
        "distance": 0,
        "cc": "RU",
        "country": "Russia"
      }
    }
    """
  val validVenuesJson  = s"""{"venues":[ $validVenueJson ]}"""
  val invalidVenueJson = """"id":[^,]+""".r replaceFirstIn(validVenueJson, """"id": null""")
  val mixedVenuesJson  = s"""{"venues":[ $invalidVenueJson, $validVenueJson ]}"""
  val validSearchResultPath = "src/test/resources/venues-search.json"
  val invalidSearchResultPath = "src/test/resources/venues-search-error.json"

  "VenueJSONProtocol" should "deserializes a single venue" in {
    json(validVenueJson).convertTo[Venue] should be(venue)
  }

  it should "raise an error when format is not valid" in {
    intercept[DeserializationException] {
      json(invalidVenueJson).convertTo[Venue]
    }
  }

  it should "deserialize a list of venues" in {
    json(validVenuesJson).convertTo[List[Venue]] should be(List(venue))
  }

  it should "parse a successful Foursquare result" in {
    inside(parseResponse(validSearchResultPath)) {
      case ResponseOK(vs) => vs should not be empty
    }
  }

  it should "parse an unsuccessful Foursquare result" in {
    inside(parseResponse(invalidSearchResultPath)) {
      case ResponseError(ParamError(msg)) => msg should not be empty
    }
  }
}
