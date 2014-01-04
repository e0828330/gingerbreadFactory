package factory.entities;

import java.io.Serializable;

public class Order implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Long id;
	private Integer packages;
	private Integer donePackages;
	
	private Integer numNormal;
	private Integer numNut;
	private Integer numChocolate;
	
	private Long logisticsId;

	private Long timestamp;
	
	public enum State {
		OPEN,
		IN_PROGRESS,
		DONE,
		MOVED
	}
	
	private State state;
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getPackages() {
		return packages;
	}

	public void setPackages(int packages) {
		this.packages = packages;
	}

	public Integer getDonePackages() {
		return donePackages;
	}

	public void setDonePackages(int donePackages) {
		this.donePackages = donePackages;
	}

	public Integer getNumNormal() {
		return numNormal;
	}

	public void setNumNormal(int numNormal) {
		this.numNormal = numNormal;
	}

	public Integer getNumNut() {
		return numNut;
	}

	public void setNumNut(int numNut) {
		this.numNut = numNut;
	}

	public Integer getNumChocolate() {
		return numChocolate;
	}

	public void setNumChocolate(int numChocolate) {
		this.numChocolate = numChocolate;
	}

	public Long getLogisticsId() {
		return logisticsId;
	}

	public void setLogisticsId(Long logisticsId) {
		this.logisticsId = logisticsId;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}
}
