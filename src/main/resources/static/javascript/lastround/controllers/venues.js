angular.module('lastroundApp.controllers.venues',
              ['lastroundApp.services.geolocation',
               'lastroundApp.services.venues'])
  .controller('VenuesCtrl', [
                '$scope', 'geolocation', 'venues',
                 function ($scope, geolocation, venues) {
  'use strict';

  $scope.ordering = "-closingDateTime";
  $scope.fetching = true;

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
