package com.lastroundapp.services

import scala.concurrent.ExecutionContext

import org.joda.time.DateTime

import com.lastroundapp.data.{VenueId, VenueHoursResponse, VenueSearchResponse}
import com.lastroundapp.data.VenueHours.{WeekDay, TimeOfDay}
import com.lastroundapp.data.Endpoints.{AccessToken, LatLon}

import akka.actor.ActorContext
import spray.http.{ContentType, MediaType}
import com.lastroundapp.services.FoursquareClient.VenueSearchQuery

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

  case class VenueSearchQuery(
      ll: LatLon,
      token: AccessToken,
      dateTime: DateTime,
      format: Format = EventStream)
}

abstract class FoursquareClient {
  def venueSearch(q:VenueSearchQuery)(implicit ac:ActorContext, ec:ExecutionContext): VenueSearchResponse
  def venueHours(vid:VenueId, token:AccessToken)(implicit ac:ActorContext, ec:ExecutionContext): VenueHoursResponse
}
