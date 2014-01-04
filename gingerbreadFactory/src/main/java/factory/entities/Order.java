package factory.entities;

import java.io.Serializable;

public class Order implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5041482297972700115L;

	private Long id;
	private int packages;
	private int donePackages;
	
	private int numNormal;
	private int numNut;
	private int numChocolate;
	
	private Long logisticsId;

	private Long timestamp;
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public int getPackages() {
		return packages;
	}

	public void setPackages(int packages) {
		this.packages = packages;
	}

	public int getDonePackages() {
		return donePackages;
	}

	public void setDonePackages(int donePackages) {
		this.donePackages = donePackages;
	}

	public int getNumNormal() {
		return numNormal;
	}

	public void setNumNormal(int numNormal) {
		this.numNormal = numNormal;
	}

	public int getNumNut() {
		return numNut;
	}

	public void setNumNut(int numNut) {
		this.numNut = numNut;
	}

	public int getNumChocolate() {
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
}
