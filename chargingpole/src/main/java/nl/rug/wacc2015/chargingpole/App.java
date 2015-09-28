package nl.rug.wacc2015.chargingpole;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import static javax.swing.SwingUtilities.invokeLater;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.rug.wacc2015.chargingpole.controller.*;
import nl.rug.wacc2015.chargingpole.model.*;
import nl.rug.wacc2015.chargingpole.util.FileHelper;
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
		
		// Read from saved state
		ss = new SessionStorage();
		String poleid = null;
		if (!arglist.contains("--poleid")) {
			l.warn("Using default poleid");
			poleid = "TEST1234";
		} else {
			for (int i = 0; i < args.length; ++i) {
				if (args[i].equals("--poleid")) {
					poleid = args[i+1];
					break;
				}
			}
		}
		if (poleid == null) {
			l.error("Could not find poleid");
			System.out.println("Usage: javar -jar <filename> [--nogui] [--poleid <poleid>]");
			System.exit(0);
		}
		Path p = Paths.get("store/"+poleid+".pole");
		if (Files.exists(p)) {
			try {
				JSONObject o = FileHelper.readJSONFile(p);
				ss.initStore(o);
			} catch (JSONException ex) {
				l.error("Could not load file: {} Cause: {}", p.toString(), ex.getMessage());
			}
		} else {
			l.warn("No file found, creating an empty one");
			ss.initEmpty(poleid);
		}
		
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
