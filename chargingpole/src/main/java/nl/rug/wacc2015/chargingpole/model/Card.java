package nl.rug.wacc2015.chargingpole.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class representing a card used with the electric charging pole
 *
 */
public class Card {

	private String plateNumber;

	public Card(String p) {
		this.plateNumber = p;
	}

	/**
	 * Create a Card object from a JSONObject
	 * 
	 * @param o
	 */
	public Card(JSONObject o) {
		if (!o.has("platenumber")) {
			throw new JSONException("Could not find platenumber");
		}
		plateNumber = o.getString("platenumber");
	}

	public String getJSON() {
		return "{\"platenumber\":\"" + plateNumber + "\"}";
	}

	public String getPlateNumber() {
		return plateNumber;
	}

	public void setPlateNumber(String p) {
		this.plateNumber = p;
	}

}
