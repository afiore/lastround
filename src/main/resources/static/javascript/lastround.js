'use strict';
var lastroundApp = angular.module('lastroundApp', ['lastroundApp.services']);

lastroundApp.controller('FoosCtrl',
                       ['$scope', 'geolocation', 'venues',
                       function ($scope, geolocation, venues) {

  $scope.ordering = "-closingDateTime";

  function onVenues (venues) {
    $scope.$apply(function () {
      $scope.venues = venues;
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
