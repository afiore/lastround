'use strict';
var lastroundApp = angular.module('lastroundApp', ['lastroundApp.services']);

lastroundApp.controller('FoosCtrl',
                       ['$scope', 'geolocation', 'venues',
                       function ($scope, geolocation, venues) {

  $scope.ordering = "name";

  function onVenues (venues) {
    $scope.$apply(function () {
      $scope.venues = venues;
      $scope.venues.forEach(function(v) {
        v.closingDateTime = new Date();
        v.closingDateTime.setHours(0);
        v.closingDateTime.setMinutes(0);
      });
    });
  }

  function onVenueHours (vh) {
    var closingTime = vh.closingTime;
    $scope.$apply(function () {
      var venue = $scope.venues.filter(function(v) { return v.id === vh.venueId})[0]
      venue.closingTime = closingTime;
      var date = new Date();
      date.setHours(closingTime.hours);
      date.setMinutes(closingTime.minutes);
      venue.closingDateTime = date;
    });
  }

  geolocation.getCoords().then(function (coords) {
    $scope.coords = coords;
    venues.getOpenVenues(coords, onVenues, onVenueHours);
  });
}]);
