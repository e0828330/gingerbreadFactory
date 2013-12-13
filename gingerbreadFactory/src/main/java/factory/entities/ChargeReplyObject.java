package factory.entities;

import java.io.Serializable;
import java.util.ArrayList;

import javax.jms.Destination;

public class ChargeReplyObject implements Serializable {

	private static final long serialVersionUID = 1L;

	private ArrayList<GingerBread> charge;
	
	private Destination destination;
	
	private String id;
	
	public ChargeReplyObject(ArrayList<GingerBread> charge, String id, Destination destination) {
		this.setCharge(charge);
		this.setDestination(destination);
		this.setId(id);
	}

	public ArrayList<GingerBread> getCharge() {
		return charge;
	}
	
	public void setCharge(ArrayList<GingerBread> charge) {
		this.charge = charge;
	}

	public Destination getDestination() {
		return destination;
	}

	public void setDestination(Destination destination) {
		this.destination = destination;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	
}
