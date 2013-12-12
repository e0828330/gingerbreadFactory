package factory.entities;

import java.io.Serializable;

public class GingerBread implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Long id;

	private Long chargeId;
	private Long packageId;
	
	/* Ingredients used */
	private Long honeyId;
	private Long flourId;
	private Long firstEggId;
	private Long secondEggId;

	/* Involved employees */
	private Long bakerId;
	private Long qaId;
	private Long logisticsId;

	public enum State {
		PRODUCED,
		IN_OVEN,
		BAKED,
		CONTROLLED,
		GARBAGE,
		EATEN,
		DONE
	}
	
	private State state;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getChargeId() {
		return chargeId;
	}

	public void setChargeId(Long chargeId) {
		this.chargeId = chargeId;
	}

	public Long getPackageId() {
		return packageId;
	}

	public void setPackageId(Long packageId) {
		this.packageId = packageId;
	}

	public Long getHoneyId() {
		return honeyId;
	}

	public void setHoneyId(Long honeyId) {
		this.honeyId = honeyId;
	}

	public Long getFlourId() {
		return flourId;
	}

	public void setFlourId(Long flourId) {
		this.flourId = flourId;
	}

	public Long getFirstEggId() {
		return firstEggId;
	}

	public void setFirstEggId(Long firstEggId) {
		this.firstEggId = firstEggId;
	}

	public Long getSecondEggId() {
		return secondEggId;
	}

	public void setSecondEggId(Long secondEggId) {
		this.secondEggId = secondEggId;
	}

	public Long getBakerId() {
		return bakerId;
	}

	public void setBakerId(Long bakerId) {
		this.bakerId = bakerId;
	}

	public Long getQaId() {
		return qaId;
	}

	public void setQaId(Long qaId) {
		this.qaId = qaId;
	}

	public Long getLogisticsId() {
		return logisticsId;
	}

	public void setLogisticsId(Long logisticsId) {
		this.logisticsId = logisticsId;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}
	
	
}
