package com.lastroundapp.data

import scala.language.implicitConversions
import scala.util.{Try, Success}
import org.joda.time.{DateTime, Interval}
import spray.httpx.unmarshalling.{FromStringDeserializer, MalformedContent}
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
  }

  implicit val DateTimeDeserialiser = new FromStringDeserializer[DateTime] {
    def apply(s: String) = {
      val tDateTime = Try { new DateTime(s.toLong) }
      tDateTime match {
        case Success(d) => Right(d)
        case _ => Left(MalformedContent(s"Cannot parse string $s as a DateTime"))
      }
    }
  }

  object TimeOfDay {
    implicit def tod2String(tod:TimeOfDay): String =
       "%02d".format(tod.hours) ++
       "%02d".format(tod.minutes)

    implicit def string2tod(s: String): TimeOfDay = s match {
      case regexp(hs, ms) =>
        TimeOfDay(hs.toInt, ms.toInt)
      case _ =>
        throw new IllegalArgumentException(
          s"must be a string of 4 digits, got $s instead")
    }

    def now: TimeOfDay = {
      val d = DateTime.now
      TimeOfDay(d.getHourOfDay, d.getMinuteOfHour)
    }
    private val regexp = """\+?(\d\d)(\d\d)""".r
  }
  sealed case class TimeOfDay(hours:Int, minutes:Int) {
    require(hours >= 0 && hours < 24, "hours must be within 0 and 23")
    require(minutes >= 0 && minutes < 60, "minutes must be within 0 and 59")
  }

  sealed case class OpeningTime(
      start: TimeOfDay,
      end: TimeOfDay,
      endsNextDay: Boolean = false) {

    def openOn(dt: DateTime): Boolean = {
      this.toInterval(timeFrameDay(dt))
        .contains(dt)
    }
    def timeFrameDay(dt: DateTime): WeekDay =
      if (isNightTime(dt))
        dt.minusDays(1).getDayOfWeek
      else
        dt.getDayOfWeek

    private def isNightTime(dt: DateTime): Boolean =
      (0 until 5) contains(dt.getHourOfDay)

    private def toInterval(d: WeekDay): Interval = {
      val startD = mkDate(d, start)
      val endD   = mkDate(d, end)
      if (endsNextDay)
        new Interval(startD, endD.plusDays(1))
      else
        new Interval(startD, endD)
    }

    private def mkDate(d: WeekDay, t: TimeOfDay) =
      DateTime.now.withDayOfWeek(d).withTime(t.hours, t.minutes, 0, 0)
  }

  sealed case class TimeFrame(days:Set[WeekDay], open:List[OpeningTime]) {
    def openingTimeAfter(dt: DateTime): Option[OpeningTime] =
      if (!days.contains(dt.getDayOfWeek)) None
      else open.find(_.openOn(dt))
  }

  sealed case class ClosingTime(time:TimeOfDay, infered: Boolean)

  object VenueOpeningHours {
    def empty = VenueOpeningHours(List.empty, List.empty)
  }
  sealed case class VenueOpeningHours(hours: List[TimeFrame], popular: List[TimeFrame]) {
    def isEmpty: Boolean = this == VenueOpeningHours.empty
    def isNotEmpty: Boolean = !isEmpty

    def closingTimeAfter(dt: DateTime): Option[ClosingTime] =
      firstAfter(dt, hours, false)
        .orElse(firstAfter(dt, popular, true))

    private def firstAfter(dt: DateTime, tfs: List[TimeFrame], infered: Boolean): Option[ClosingTime] =
      tfs.view.map(_.openingTimeAfter(dt))
        .collectFirst {
           case Some(OpeningTime(_, t, _)) => ClosingTime(t, infered)
        }
  }

  sealed case class ClosingTimeFor(vid: VenueId, ct: ClosingTime)
  sealed case class VenueHoursFor(vid:VenueId, vhs: VenueOpeningHours)

  // JSON serialisation
  object VenueHoursJSONProtocol extends DefaultJsonProtocol {
    import VenueJSONProtocol._

    implicit object WeekDay2Json extends JsonFormat[WeekDay] {
      def write(d:WeekDay):JsValue =
        JsNumber(d)
      def read(v:JsValue):WeekDay = v match {
        case JsNumber(n) if n.toInt > 0 && n.toInt <= 7 =>
          n.toInt
        case _ =>
          throw new DeserializationException(
            "WeekDay2Json: Number between 1 and 7 expected")
      }
    }

    implicit object OpeningTime2Json extends JsonFormat[OpeningTime] {
      def write(ot:OpeningTime):JsValue = {
        JsObject(
          "start"       -> JsString(ot.start),
          "end"         -> JsString(ot.end),
          "endsNextDay" -> JsBoolean(ot.endsNextDay)
        )
      }

      def read(v:JsValue) = v.asJsObject.getFields("start", "end") match {
        case Seq(JsString(start), JsString(end)) =>
          OpeningTime(start, end, end.startsWith("+"))
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
              "VenueOpeningHours2Json: Cannot find either 'hours' or 'popular' attributes")
      }
    }

    implicit object ClosingTime2Json extends JsonFormat[ClosingTime] {
      def write(ct: ClosingTime): JsValue = JsObject(
        "hours"   -> JsNumber(ct.time.hours),
        "minutes" -> JsNumber(ct.time.minutes),
        "infered" -> JsBoolean(ct.infered)
      )
      def read(v:JsValue):ClosingTime = ???
    }

    implicit object ClosingTimeFor2Json extends JsonFormat[ClosingTimeFor] {
      def write(ctf: ClosingTimeFor):JsValue = JsObject(
        "venueId"  -> ctf.vid.toJson,
        "closingTime" -> ctf.ct.toJson
      )
      def read(v:JsValue):ClosingTimeFor = ???
    }
  }
}
