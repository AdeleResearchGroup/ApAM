var WEB_SERVICE_PATH = "rest";

// call getApplicationStatus
// if case of success, the json oject is composed of :
// - status - int - 0(GREEN_STATE), 1(ORANGE_STATE), 2(RED_STATE)
// - errorCodes - array of String - SERVICE_DISABLED, NO_SMOKE_DETECTOR_DEVICES, NO_WARNING_DEVICES, NO_PUMP_DEVICES
function getApplicationStatus(getStatusComplete, getStatusError) {
	$.ajax({
		url : WEB_SERVICE_PATH + "/status",
		dataType : "json",
		cache : false
	}).done(getStatusComplete).error(getStatusError);
}


// call enable method.
function enable(complete, error) {
	console.log("enable called (complete=" + complete + ", error=" + error);
	$.ajax({
		url : WEB_SERVICE_PATH + "/enable",
		type : "POST",
		cache : false
	}).done(complete).error(error);
}


//call disable method.
function disable(complete, error) {
	console.log("disable called (complete=" + complete + ", error=" + error);
	$.ajax({
		url : WEB_SERVICE_PATH + "/disable",
		type : "POST",
		cache : false
	}).done(complete).error(error);
}


// call devices method from server
function getDevices(complete, error) {
	console.log("getDevices from service");
	$.ajax({
		url : WEB_SERVICE_PATH + "/devices",
		type: "GET",
		cache : false,
		dataType: "json"
	}).done(complete).error(error);
}
