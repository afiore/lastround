package com.lastroundapp.data

import scala.language.implicitConversions
import scala.util.{Success, Try}

import spray.http._
import spray.http.Uri._
import spray.httpx.unmarshalling.{FromStringDeserializer, MalformedContent}

object Endpoints {
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
    def params(a:A): Set[Param] = Set[Param]()
    def toUri(a: A): Uri = host.withPath(path(a)).withQuery(params(a).toMap)
  }
  def toUri[A](a: A)(implicit ev:EndpointUri[A])       = ev.toUri(a)
  def toUriStirng[A](a: A)(implicit ev:EndpointUri[A]) = ev.toUri(a).toString()

  object LatLon {
    implicit val string2LatLon = new FromStringDeserializer[LatLon] {
      def apply(s: String) = {
        val tryLatLon = s.split(',').map { (s:String) => Try(s.toDouble) }
        tryLatLon match {
          case Array(Success(lat:Double), Success(lon:Double)) => Right(LatLon(lat, lon))
          case _ => Left(MalformedContent(s"Cannot parse string $s as a LatLon"))
        }
      }
    }
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

  sealed case class Category(id: String)
  object Category {
    implicit val string2CategorySet = new FromStringDeserializer[Set[Category]] {
      def apply(s: String) = {
        val categories = s.split(',').map { (s:String) => Category(s) }.toSet
        if (categories.isEmpty) Left(MalformedContent(s"Empty categories' set"))
        else Right(categories)
      }
    }

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
    val default = Radius(2000)
  }
  sealed case class Radius(r:Int)

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
    require(radius.r < 100000, "Max supported radius is 100KMs")

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
    val default = AccessToken("test-token")

    implicit val string2AccessToken = new FromStringDeserializer[AccessToken] {
      def apply(s: String) =
        if (s.isEmpty) Left(MalformedContent("Blank token")) else Right(AccessToken(s))
    }
    implicit object AccessToken2Param extends ToParam[AccessToken] {
      val paramName = "oauth_token"
      def paramValue(t:AccessToken) = t.token
    }
  }
  case class AccessToken(token: String) extends AnyVal

  // OAuth authentication endpoints
  val fsHost = "https://foursquare.com"
  val oauthParams =
    Set(
      "client_id"     -> Settings.foursquareClientId,
      "client_secret" -> Settings.foursquareSecret,
      "redirect_uri"  -> Settings.foursquareRedirectUrl,
      "grant_type"    -> "authorization_code"
    )

  object AuthEndpoint {
    implicit object AuthEndpointUri extends EndpointUri[AuthEndpoint] {
      override val host = fsHost
      def path(ep:AuthEndpoint) = Path("/oauth2/authenticate")
      override def params(ep:AuthEndpoint) = oauthParams ++ Set("response_type" -> "code")
    }
  }
  case class AuthEndpoint()

  object TokenEndpoint {
    implicit object TokenEndpointUri extends EndpointUri[TokenEndpoint] {
      override val host = fsHost
      def path(ep:TokenEndpoint) = Path("/oauth2/access_token")
      override def params(ep:TokenEndpoint) = oauthParams ++ Set("code" -> ep.code)
    }
  }
  sealed case class TokenEndpoint(code:String)

  //Wrapper typeclass allowing to add token and api version to each endpoint
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
