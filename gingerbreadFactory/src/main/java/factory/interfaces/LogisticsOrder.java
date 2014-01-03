package factory.interfaces;

import factory.entities.Order;

/**
 * Interface for LogisticsOrder thread used by GUI
 * 
 */
public interface LogisticsOrder extends Runnable {

	/**
	 * Places a new order
	 * 
	 * @param order
	 */
	public void placeOrder(Order order);
}
