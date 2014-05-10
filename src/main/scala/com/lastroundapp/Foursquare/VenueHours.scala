package com.lastroundapp.data

import scala.language.implicitConversions
import java.util.Calendar

import spray.json._
import DefaultJsonProtocol._

object VenueHours {

  sealed trait WeekDay

  object Sunday    extends WeekDay
  object Monday    extends WeekDay
  object Tuesday   extends WeekDay
  object Wednesday extends WeekDay
  object Thursday  extends WeekDay
  object Friday    extends WeekDay
  object Saturday  extends WeekDay

  implicit def int2WeekDay(n:Int):WeekDay = n match {
    case 1 => Monday
    case 2 => Tuesday
    case 3 => Wednesday
    case 4 => Thursday
    case 5 => Friday
    case 6 => Saturday
    case 7 => Sunday
  }

  object WeekDay {
    def today:WeekDay =
      Calendar.getInstance.get(Calendar.DAY_OF_WEEK) match {
        case 1 => 7
        case n => n
      }
  }

  sealed case class TimeOfDay(hours:Int, minutes:Int) {
    require(hours >= 0 && hours < 24,     "hours must be within 0 and 23")
    require(minutes >= 0 && minutes < 60, "minutes must be within 0 and 59")
  }

  object TimeOfDay {
    def fromString(rawS:String):TimeOfDay = {
      val s = rawS.filter(_.isDigit)

      if (s.length == 4 && s.forall(_.isDigit)) {
        val (hs, ms) = s.splitAt(2)
        TimeOfDay(hs.toInt, ms.toInt)
      }
      else {
        throw new IllegalArgumentException(
          s"must be a string of 4 digits, got $s instead")
      }
    }
  }

  sealed case class OpeningTime(start:TimeOfDay, end:TimeOfDay)

  sealed case class TimeFrame(days:Set[WeekDay], open:List[OpeningTime]) {
    def includesToday: Boolean = days.contains(WeekDay.today)
  }

  object VenueOpeningHours {
    def empty = VenueOpeningHours(List.empty, List.empty)
  }
  sealed case class VenueOpeningHours(
      hours: List[TimeFrame],
      popular: List[TimeFrame])

  object VenueHoursJSONProtocol extends DefaultJsonProtocol {

    implicit object WeekDay2Json extends JsonFormat[WeekDay] {
      def write(d:WeekDay):JsValue = ???
      def read(v:JsValue):WeekDay = v match {
        case JsNumber(n) if n.toInt > 0 && n.toInt <= 7 =>
          int2WeekDay(n.toInt)
        case _ =>
          throw new DeserializationException(
            "WeekDay2Json: Number between 1 and 7 expected")
      }
    }

    implicit object OpeningTime2Json extends JsonFormat[OpeningTime] {
      def write(tod:OpeningTime):JsValue = ???
      def read(v:JsValue) = v.asJsObject.getFields("start", "end") match {
        case Seq(JsString(start), JsString(end)) =>
          OpeningTime(
            TimeOfDay.fromString(start),
            TimeOfDay.fromString(end))
        case _ =>
          throw new DeserializationException(
            "OpeningTime2Json: Cannot parse start/end attributes")
      }
    }

    implicit object TimeFrame2Json extends JsonFormat[TimeFrame] {
      def write(tf:TimeFrame):JsValue = ???
      def read(v:JsValue) = v.asJsObject.getFields("days", "open") match {
        case Seq(JsArray(days), JsArray(open)) =>
          TimeFrame(
            days.map(_.convertTo[WeekDay]).toSet,
            open.map(_.convertTo[OpeningTime]))
        case _ =>
          throw new DeserializationException(
            "TimeFrame2Json: Cannot parse 'days'/'open' attributes")
      }
    }

    implicit object TimeFrameList2Json extends JsonFormat[List[TimeFrame]] {
      def write(tf:List[TimeFrame]):JsValue = ???
      def read(v:JsValue) = v.asJsObject.getFields("timeframes") match {
        case Seq(JsArray(timeframes)) =>
          timeframes.map(_.convertTo[TimeFrame])
        case _ => Nil
      }
    }

    implicit object VenueOpeningHours2Json extends JsonFormat[VenueOpeningHours] {
      def write(voh:VenueOpeningHours):JsValue = ???
      def read(v:JsValue):VenueOpeningHours = v.asJsObject.getFields("hours", "popular") match {
        case Seq(hours:JsObject, popular:JsObject) =>
          VenueOpeningHours(
            hours.convertTo[List[TimeFrame]],
            popular.convertTo[List[TimeFrame]])

          case _ =>
            throw new DeserializationException(
              "VenueOpeningHours2Json: Cannot find both 'hours' and 'popular' attributes")
      }
    }
  }

}
