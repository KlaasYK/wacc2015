package nl.rug.wacc2015.chargingpole.controller;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static javax.swing.SwingUtilities.invokeLater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.rug.wacc2015.chargingpole.model.*;

/**
 * Controller for handling actions from the UI
 *
 */
public class UIController implements ActionListener {

	private SessionStorage ss;

	private Logger l;

	public UIController(SessionStorage ss) {
		l = LoggerFactory.getLogger(UIController.class);
		this.ss = ss;
	}

	/**
	 * Called car is connected
	 */
	private void carConnect() {
		invokeLater(new Runnable() {
			@Override
			public void run() {
				l.info("Connecting");
				// TODO: read card
				l.debug("Reading card");
				Card c = new Card("AA-BB-07");
				// Create session
				ChargeSession s = new ChargeSession(c);
				// Update session store
				try {
					ss.startSession(s);
				} catch (RuntimeException e) {
					l.warn("RuntimeException: {}", e.getMessage());
				}
			}
		});
	}

	/**
	 * Called when car is disconnected
	 */
	private void carDisconnect() {
		invokeLater(new Runnable() {
			@Override
			public void run() {
				// TODO: read card
				l.info("Disconnecting");
				l.debug("Reading card");
				Card c = new Card("AA-BB-07");
				try {
					ss.stopSession(c);
				} catch (RuntimeException e) {
					l.warn("RuntimeException: {}", e.getMessage());
				}
			}
		});
	}

	/**
	 * Called when main window is opened
	 */
	private void startUp() {
		l.debug("Main Window open");
		ss.notifyObservers();
	}

	/**
	 * Called when main window is closed
	 */
	private void shutdown() {
		ss.store();
		l.info("Shutting down");
		System.exit(0);
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		switch (event.getActionCommand()) {
		case "POLE.RUNNING":
			startUp();
			break;
		case "POLE.SHUTDOWN":
			shutdown();
			break;
		case "CAR.CONNECT":
			carConnect();
			break;
		case "CAR.DISCONNECT":
			carDisconnect();
			break;
		default:
			l.warn("Unknown ActionCommand: {}", event.getActionCommand());
			break;
		}
	}

}
