function onMessage (e) {
  //var li   = document.createElement("li");
  //var span = document.createElement("span");
  //var code = document.createElement("code");

  //span.innerHTML = e.type;
  //span.innerHTML = e.data;

  //li.appendChild(span);
  //li.appendChild(code);
  //document.querySelector("body").appendChild(li)
}

function geoLocateAndSubscribeSSE () {
  var blink = document.querySelector("blink");
  blink.innerHTML = "Fetching location";
  blink.setAttribute("style", 'display:block');
  navigator.geolocation.getCurrentPosition(subscribeSSE);
}

function subscribeSSE (location) {
  var lat    = location.coords.latitude.toFixed(2);
  var lon    = location.coords.longitude.toFixed(2);
  var latLon = lat + ',' + lon;
  var esUrl  = 'http://localhost:8080/search/open-venues?ll=' + latLon +'&token=514BEI2UIDTNON3RYD3SVLKZ3ZIBOPCUZQ1IS3WIM2JZLJQT';
  var source = new EventSource(esUrl);

  document.querySelector("#lat-lon").innerHTML = latLon;

  source.addEventListener('VENUES', function(e) {
    displayVenues(JSON.parse(e.data));
  }, false);

  source.addEventListener('VENUE_HOURS', function(e) {
    markVenueAsOpen(JSON.parse(e.data));
  }, false);

  source.addEventListener('SERVER_ERROR', function(e) {
    alert("Got an API error: ");
  }, false);

  source.addEventListener('open', function(e) {
    var blink = document.querySelector("blink");
    blink.innerHTML = "Loading..."
    document.body.appendChild(blink);
  }, false);

  source.addEventListener('error', function(e) {
    console.log("An error occurred!", e);
    source.close();
    if (e.readyState == EventSource.CLOSED) {
      console.log("Connection closed");
    }
  }, false);

  function markVenueAsOpen(data) {
    var a = document.getElementById(data.venueId);
    a.classList.add("has-opening-time");
  }

  function displayVenues(data) {
    var ul = document.createElement("ul");
    document.body.removeChild(document.querySelector("blink"));

    data.forEach(function(venue) {
      var li  = document.createElement("li");
      var a   = document.createElement("a")
      a.setAttribute("href","https://foursquare.com/v/" + venue.id);
      a.setAttribute("id", venue.id);
      a.innerHTML = venue.name;
      li.appendChild(a);
      ul.appendChild(li);
    });
    document.body.appendChild(ul);
  }
}

document.addEventListener("DOMContentLoaded", geoLocateAndSubscribeSSE, false);
