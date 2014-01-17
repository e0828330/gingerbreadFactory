package factory.entities;

/**
 * Stores which factory has which load on request of orderID
 * @author martin
 *
 */
public class LoadEntity {

	private int load;
	
	private Long orderID;
	
	private int factoryID;

	public int getLoad() {
		return load;
	}

	public void setLoad(int load) {
		this.load = load;
	}

	public Long getOrderID() {
		return orderID;
	}

	public void setOrderID(Long orderID) {
		this.orderID = orderID;
	}

	public int getFactoryID() {
		return factoryID;
	}

	public void setFactoryID(int factoryID) {
		this.factoryID = factoryID;
	}
	
}
