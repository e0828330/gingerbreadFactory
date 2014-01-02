package factory.interfaces;

import factory.entities.GingerBread.Flavor;

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
	 * Sets the amount of a specific flavor
	 * For instance one can do
	 * 	setAmount(NUT, 4);
	 * 	setAmount(NORMAL, 2);
	 * 
	 * @param flavor
	 * @param amount
	 */
	public void setAmount(Flavor flavor, int amount);
}
