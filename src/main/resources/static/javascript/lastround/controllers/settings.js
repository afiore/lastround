angular.module('lastroundApp.controllers.settings', []).controller('SettingsCtrl', [
                '$scope', '$window',
                 function ($scope, $window) {
  'use strict';

  var defaultSettings    = {
    radius: 2000,
    categories: {},
  };
  var rawSettings        = window.localStorage.getItem("settings");
  var settings           = rawSettings ? JSON.parse(rawSettings) : defaultSettings;

  function Venue (name, code) {
    this.name = name;
    this.code = code;
  }
  Object.defineProperty(Venue.prototype, "selected", {
    get: function () {
      return !!$scope.settings.categories[this.code];
    },
    set: function (input) {
      $scope.settings.categories[this.code] = input;
    }
  });

  $scope.settings = settings;
  $scope.$watch("settings", function (oldVal, newVal) {
    if (oldVal === newVal) { return ; }
    $window.localStorage.setItem("settings", JSON.stringify($scope.settings));
  }, true);

  $scope.venueCategories = [
    new Venue("Bar", "4bf58dd8d48988d116941735"),
    new Venue("Beer Garden", "4bf58dd8d48988d117941735"),
    new Venue("Brewery", "50327c8591d4c4b30a586d5d"),
    new Venue("Champagne Bar", "52e81612bcbc57f1066b7a0e"),
    new Venue("Cocktail Bar", "4bf58dd8d48988d11e941735"),
    new Venue("Dive Bar", "4bf58dd8d48988d118941735"),
    new Venue("Gay Bar", "4bf58dd8d48988d1d8941735"),
    new Venue("Hookah Bar", "4bf58dd8d48988d119941735"),
    new Venue("Hotel Bar", "4bf58dd8d48988d1d5941735"),
    new Venue("Karaoke Bar", "4bf58dd8d48988d120941735"),
    new Venue("Lounge", "4bf58dd8d48988d121941735"),
    new Venue("Nightclub", "4bf58dd8d48988d11f941735"),
    new Venue("Other Nightlife", "4bf58dd8d48988d11a941735"),
    new Venue("Pub", "4bf58dd8d48988d11b941735"),
    new Venue("Sake Bar", "4bf58dd8d48988d11c941735"),
    new Venue("Speakeasy", "4bf58dd8d48988d1d4941735"),
    new Venue("Sports Bar", "4bf58dd8d48988d11d941735"),
    new Venue("Strip Club", "4bf58dd8d48988d1d6941735"),
    new Venue("Whisky Bar", "4bf58dd8d48988d122941735"),
    new Venue("Wine Bar", "4bf58dd8d48988d123941735")
  ];
}]);
