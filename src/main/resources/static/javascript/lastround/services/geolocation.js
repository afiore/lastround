angular.module("lastroundApp.services.geolocation", [])
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
  }]);
