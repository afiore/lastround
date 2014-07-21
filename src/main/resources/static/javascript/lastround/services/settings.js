angular.module("lastroundApp.services.settings", [])
  .factory("defaults", [function () {
    "use strict";

    var radius          = 2000;
    var venueCategories = {
     "4bf58dd8d48988d11e941735": true, //Cocktail Bar
     "4bf58dd8d48988d11f941735": true, //Nightlife
     "4bf58dd8d48988d11b941735": true  //Pub
    };

    return {
      "SETTINGS_KEY": "settings",
      "SETTINGS": {
        "radius": radius,
        "venueCategories": venueCategories,
      }
    };
  }])
  .factory("settings",
           ['$window', "defaults",
           function ($window, defaults) {
  "use strict";

  return {
    writeSettings: function (suppliedSettings) {
      $window
        .localStorage
         .setItem(
           defaults.SETTINGS_KEY,
           JSON.stringify(suppliedSettings));
    },

    readSettings: function () {
      var savedSettings = $window.localStorage.getItem(defaults.SETTINGS_KEY);
      return savedSettings ? JSON.parse(savedSettings) : defaults.SETTINGS;
    }
  };
}]);
