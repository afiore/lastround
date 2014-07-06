package com.lastroundapp.data

import com.lastroundapp.data.VenueHours._
import com.lastroundapp.data.Responses._
import spray.json._

object Events {
  trait ServerEventType {
    def toString: String
  }
  object VenueSearchResult extends ServerEventType {
    override def toString = "VENUES"
  }
  object VenueHours extends ServerEventType {
    override def toString = "VENUE_HOURS"
  }
  object Error extends ServerEventType {
    override def toString = "SERVER_ERROR"
  }

  case class ServerEvent[T: JsonFormat](eventType: ServerEventType, data: T)

  object ServerEventConversions {
    import VenueJSONProtocol._
    import VenueHoursJSONProtocol._
    import FSResponseJsonProtocol._

    implicit def venueList2ServerEvent(vs: List[Venue]): ServerEvent[List[Venue]] =
      ServerEvent(VenueSearchResult, vs)

    implicit def venueHoursFor2ServerEvent(vhf: VenueHoursFor): ServerEvent[VenueHoursFor] =
      ServerEvent(VenueHours, vhf)

    implicit def err2ServerEvent(err: ApiError): ServerEvent[ApiError] =
      ServerEvent(Error, err)
  }

  object ServerEventJsonProtocol {
    implicit def ServerEvent2Json[T: JsonFormat] = new JsonFormat[ServerEvent[T]] {
      def write(event: ServerEvent[T]): JsValue =
        JsObject("type" -> JsString(event.eventType.toString),
                 "data" -> event.data.toJson)
      def read(v:JsValue): ServerEvent[T] = ???
    }
  }
}