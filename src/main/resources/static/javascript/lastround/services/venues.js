angular.module("lastroundApp.services.venues",
              ["lastroundApp.services.settings"])
  .factory("venues",
          ["$location", "$window", "$interpolate", "settings",
          function ($location, $window, $intrpl, settingsSrv) {
    "use strict";

    function endpointUrl(coords) {
      var settings        = settingsSrv.readSettings();
      var venueCategories = _.reduce(settings.venueCategories, function (acc, selected, category) {
        return selected ? acc.concat([category]) : acc;
      }, []).join(",");

      var tpl             =
        "{{proto}}://api.{{host}}:{{port}}/search/open-venues?"+
        "ll={{latLon}}&radius={{radius}}&categories={{categories}}"+
        "&datetime={{date}}&token={{token}}";

      var context = {
        token: "514BEI2UIDTNON3RYD3SVLKZ3ZIBOPCUZQ1IS3WIM2JZLJQT",
        latLon: coords.latitude.toFixed(2) + "," + coords.longitude.toFixed(2),
        host: $location.host(),
        port: $location.port(),
        radius: settings.radius,
        categories: venueCategories,
        proto: $location.protocol(),
        date: (new Date()).valueOf(),
      };

      return $intrpl(tpl)(context);
    }

    return {
      getOpenVenues: function (latLon, onVenues, onVenueHours) {
        var source  = new $window.EventSource(endpointUrl(latLon));

        source.addEventListener('VENUES', function(e) {
          var venues = angular.fromJson(e.data).map(function (v) {
            var date = new Date();
            date.setHours(0);
            date.setMinutes(0);
            v.closingDateTime = date;
            return v;
          });

          onVenues(venues);
        }, false);

        source.addEventListener('VENUE_HOURS', function(e) {
          var data = angular.fromJson(e.data);
          onVenueHours(data);
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
      }
    };
  }]);
