package nl.rug.wacc2015.chargingpole;

import java.util.LinkedList;
import java.util.List;

import static javax.swing.SwingUtilities.invokeLater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.rug.wacc2015.chargingpole.controller.*;
import nl.rug.wacc2015.chargingpole.model.*;
import nl.rug.wacc2015.chargingpole.view.*;

/**
 * Charging pole entry point
 * 
 */
public class App {

	private static final String APP_NAME = "Smart Charging Pole";
	private static final String VERSION = "v0.1";

	private static SessionStorage ss;
	private static UIController uic;

	public static void main(String[] args) {
		Logger l = LoggerFactory.getLogger(App.class);
		l.info("{} {}", APP_NAME, VERSION);
		// Parse arguments
		List<String> arglist = new LinkedList<>();
		for (int i = 0; i < args.length; ++i) {
			arglist.add(args[i]);
		}
		
		ss = new SessionStorage();
		// TODO: read form saved state (if any)
		
		uic = new UIController(ss);

		// use invokeLater to init GUI
		if (!arglist.contains("--nogui")) {
			invokeLater(new Runnable() {
				@Override
				public void run() {
					createGUI();
				}
			});
		}
		
		Thread ht = new Thread(new Heartbeat(ss));
		ht.setDaemon(true);
		ht.setName("hearbeat");
		ht.start();
	}

	private static void createGUI() {
		MainWindow mw = new MainWindow(uic);
		ss.addObserver(mw);
		mw.setVisible(true);
	}

}
