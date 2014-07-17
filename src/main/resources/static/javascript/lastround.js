'use strict';
var lastroundApp = angular.module('lastroundApp', ['lastroundApp.services']);

lastroundApp.controller('FoosCtrl',
                       ['$scope', '$q', '$timeout', 'geolocation', 'venues',
                       function ($scope, $q, $timeout, geolocation, venues) {

  geolocation.getCoords().then(function (coords) {
    $scope.coords = coords;
    venues.getOpenVenues(coords).then(function (venueList) {
      $scope.venues = venueList;
    });
  });

  $scope.$watch("venues", function (newVal) {
    if (angular.isObject(newVal)) {
      console.info("coords has changed", newVal);
    }
  });

}]);
