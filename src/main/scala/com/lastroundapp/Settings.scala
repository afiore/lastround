package com.lastroundapp

import com.typesafe.config.ConfigFactory

object Settings {
  private val conf = ConfigFactory.load()

  val foursquareHost          = conf.getString("lastround.foursquare.host")
  val foursquareApiVersion    = conf.getInt("lastround.foursquare.api-version")
  val foursquareAccessToken   = conf.getString("lastround.foursquare.access-token")
  val venueHoursWorkerTimeout = conf.getInt("lastround.venue-hours-worker.timeout")
  val venueSearcherTimeout    = conf.getInt("lastround.venue-searcher.timeout")
}
