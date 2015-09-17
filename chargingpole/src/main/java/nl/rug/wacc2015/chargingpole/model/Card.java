package nl.rug.wacc2015.chargingpole.model;

/**
 * Class representing a card used with the electric charging pole
 *
 */
public class Card {

	private String plateNumber;
	
	public Card(String p) {
		this.plateNumber = p;
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
