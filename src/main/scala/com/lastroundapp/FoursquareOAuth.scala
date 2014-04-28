package com.lastroundapp.auth

import scala.concurrent._

import akka.actor.ActorSystem
import akka.event.Logging;
import akka.event.LoggingAdapter;

import spray.httpx.encoding.{Gzip, Deflate}

import spray.http._
import spray.util.SprayActorLogging
import HttpMethods._
import HttpHeaders._
import MediaTypes._

import spray.client.pipelining._
import com.lastroundapp.data._

import spray.json._
import DefaultJsonProtocol._
import FoursquareJsonProtocol._

class FoursquareOAuth(implicit system:ActorSystem) {
  import system.dispatcher // execution context for futures

  val log = Logging.getLogger(system, this)
  val logRequest: HttpRequest   => HttpRequest  = { r => log.debug("Request.." ++ r.toString); r }
  val logResponse: HttpResponse => HttpResponse = { r => log.debug("Response..." ++ r.toString); r }

  def getAccessToken(code:String): Future[Option[FSToken]] = {

    val pipeline =
      (encode(Gzip)
        ~> addHeader(Accept(`application/json`))
        ~> logRequest
        ~> sendReceive
        ~> logResponse
        ~> decode(Deflate)
        ~> unmarshal[Option[FSToken]])

    pipeline {
      Post(Foursquare.tokenUri(code))
    }
  }
}
