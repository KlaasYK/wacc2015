package nl.rug.wacc2015.chargingpole.model;

import java.util.Date;
import java.util.Observable;

/**
 * Class representing a charge session
 *
 */
public class ChargeSession extends Observable {

	private Date startDate;
	private Date endDate;
	private Card card;
	private double kwh;
	private double price;

	public ChargeSession(Card c) {
		card = c;
		setChanged();
	}

	public String getJSON() {
		String s = "{\"startDate\":" + startDate.getTime() + ",";
		s += "\"endDate\":" + endDate.getTime() + ",";
		s += "\"card\":" + card.getJSON() + ",";
		s += "\"kwh\":" + kwh + ",";
		s += "\"price\":" + price + "}";
		return s;
	}

	public Date getStarttime() {
		return startDate;
	}

	public void setStarttime(Date starttime) {
		this.startDate = starttime;
		setChanged();
	}

	public Date getEndtime() {
		return endDate;
	}

	public void setEndtime(Date endtime) {
		this.endDate = endtime;
		setChanged();
	}

	public Card getCard() {
		return card;
	}

	public void setCard(Card card) {
		this.card = card;
		setChanged();
	}

	public double getKwh() {
		return kwh;
	}

	public void setKwh(double kwh) {
		this.kwh = kwh;
		setChanged();
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
		setChanged();
	}
}
