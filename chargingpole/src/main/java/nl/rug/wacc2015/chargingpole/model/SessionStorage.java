package nl.rug.wacc2015.chargingpole.model;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

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
	
	private double kwhpersec = 0.01;
	private double priceperkwh = 0.24;
	
	private String server = "http://127.0.0.1:9000/poleupdate";

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

	public String getPoleID() {
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

	public List<ChargeSession> getReadySessions() {
		return sessionsReadyToSend;
	}

	public List<ChargeSession> getStoredSessions() {
		return sessionsStored;
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
		s += "\"currentsession\":" + currentsession.getJSON() + ",";
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
		// TODO Auto-generated method stub
		return null;
	}

}
