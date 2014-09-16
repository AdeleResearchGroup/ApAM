$(document).ready(function() {


	/**
	 * Refresh the GUI from the actual state of the application
	 */
	var refresh = function(application) {
		
		var $switch = $('#isDay');
		var label 	= application.isDay == undefined || application.isDay == null ? "Unknown state !" :
					  application.isDay ? 	"Home is unlocked" : "Home is locked";
		
		$switch.bootstrapSwitch('labelText', "<span class='text-center'>"+label+"</span>");
	};

	/**
	 * Updates the application state
	 */
	var update = function(checked) {
		$.post( "REST", {isDay: checked}, refresh,"json");
	};
	
	/**
	 * Initialize the GUI from the current state of the application
	 */
	var initialize = function() {
		var $switch = $('#isDay');

		$switch.bootstrapSwitch();
		$switch.on('switchChange.bootstrapSwitch', function (event, checked) {
			update(checked);
		});
		
		$.get( "REST", refresh,"json");
	};


	/**
	 * Initializes the GUI and trigger initial refresh
	 */
	initialize();
	
});