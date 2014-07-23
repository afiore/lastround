angular.module('lastroundApp.controllers.venues',
              ['ngCookies',
               'lastroundApp.services.geolocation',
               'lastroundApp.services.venues'])

  .controller('VenuesCtrl', [
                '$scope', '$cookies', 'geolocation', 'venues',
                 function ($scope, $cookies, geolocation, venues) {
  'use strict';

  debugger;

  $scope.ordering  = "-closingDateTime";
  $scope.fetching  = true;
  $scope.authToken = $cookies.authToken;

  function onVenues (venues) {
    $scope.$apply(function () {
      $scope.venues   = venues;
      $scope.fetching = false;
    });
  }

  function onVenueHours (vh) {
    $scope.$apply(function () {
      $scope.venues.filter(function(v) {
        return v.id === vh.venueId ;
      }).forEach(function (v) {
        v.closingTime = vh.closingTime;
        v.closingDateTime = new Date(vh.closingTime.time);
      });
    });
  }

  geolocation.getCoords().then(function (coords) {
    $scope.coords = coords;
    venues.getOpenVenues(coords, onVenues, onVenueHours);
  });
}]);
