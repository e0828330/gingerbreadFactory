package factory.entities;

import java.io.Serializable;

import javax.jms.Destination;

public class LogisticsEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	private Destination destination;
	
	private String correlationID;
	
	private Long id;
	
	public void setDestination(Destination jmsReplyTo) {
		this.destination = jmsReplyTo;
	}
	
	public Destination getDestination() {
		return this.destination;
	}

	public void setCorrelationID(String jmsCorrelationID) {
		this.correlationID = jmsCorrelationID;		
	}
	
	public String getCorrelationID() {
		return this.correlationID;
	}

	public void setID(Long logisticsID) {
		this.id = logisticsID;
	}
	
	public Long getID() {
		return this.id;
	}
	
	

	
	
}
