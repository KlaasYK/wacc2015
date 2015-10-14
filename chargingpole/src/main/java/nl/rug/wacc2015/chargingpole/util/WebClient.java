package nl.rug.wacc2015.chargingpole.util;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebClient {

	private Logger l;
	
	private WebTarget resource;
	
	public WebClient(String server) {
		l = LoggerFactory.getLogger(WebClient.class);
		Client c = ClientBuilder.newClient();
		resource = c.target(server);
	}
	
	public boolean sendSession(String URI, String data) {
		l.debug("Sending status");
		WebTarget t = resource.path(URI);
		l.debug(t.getUri().toString());
		try {
			Builder req = t.request();
			req.accept(MediaType.APPLICATION_JSON);
			req.acceptEncoding("utf-8");
			Response res = req.put(Entity.entity(data, MediaType.APPLICATION_JSON));
			switch (res.getStatus()) {
			case 200:
				// Do nothing
				break;
			case 404:
				l.warn("API not found");
				return false;
			default:
				l.warn("Server error: {}", res.getStatus());
				l.debug("{}", res.readEntity(String.class));
				return false;
			}
			String resdata = res.readEntity(String.class);
			try {
				JSONObject o = new JSONObject(resdata);
				if (o.has("success") && o.getBoolean("success")) {
					return true;
				} else {
					l.warn("Server error: {}", o.getString("errormsg"));
				}
			} catch (JSONException e) {
				l.warn("Malformed response: {}", resdata);
			}
			
		} catch (ProcessingException ex) {
			l.warn("Server not reachable");
		}
		return false;
	}
	
}
