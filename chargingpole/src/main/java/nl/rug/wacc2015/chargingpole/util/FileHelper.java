package nl.rug.wacc2015.chargingpole.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileHelper {

	public static JSONObject readJSONFile(Path f) throws JSONException {
		Logger l = LoggerFactory.getLogger(FileHelper.class);
		l.debug("Reading file: {}", f.toString());
		BufferedReader in = null;
		String s = "";
		try {
			in = new BufferedReader(new FileReader(f.toFile()));
			String line;
			while ((line = in.readLine()) != null) {
				s += line;
			}
		} catch (IOException ex) {
			l.error("IOException: {}", ex.getMessage());
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					l.error("IOEception: {}", e.getMessage());
				}
			}
		}
		JSONObject o = new JSONObject(s);
		return o;
	}
}
