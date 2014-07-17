"use strict";
angular.module("lastroundApp.services", [])
  .factory("geolocation", ["$q", function ($q) {
    'use strict';

    return {
      getCoords: function () {
        var deferred   = $q.defer(),
            geoLocOpts = { timeout: 10000 };

        function onSuccess (pos) {
          deferred.resolve(pos.coords);
        }
        function onError () {}

        navigator
          .geolocation
          .getCurrentPosition(onSuccess, onError, geoLocOpts);

        return deferred.promise;
      }
    };
  }])
  .factory("venues",
          ["$q", "$location", "$window", "$interpolate", "$rootScope",
          function ($q, $location, $window, $intrpl, $rootScope) {

    function endpointUrl(coords) {
      var tpl =
        "{{proto}}://{{host}}:{{port}}/search/open-venues?"+
        "ll={{latLon}}&datetime={{date}}&token={{token}}";

      var context = {
        token: "514BEI2UIDTNON3RYD3SVLKZ3ZIBOPCUZQ1IS3WIM2JZLJQT",
        latLon: coords.latitude.toFixed(2) + "," + coords.longitude.toFixed(2),
        host: $location.host(),
        port: $location.port(),
        proto: $location.protocol(),
        date: (new Date()).valueOf(),
      };

      return $intrpl(tpl)(context);
    }

    return {
      getOpenVenues: function (latLon) {
        var defered = $q.defer();
        var source  = new $window.EventSource(endpointUrl(latLon));
        var venuesObj = {};

        source.addEventListener('VENUES', function(e) {
          var venues = angular.fromJson(e.data);

          angular.forEach(venues, function (venue) {
            venuesObj[venue.id] = venue;
          });

          defered.resolve(venuesObj);
        }, false);

        source.addEventListener('VENUE_HOURS', function(e) {
          console.info("Got VENUE_HOURS", e);
          var data = angular.fromJson(e.data);
          venuesObj[data.venueId].closingTime = data.closingTime;
          //
        }, false);

        source.addEventListener('SERVER_ERROR', function() {
          $window.alert("Got an API error: ");
        }, false);

        source.addEventListener('open', function() {
          console.info("Connection open...");
        }, false);

        source.addEventListener('error', function(e) {
          source.close();
          if (e.readyState == $window.EventSource.CLOSED) {
            console.log("Connection closed");
          }
        }, false);

        return defered.promise;
      }
    };
  }]);
