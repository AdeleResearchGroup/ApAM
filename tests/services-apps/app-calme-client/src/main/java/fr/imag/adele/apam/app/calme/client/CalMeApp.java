package fr.imag.adele.apam.app.calme.client;

import fr.imag.adele.apam.app.calme.api.ApamCalendar;
import fr.imag.adele.apam.app.calme.api.CalendarManager;

public class CalMeApp {

	CalendarManager calendarManager;
	
	boolean active;
	
	public void start() throws InterruptedException{
		System.out.println("starting app");
		active=true;
		
		while(active){
			
			System.out.println("--- List of calendars ----");
			
			for(ApamCalendar cal:calendarManager.getCalendars()){
				System.out.println("*"+cal.summary);
			}
			
			System.out.println("--- END: List of calendars ----");
			
			Thread.sleep(4000);
		}
		
	}
	
	public void stop(){
		System.out.println("stopping app");
		active=false;
	}
	
}
