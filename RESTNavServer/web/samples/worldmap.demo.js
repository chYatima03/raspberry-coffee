/*
 * @author Olivier Le Diouris
 */
"use strict";

var worldMap;
var currentDate;

var init = function () {
	worldMap = new WorldMap('mapCanvas', 'GLOBE');
};

var DEFAULT_TIMEOUT = 60000;

var getDeferred = function(
		url,                          // full api path
		timeout,                      // After that, fail.
		verb,                         // GET, PUT, DELETE, POST, etc
		happyCode,                    // if met, resolve, otherwise fail.
		data,                         // payload, when needed (PUT, POST...)
		show) {                       // Show the traffic [true]|false
	if (show === undefined) {
		show = true;
	}
	if (show === true) {
		document.body.style.cursor = 'wait';
	}
	var deferred = $.Deferred(),  // a jQuery deferred
			url = url,
			xhr = new XMLHttpRequest(),
			TIMEOUT = timeout;

	var req = verb + " " + url;
	if (data !== undefined && data !== null) {
		req += ("\n" + JSON.stringify(data, null, 2));
	}

	xhr.open(verb, url, true);
	xhr.setRequestHeader("Content-type", "application/json");
	if (data === undefined) {
		xhr.send();
	} else {
		xhr.send(JSON.stringify(data));
	}

	var requestTimer = setTimeout(function() {
		xhr.abort();
		var mess = { message: 'Timeout' };
		deferred.reject(408, mess);
	}, TIMEOUT);

	xhr.onload = function() {
		clearTimeout(requestTimer);
		if (xhr.status === happyCode) {
			deferred.resolve(xhr.response);
		} else {
			deferred.reject(xhr.status, xhr.response);
		}
	};
	return deferred.promise();
};

var getSunMoonGP = function(when) {
	var url = "/sun-moon-gp";
	// Add date
	url += ("?at=" + when);
	return getDeferred(url, DEFAULT_TIMEOUT, 'GET', 200, null, false);
};

var getAstroData = function(when, callback) {
	var getData = getSunMoonGP(when);
	getData.done(function(value) {
		var json = JSON.parse(value);
		if (callback !== undefined) {
			callback(json);
		} else {
			console.log(JSON.stringify(json, null, 2));
		}
	});
	getData.fail(function(error, errmess) {
		var message;
		if (errmess !== undefined) {
			if (errmess.message !== undefined) {
				message = errmess.message;
			} else {
				message = errmess;
			}
		}
		errManager("Failed to get the station list..." + (error !== undefined ? error : ' - ') + ', ' + (message !== undefined ? message : ' - '));
	});
};

/*
 *  Demo features
 */

var position = {
	lat: 38,
	lng: -122
};

const MINUTE = 60000;

var initAjax = function () {

	var date = new Date();
	var offset = date.getTimezoneOffset() * MINUTE; // in millisecs

	currentDate = new Date().getTime() + offset;
	console.log("Starting (now) at " + new Date(currentDate).format("Y-M-d H:i:s UTC"));

	var interval = setInterval(function () {
		tickClock();
	}, 1000);

};

var tickClock = function () {

	position.lng += 1;
	if (position.lng > 360) position.lng -= 360;
	if (position.lng > 180) position.lng -= 360;

	var json = { // Changed position, increment time
		Position: {
			lat: position.lat,
			lng: position.lng
		},
		GPS: new Date(currentDate)
	};
	onMessage(json); // Position and date

	currentDate += (10 * MINUTE);
	var mess = "Time is now " + new Date(currentDate).format("Y-M-d H:i:s UTC");
	var dateField = document.getElementById("current-date");
	if (dateField !== undefined) {
		dateField.innerText = mess;
	} else {
		console.log(mess);
	}
};

var onMessage = function (json) {
	try {
		var errMess = "";

		try {
			var latitude = json.Position.lat;
//          console.log("latitude:" + latitude)
			var longitude = json.Position.lng;
//          console.log("Pt:" + latitude + ", " + longitude);
			events.publish('pos', {
				'lat': latitude,
				'lng': longitude
			});
		} catch (err) {
			errMess += ((errMess.length > 0 ? ", " : "Cannot read ") + "position");
		}
		try {
			var gpsDate = json.GPS;
			events.publish('gps-time', gpsDate);
		} catch (err) {
			errMess += ((errMess.length > 0 ? ", " : "Cannot read ") + "GPS Date (" + err + ")");
		}
		if (errMess !== undefined)
			displayErr(errMess);
	}
	catch (err) {
		displayErr(err);
	}
};
