package com.lastroundapp.services

import scala.util.{Success, Try}
import scala.concurrent.ExecutionContext

import com.lastroundapp.data.{VenueId, VenueHoursResponse, VenueSearchResponse}
import com.lastroundapp.data.VenueHours.{WeekDay, TimeOfDay}
import com.lastroundapp.data.Endpoints.{AccessToken, LatLon}

import akka.actor.ActorContext
import spray.http.{ContentType, MediaType}
import com.lastroundapp.services.FoursquareClient.VenueSearchQuery

import spray.httpx.unmarshalling.{FromStringDeserializer, MalformedContent}
import spray.http.MediaTypes._

object FoursquareClient {
  object Format {
    def fromHeaderValue(accept: Option[String]) =
      if (accept == Some("text/event-stream")) EventStream else JsonStream
  }
  trait Format {
    val lineSeparator: String
    val contentType: MediaType
  }

  object JsonStream extends Format {
    val lineSeparator = "\r\n"
    val contentType   = `application/json`
  }

  object EventStream extends Format {
    val lineSeparator = "\n\n"
    val contentType   = register(
      MediaType.custom(
        mainType = "text",
        subType  = "event-stream",
        compressible = true,
        binary = false
      )
    )
  }

  object DayWithTime {
    def now = apply(WeekDay.today, TimeOfDay.now)

    implicit val deserializer = new FromStringDeserializer[DayWithTime] {
      def apply(s: String) = {

        val dayOnly    = """([1-7])""".r
        val timeOnly   = """(\d\d\d\d)""".r
        val dayAndTime = """([1-7])-(\d\d\d\d)""".r

        s match {
          case dayOnly(d)      => Right(DayWithTime(d.toInt, TimeOfDay.now))
          case timeOnly(t)     => Right(DayWithTime(WeekDay.today, t))
          case dayAndTime(d,t) => Right(DayWithTime(d.toInt, t))
          case _               => Left(MalformedContent("Cannot parse DayWithTime!"))
        }
      }
    }
  }

  case class DayWithTime(d: WeekDay, tod: TimeOfDay)

  case class VenueSearchQuery(
      ll: LatLon,
      token: AccessToken,
      dateTime: DayWithTime,
      format: Format = EventStream)
}

abstract class FoursquareClient {
  def venueSearch(q:VenueSearchQuery)(implicit ac:ActorContext, ec:ExecutionContext): VenueSearchResponse
  def venueHours(vid:VenueId, token:AccessToken)(implicit ac:ActorContext, ec:ExecutionContext): VenueHoursResponse
}
