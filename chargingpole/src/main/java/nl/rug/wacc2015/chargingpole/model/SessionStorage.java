package nl.rug.wacc2015.chargingpole.model;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.rug.wacc2015.chargingpole.controller.ChargeController;

/**
 * Class representing a database of stored session and running session
 *
 */
public class SessionStorage extends Observable implements Observer {

	private String poleID = null;

	private double longitude = 0;
	private double latitude = 0;
	
	// TODO: read these from file
	private double kwhpersec = 0.01;
	private double priceperkwh = 0.24;
	
	// TODO: read this from file
	private String server = "http://localhost:9000/poleupdate";

	private List<ChargeSession> sessionsReadyToSend;
	private List<ChargeSession> sessionsStored;
	private ChargeSession currentsession;

	private ChargeController cc;
	private Thread chargethread;
	
	private Logger l;
	
	/**
	 * Creates an empty session storage
	 */
	public SessionStorage() {
		l = LoggerFactory.getLogger(SessionStorage.class);
		currentsession = null;
		sessionsReadyToSend = Collections.synchronizedList(new LinkedList<>());
		sessionsStored = new LinkedList<>();
		// initial set changed, still had to be displayed
		setChanged();
	}

	public String getPoleIDString() {
		if (poleID != null) {
			return poleID;
		}
		return "<No PoleID>";
	}

	/**
	 * Returns the number of sessions waiting to be send
	 * 
	 * @return int containing the number of sessions
	 */
	public int getNumSessionsReady() {
		return sessionsReadyToSend.size();
	}

	/**
	 * Add a session to the list of stored sessions Used to init from file
	 * 
	 * @param s
	 */
	public void addSessionStored(ChargeSession s) {
		sessionsStored.add(s);
	}

	/**
	 * Add a session to the list of sessions to be send
	 *
	 * @param s
	 */
	public void addSessionReadyToSend(ChargeSession s) {
		sessionsReadyToSend.add(s);
	}

	/**
	 * Start a new charging session, store it in the given sessions
	 * <code>s</code>
	 * 
	 * @param s
	 *            the new chargingsessions where the data has to be stored
	 */
	public void startSession(ChargeSession s) {
		if (currentsession != null) {
			throw new RuntimeException("Already running a session!");
		}
		// Create Charging Thread
		cc = new ChargeController(s, priceperkwh, kwhpersec);
		chargethread = new Thread(cc);
		chargethread.setDaemon(true);
		chargethread.setName("charge-t");
		chargethread.start();
		currentsession = s;
		s.addObserver(this);
		updateGUI();
	}

	/**
	 * Stop the running charging session, adds it to the list to be send
	 * @param c 
	 */
	public void stopSession(Card c) {
		if (currentsession == null) {
			throw new RuntimeException("No session running!");
		}
		// FIXME: check card validity
		cc.stopCharging();
		try {
			// 5 second delay, as it ChargeController should poll each second
			chargethread.join(5000);
		} catch (InterruptedException e) {
			l.error("SessionStorage Interrupted");
		}
		addSessionReadyToSend(currentsession);
		currentsession = null;
		updateGUI();
	}

	/**
	 * Get current running session
	 * 
	 * @return ChargeSession of active session, or null if none
	 */
	public ChargeSession getCurrentSession() {
		return currentsession;
	}

	/**
	 * Convert this storage to a JSON string
	 * 
	 * @return JSON String representing this object
	 */
	public String getJSON() {
		String s = "{\"poleID\":\"" + poleID + ",";
		s += "\"longitude\":" + longitude + ",";
		s += "\"latitude\":" + latitude + ",";
		if (currentsession != null) {
			s += "\"currentsession\":" + currentsession.getJSON() + ",";
		}
		s += "\"sessionsStored\":[";
		Iterator<ChargeSession> it = sessionsStored.iterator();
		while (it.hasNext()) {
			s += it.next().getJSON();
			if (it.hasNext()) {
				s += ",";
			}
		}
		s += "],\"sessionsReadyToSend\":[";
		it = sessionsReadyToSend.iterator();
		while (it.hasNext()) {
			s += it.next().getJSON();
			if (it.hasNext()) {
				s += ",";
			}
		}
		s += "]}";
		return s;
	}

	/**
	 * Called when GUI needs update
	 */
	private void updateGUI() {
		setChanged();
		notifyObservers();
	}

	@Override
	public void update(Observable o, Object arg) {
		updateGUI();
	}

	public String getServer() {
		return server;
	}

	public String getServerJSON() {
		String s = "{\"poleID\":\"" + poleID + ",";
		s += "\"longitude\":" + longitude + ",";
		s += "\"latitude\":" + latitude + ",";
		if (currentsession != null) {
			s += "\"charging\":true,";
		} else {
			s += "\"charging\":false,";
		}
		s += "],\"sessionsReadyToSend\":[";
		Iterator<ChargeSession> it = sessionsReadyToSend.iterator();
		while (it.hasNext()) {
			s += it.next().getJSON();
			if (it.hasNext()) {
				s += ",";
			}
		}
		s += "]}";
		return s;
	}

	/**
	 * Stores all the ready sessions into the stored list
	 */
	public void storeReadySessions() {
		sessionsStored.addAll(sessionsReadyToSend);
		sessionsReadyToSend.clear();
		updateGUI();
	}

	/**
	 * Retrieves the data from the storage
	 * @param o
	 */
	public void initStore(JSONObject o) throws JSONException{
		if (!o.has("poleID")) {
			throw new JSONException("Could not find poleID");
		}
		poleID = o.getString("poleID");
		
		if (!o.has("longitude")) {
			throw new JSONException("Could not find longitude");
		}
		longitude = o.getDouble("longitude");
		
		if (!o.has("latitude")) {
			throw new JSONException("Could not find latitude");
		}
		latitude = o.getDouble("latitude");
		
		if (!o.has("sessionsStored")) {
			throw new JSONException("Could not find sessionsStored");
		}
		JSONArray arr = o.getJSONArray("sessionsStored");
		for (int i = 0; i < arr.length(); ++i) {
			sessionsStored.add(new ChargeSession(arr.getJSONObject(i)));
		}
		arr = o.getJSONArray("sessionsreadyToSend");
		for (int i = 0; i < arr.length(); ++i) {
			sessionsReadyToSend.add(new ChargeSession(arr.getJSONObject(i)));
		}
		
		if (o.has("currentsession")) {
			currentsession = new ChargeSession(o.getJSONObject("currentsession"));
			// TODO: start the charging again, when finished
		}
		
	}
	
	/**
	 * Store the cotents to disk
	 */
	public void store() {
		// TODO:
	}

}
