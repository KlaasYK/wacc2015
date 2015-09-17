package nl.rug.wacc2015.chargingpole.controller;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.rug.wacc2015.chargingpole.model.ChargeSession;

/**
 * Thread in charge of charging the current session
 *
 */
public class ChargeController implements Runnable {

	private boolean charging;
	private ChargeSession s;
	private double pricePerKWh;
	private double kwhPerSec;

	private static final long SECOND = 1000;

	private Logger l;

	public ChargeController(ChargeSession s, double pricePerKWh, double kwhPerSec) {
		this.s = s;
		this.pricePerKWh = pricePerKWh;
		this.kwhPerSec = kwhPerSec;
		l = LoggerFactory.getLogger(ChargeController.class);
	}

	@Override
	public void run() {
		startCharging();
		s.setStarttime(new Date());
		l.info("Charging started");
		while (getCharging()) {
			double kwh = s.getKwh() + kwhPerSec;
			s.setKwh(kwh);
			s.setPrice(kwh * pricePerKWh);
			s.notifyObservers();
			try {
				Thread.sleep(SECOND);
			} catch (InterruptedException e) {
				l.error("Charging interrupted");
			}
		}
		s.setEndtime(new Date());
		l.info("Charging stopped");
	}

	private synchronized void startCharging() {
		charging = true;
	}

	private synchronized boolean getCharging() {
		return charging;
	}

	public synchronized void stopCharging() {
		charging = false;
	}
}
