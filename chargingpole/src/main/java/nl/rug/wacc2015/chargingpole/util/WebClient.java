package nl.rug.wacc2015.chargingpole.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebClient {

	private Logger l;
	
	public WebClient() {
		l = LoggerFactory.getLogger(WebClient.class);
	}
	
	public boolean sendHeartbeat(String URI, String message) {
		l.debug("Sending Heartbeat");
		
		
		return false;
	}
	
}
