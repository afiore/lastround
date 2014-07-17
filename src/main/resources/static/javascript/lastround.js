'use strict';
var lastroundApp = angular.module('lastroundApp', ['lastroundApp.services']);

lastroundApp.controller('FoosCtrl',
                       ['$rootScope', '$scope', '$q', '$timeout', 'geolocation', 'venues',
                       function ($rootScope, $scope, $q, $timeout, geolocation, venues) {

  function onVenues (venues) {
    $scope.venues = venues;
    $scope.$digest();
  }

  function onVenueHours (vh) {
    $scope.venues[vh.venueId].closingTime = vh.closingTime;
    $scope.$digest();
  }

  geolocation.getCoords().then(function (coords) {
    $scope.coords = coords;
    venues.getOpenVenues(coords, onVenues, onVenueHours);
  });
}]);
