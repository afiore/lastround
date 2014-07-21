angular.module('lastroundApp.controller.nav', []).controller('NavCtrl', [
              '$scope', '$location',
               function ($scope, $location) {
  'use strict';


  $scope.isActive = function (path) {
    return path === $location.path();
  };
}]);
