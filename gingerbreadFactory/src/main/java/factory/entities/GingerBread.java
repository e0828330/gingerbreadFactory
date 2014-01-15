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
	private Long chocolateId;
	private Long nutId;
	

	/* Involved employees */
	private Long bakerId;
	private Long qaId;
	private Long logisticsId;

	/* Ingredients suppliers */
	private Long honeySupplierId;
	private Long flourSupplierId;
	private Long firstEggSupplierId;
	private Long secondEggSupplierId;
	private Long chocolateSupplierId;
	private Long nutSupplierId;
	
	/* Order if any */
	private Long orderId;
	
	public enum Flavor {
		NORMAL,
		CHOCOLATE,
		NUT
	}
	
	private Flavor flavor;
	
	public enum State {
		PRODUCED,
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

	public Long getHoneySupplierId() {
		return honeySupplierId;
	}

	public void setHoneySupplierId(Long honeySupplierId) {
		this.honeySupplierId = honeySupplierId;
	}

	public Long getFlourSupplierId() {
		return flourSupplierId;
	}

	public void setFlourSupplierId(Long flourSupplierId) {
		this.flourSupplierId = flourSupplierId;
	}

	public Long getFirstEggSupplierId() {
		return firstEggSupplierId;
	}

	public void setFirstEggSupplierId(Long firstEggSupplierId) {
		this.firstEggSupplierId = firstEggSupplierId;
	}

	public Long getSecondEggSupplierId() {
		return secondEggSupplierId;
	}

	public void setSecondEggSupplierId(Long secondEggSupplierId) {
		this.secondEggSupplierId = secondEggSupplierId;
	}

	public Long getChocolateId() {
		return chocolateId;
	}

	public void setChocolateId(Long chocolateId) {
		this.chocolateId = chocolateId;
	}

	public Long getNutId() {
		return nutId;
	}

	public void setNutId(Long nutId) {
		this.nutId = nutId;
	}

	public Long getChocolateSupplierId() {
		return chocolateSupplierId;
	}

	public void setChocolateSupplierId(Long chocolateSupplierId) {
		this.chocolateSupplierId = chocolateSupplierId;
	}

	public Long getNutSupplierId() {
		return nutSupplierId;
	}

	public void setNutSupplierId(Long nutSupplierId) {
		this.nutSupplierId = nutSupplierId;
	}

	public Flavor getFlavor() {
		return flavor;
	}

	public void setFlavor(Flavor flavor) {
		this.flavor = flavor;
	}

	public Long getOrderId() {
		return orderId;
	}

	public void setOrderId(Long orderId) {
		this.orderId = orderId;
	}
	
	
}
