'use strict';
var lastroundApp = angular.module('lastroundApp', ['lastroundApp.services']);

lastroundApp.controller('FoosCtrl',
                       ['$rootScope', '$scope', '$q', '$timeout', 'geolocation', 'venues',
                       function ($rootScope, $scope, $q, $timeout, geolocation, venues) {

  $scope.ordering = "name";

  function onVenues (venues) {
    $scope.$apply(function () {
      $scope.venues = venues;
    });
  }

  function onVenueHours (vh) {
    $scope.$apply(function () {
      $scope.venues[vh.venueId].closingTime = vh.closingTime;
    });
  }

  geolocation.getCoords().then(function (coords) {
    $scope.coords = coords;
    venues.getOpenVenues(coords, onVenues, onVenueHours);
  });
}]);
