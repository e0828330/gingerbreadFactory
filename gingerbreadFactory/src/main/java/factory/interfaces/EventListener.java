package factory.interfaces;

import java.util.List;

import factory.entities.GingerBread;
import factory.entities.Ingredient;
import factory.entities.Order;

public interface EventListener {

	/**
	 * Called when oven content changes
	 * @param ovenContent
	 */
	public void onOvenChanged(List<GingerBread> ovenContent);
	
	/**
	 * Called when new ingredient gets delivered or old ones get used
	 * @param item
	 */
	public void onIngredientChanged(List<Ingredient> list);
	
	/**
	 * Called when the gingerbread status changed, gives a list of current state
	 * @param list
	 */
	public void onGingerBreadStateChange(List<GingerBread> list);

	/**
	 * Called when the state of orders changes, gives a list of current orders
	 * 
	 * @param list
	 */
	public void onOrderChanged(List<Order> list);
	
}
