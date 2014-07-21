var lastroundApp = angular.module('lastroundApp',
                                 ['ngRoute',
                                  'lastroundApp.controllers.settings',
                                  'lastroundApp.controllers.venues',
                                  'lastroundApp.controller.nav']);

lastroundApp.config(['$routeProvider', function ($routeProvider) {
  'use strict';

  function tplPath(tpl) {
    return "javascript/lastround/partials/" + tpl;
  }

  $routeProvider
    .when('/settings', {
      templateUrl: tplPath('settings.html'),
      controller: 'SettingsCtrl'
    }).when('/venues', {
      templateUrl: tplPath('venues.html'),
      controller: 'VenuesCtrl'
    }).otherwise({
      redirectTo: '/venues'
    });
}]);
