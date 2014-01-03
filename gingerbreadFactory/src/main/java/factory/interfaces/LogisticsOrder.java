package factory.interfaces;

import factory.entities.Order;

/**
 * Interface for LogisticsOrder thread used by GUI
 * 
 */
public interface LogisticsOrder extends Runnable {
	
	/**
	 * Sets the id of this Order
	 * @param id
	 */
	public void setId(Long id);
	

	/**
	 * Places a new order
	 * 
	 * @param order
	 */
	public void placeOrder(Order order);
}
