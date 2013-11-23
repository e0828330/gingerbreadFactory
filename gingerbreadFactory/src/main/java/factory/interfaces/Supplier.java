package factory.interfaces;

import factory.entities.Ingredient;

/**
 * Interface for supplier thread, used by GUI
 * 
 *
 */
public interface Supplier extends Runnable {
	/**
	 * Sets the suppliers ID
	 * 
	 * @param id
	 */
	public void setId(Long id);

	/**
	 * Tells supplier about the type and amount ordered
	 *  
	 * @param amount
	 * @param type
	 */
	public void placeOrder(int amount, Ingredient.Type type);
}
