package factory.entities;

import java.io.Serializable;
import java.util.ArrayList;

import javax.jms.Destination;

public class BakerWaitingObject implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private Destination destination;
	
	private String id;
	
	public BakerWaitingObject(String id, Destination destination) {
		this.setDestination(destination);
		this.setId(id);
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
