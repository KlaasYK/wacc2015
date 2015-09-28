package nl.rug.wacc2015.chargingpole.controller;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.rug.wacc2015.chargingpole.model.SessionStorage;

public class Heartbeat implements Runnable {

	private SessionStorage ss;
	private WebTarget resource;

	private Logger l;

	/**
	 * Send an update every 30 seconds
	 */
	private static final long HEART_RATE = 1000 * 10;

	public Heartbeat(SessionStorage ss) {
		this.ss = ss;
		l = LoggerFactory.getLogger(Heartbeat.class);
		Client c = ClientBuilder.newClient();
		resource = c.target(ss.getServer());
	}

	private void doRequest() {
		// FIXME: use a timeout or something
		String data = ss.getServerJSON();
		try {
			Builder req = resource.request();
			req.accept(MediaType.APPLICATION_JSON);
			req.acceptEncoding("utf-8");
			Response res = req.put(Entity.entity(data, MediaType.APPLICATION_JSON));
			switch (res.getStatus()) {
			case 200:
				// Do nothing
				break;
			case 404:
				l.warn("API not found");
				return;
			default:
				l.warn("Server error: {}", res.getStatus());
				l.debug("{}", res.readEntity(String.class));
				return;
			}
			String resdata = res.readEntity(String.class);
			try {
				JSONObject o = new JSONObject(resdata);
				if (o.has("success") && o.getBoolean("success")) {
					ss.storeReadySessions();
				} else {
					l.warn("Server error: {}", o.getString("errormsg"));
				}
			} catch (JSONException e) {
				l.warn("Malformed response: {}", resdata);
			}
			
		} catch (ProcessingException ex) {
			l.warn("Server not reachable");
		}
	}

	@Override
	public void run() {
		l.info("Heartbeat started");
		while (true) {
			doRequest();
			try {
				Thread.sleep(HEART_RATE);
			} catch (InterruptedException e) {
				l.error("Heartbeat interrupted");
			}
		}
	}

}
