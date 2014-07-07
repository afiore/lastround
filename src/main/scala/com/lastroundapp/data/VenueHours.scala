package com.lastroundapp.data

import scala.language.implicitConversions
import java.util.Calendar
import org.joda.time.DateTime
import spray.json._

object VenueHours {

  sealed trait WeekDay

  object Sunday    extends WeekDay
  object Monday    extends WeekDay
  object Tuesday   extends WeekDay
  object Wednesday extends WeekDay
  object Thursday  extends WeekDay
  object Friday    extends WeekDay
  object Saturday  extends WeekDay

  implicit def weekdDay2Int(d:WeekDay):Int = d match {
    case Monday    => 1
    case Tuesday   => 2
    case Wednesday => 3
    case Thursday  => 4
    case Friday    => 5
    case Saturday  => 6
    case Sunday    => 7
  }

  object WeekDay {
    implicit def int2WeekDay(n:Int):WeekDay = n match {
      case 1 => Monday
      case 2 => Tuesday
      case 3 => Wednesday
      case 4 => Thursday
      case 5 => Friday
      case 6 => Saturday
      case 7 => Sunday
    }

    def today:WeekDay =
      Calendar.getInstance.get(Calendar.DAY_OF_WEEK) match {
        case 1 => 7
        case n => n
      }
  }

  object TimeOfDay {
    implicit def tod2String(tod:TimeOfDay): String =
       "%02d".format(tod.hours) ++
       "%02d".format(tod.minutes)

    implicit def string2tod(rawS: String): TimeOfDay = {
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

    def now: TimeOfDay = {
      val d = DateTime.now
      TimeOfDay(d.getHourOfDay, d.getMinuteOfHour)
    }
  }
  sealed case class TimeOfDay(hours:Int, minutes:Int) {
    require(hours >= 0 && hours < 24,     "hours must be within 0 and 23")
    require(minutes >= 0 && minutes < 60, "minutes must be within 0 and 59")
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
      popular: List[TimeFrame]) {
    def isEmpty: Boolean = this == VenueOpeningHours.empty
    def isNotEmpty: Boolean = !isEmpty
  }

  sealed case class VenueHoursFor(vid:VenueId, vhs: VenueOpeningHours)

  object VenueHoursJSONProtocol extends DefaultJsonProtocol {

    import VenueJSONProtocol._

    implicit object WeekDay2Json extends JsonFormat[WeekDay] {
      def write(d:WeekDay):JsValue =
        JsNumber(d)
      def read(v:JsValue):WeekDay = v match {
        case JsNumber(n) if n.toInt > 0 && n.toInt <= 7 =>
          n.toInt
        case _ =>
          throw new DeserializationException("WeekDay2Json: Number between 1 and 7 expected")
      }
    }

    implicit object OpeningTime2Json extends JsonFormat[OpeningTime] {
      def write(ot:OpeningTime):JsValue = JsObject(
        "start" -> JsString(ot.start),
        "end"   -> JsString(ot.end)
      )

      def read(v:JsValue) = v.asJsObject.getFields("start", "end") match {
        case Seq(JsString(start), JsString(end)) =>
          OpeningTime(start,end)
        case _ =>
          throw new DeserializationException(
            "OpeningTime2Json: Cannot parse start/end attributes")
      }
    }

    implicit object TimeFrame2Json extends JsonFormat[TimeFrame] {
      def write(tf:TimeFrame):JsValue = JsObject(
        "days" -> JsArray(tf.days.toList.map(_.toJson)),
        "open" -> JsArray(tf.open.map(_.toJson))
      )
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
      def write(tf:List[TimeFrame]):JsValue =
        JsArray(tf.map(_.toJson))
      def read(v:JsValue) = v.asJsObject.getFields("timeframes") match {
        case Seq(JsArray(timeframes)) =>
          timeframes.map(_.convertTo[TimeFrame])
        case _ => Nil
      }
    }

    implicit object VenueOpeningHours2Json extends JsonFormat[VenueOpeningHours] {
      def write(voh:VenueOpeningHours):JsValue = JsObject(
        "hours"   -> JsArray(voh.hours.map(_.toJson)),
        "popular" -> JsArray(voh.popular.map(_.toJson))
      )
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
    implicit object VenueHoursFor2Json extends JsonFormat[VenueHoursFor] {
      def write(vhf:VenueHoursFor):JsValue = JsObject(
        "venueId" -> vhf.vid.toJson,
        "hours" -> vhf.vhs.toJson
      )
      def read(v:JsValue):VenueHoursFor= ???
    }
  }
}
