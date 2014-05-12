package com.lastroundapp

import com.lastroundapp.data.Responses.FoursquareResponse
import com.lastroundapp.data.VenueHours.VenueOpeningHours

package object data {
  type VenueSearchResponse       = FoursquareResponse[List[Venue]]
  type VenueHoursResponse = FoursquareResponse[VenueOpeningHours]
}
