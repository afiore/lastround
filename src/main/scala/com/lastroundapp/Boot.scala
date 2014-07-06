package com.lastroundapp

import akka.actor.{ActorSystem, Props}
import akka.event.Logging
import akka.routing.FromConfig
import akka.io.IO
import spray.can.Http

import com.lastroundapp.data.Endpoints.{AccessToken, LatLon}
import com.lastroundapp.services.{LastRoundActor, FoursquareClientImpl}
import com.lastroundapp.actors.{VenueSearcher, VenueHoursWorker}
import VenueSearcher.RunSearch
import com.lastroundapp.services.FoursquareClient.VenueSearchQuery

object Boot extends App {

  implicit val system = ActorSystem("on-spray-can")

  val fsClient = new FoursquareClientImpl(Logging.getLogger(system, "FoursquareClient")) 
  val venueSearcher = VenueSearcher.buildPool(fsClient)

  val service =
    system.actorOf(Props(classOf[LastRoundActor], venueSearcher), "lastround-service")

  // val q = VenueSearchQuery(LatLon(51.545, -0.0553), AccessToken.default)
  //venueSearcher ! RunSearch(q)
  //venueSearcher ! RunSearch(q)

  // start a new HTTP server on port 8080 with our service actor as the handler
  IO(Http) ! Http.Bind(service, interface = "0.0.0.0", port = 8080)
}
