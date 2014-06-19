package com.lastroundapp.data

import com.lastroundapp.data.Events.{VenueSearchResult, ServerEvent}

import scala.language.implicitConversions

import spray.json._

sealed case class Location(
    address: Option[String],
    lat:Double,
    lng:Double,
    distance:Int,
    postalCode: Option[String],
    cc: String,
    country:String,
    city: Option[String])

object VenueId {
  implicit def VenueId2String(v:VenueId):String = v.id
}
sealed case class VenueId(val id:String)

sealed case class Venue(
     id: VenueId,
     name: String,
     location: Location,
     url: Option[String])

object VenueJSONProtocol extends DefaultJsonProtocol {
  implicit object VenueIdFormat extends JsonFormat[VenueId] {
    def write(vId:VenueId):JsValue = JsString(vId.id)
    def read(v:JsValue):VenueId = v match {
      case JsString(id) =>
        VenueId(id)
      case _ =>
        throw new DeserializationException("VenueId expected")
    }
  }
  implicit object VenueListFormat extends JsonFormat[List[Venue]] {
    def write(vs:List[Venue]):JsValue =
      JsArray(vs.map(_.toJson))
    def read(v:JsValue): List[Venue]  = v.asJsObject.getFields("venues") match {
      case Seq(JsArray(vs)) =>
        vs.map(_.convertTo[Venue])
      case _ =>
        Nil
    }
  }
  implicit val locationFormat = jsonFormat8(Location)
  implicit val venueFormat    = jsonFormat4(Venue)
}
