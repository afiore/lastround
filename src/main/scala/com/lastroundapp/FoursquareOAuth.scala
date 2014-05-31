package com.lastroundapp.auth

import scala.concurrent._

import akka.actor.ActorContext
import akka.event.LoggingAdapter
import com.lastroundapp.services.LoggablePipeline
import spray.json.DefaultJsonProtocol
import spray.client.pipelining.Post
import com.lastroundapp.data.{FSToken, FoursquareJsonProtocol, Endpoints}
import Endpoints._

class FoursquareOAuth(val log: LoggingAdapter) extends LoggablePipeline {
  import DefaultJsonProtocol._
  import FoursquareJsonProtocol._

  def getAccessToken(code:String)(implicit ac:ActorContext, ec:ExecutionContext): Future[Option[FSToken]] =
    pipeline[Option[FSToken]](Post(toUri(new TokenEndpoint(code))))
}
