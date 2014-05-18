package com.lastroundapp.data

import spray.http._
import spray.http.Uri._
import scala.language.implicitConversions

import com.lastroundapp.Settings

object Endpoints {
  import VenueConversions._

  type Param = (String, String)

  trait ToParam[A] {
    val paramName: String
    def paramValue(a: A): String
    def toParam(a: A): Param     = (paramName, paramValue(a))
  }
  def toParam[A](a: A)(implicit tp: ToParam[A]) = tp.toParam(a)

  trait EndpointUri[A] {
    val host = Settings.foursquareHost
    def path(a:A): Path
    def params(a:A): Set[Param]      = Set[Param]()
    def toUri(a: A): Uri             = host.withPath(path(a)).withQuery(params(a).toMap)
  }
  def toUri[A](a: A)(implicit ev:EndpointUri[A])       = ev.toUri(a)
  def toUriStirng[A](a: A)(implicit ev:EndpointUri[A]) = ev.toUri(a).toString()

  object LatLon {
    implicit object LatLonToParam extends ToParam[LatLon] {
      val paramName              = "ll"
      def paramValue(ll: LatLon) =
        List("%.3f".format(ll.lat), "%.3f".format(ll.lon)).mkString(",")
    }
  }
  sealed case class LatLon(lat:Double, lon:Double)

  object Intent {
    implicit object IntentToParam extends ToParam[Intent] {
      val paramName                 = "intent"
      def paramValue(intent:Intent) = intent match {
        case CheckIn => "checkin"
        case Browse  => "browse"
        case Global  => "global"
        case Match   => "match"
      }
    }
  }
  sealed trait Intent
  object CheckIn extends Intent
  object Browse extends Intent
  object Global  extends Intent
  object Match   extends Intent

  sealed case class Category(val id:String)
  object Category {
    val Bar        = apply("4bf58dd8d48988d116941735")
    val NightClub  = apply("4bf58dd8d48988d11f941735")
    val Pub        = apply("4bf58dd8d48988d11b941735")
    def defaultSet = Set(Bar, NightClub, Pub)
  }

  implicit object CategorySetParm extends ToParam[Set[Category]] {
    val paramName                      = "categoryId"
    def paramValue(cats:Set[Category]) = cats.map(_.id).mkString(",")
  }


  object Radius {
    val default = 2000
  }
  sealed case class Radius(val r:Int)

  implicit object RadiusToParam extends ToParam[Radius] {
    val paramName                 = "radius"
    def paramValue(radius:Radius) = radius.r.toString
  }


  sealed abstract class Endpoint

  // VenueSearchEndpoint
  object VenueSearchEndpoint {
    implicit object VenueSearchEndpointUri extends EndpointUri[VenueSearchEndpoint] {
      def path(ep:VenueSearchEndpoint)             = Path("/v2/venues/search")
      override def params(ep: VenueSearchEndpoint) = Set(
        toParam(ep.latLon), toParam(ep.intent), toParam(ep.radius), toParam(ep.categories)
      )
    }
  }
  case class VenueSearchEndpoint(
      latLon: LatLon,
      categories: Set[Category],
      intent:Intent,
      radius:Radius) extends Endpoint {
    require(radius.r < 100000, "Max supported radius is 100,000 meters")

    def this(_latLon:LatLon) =
      this(_latLon, Category.defaultSet, Browse, Radius(800))
  }

  // VenueHoursEndpoint
  object VenueHoursEndpoint {
    implicit object VenueHoursEndpointUri extends EndpointUri[VenueHoursEndpoint] {
      def path(ep:VenueHoursEndpoint) =
        Path("/v2/venues") / ep.venueId / "hours"
    }
  }
  case class VenueHoursEndpoint(venueId: VenueId) extends Endpoint

  // AuthenticatedEndpoint

  object ApiVersion {
    val default = ApiVersion(Settings.foursquareApiVersion)
    implicit object ApiVersion2Param extends ToParam[ApiVersion] {
      val paramName = "v"
      def paramValue(v:ApiVersion) = v.version.toString
    }
  }
  case class ApiVersion(version: Int) extends AnyVal

  object AccessToken {
    implicit object AccessToken2Param extends ToParam[AccessToken] {
      val paramName = "oauth_token"
      def paramValue(t:AccessToken) = t.token
    }
    val default = AccessToken(Settings.foursquareAccessToken)
  }
  case class AccessToken(token: String) extends AnyVal

  object AuthenticatedEndpoint {
    implicit def aep2EndpointUri[E: EndpointUri] = new EndpointUri[AuthenticatedEndpoint[E]] {
      def path(aep:AuthenticatedEndpoint[E]) =
        implicitly[EndpointUri[E]].path(aep.endpoint)

      override def params(aep:AuthenticatedEndpoint[E]) =
        implicitly[EndpointUri[E]].params(aep.endpoint) ++
          Set(toParam(aep.version), toParam(aep.token))
    }
  }
  sealed case class AuthenticatedEndpoint[E: EndpointUri](
      endpoint: E,
      token: AccessToken,
      version: ApiVersion) {
    def this(ep: E, token: AccessToken) = this(ep, token, ApiVersion.default)
  }
}
