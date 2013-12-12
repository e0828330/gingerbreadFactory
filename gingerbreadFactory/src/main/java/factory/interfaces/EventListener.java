package factory.interfaces;

import java.util.List;

import factory.entities.GingerBread;
import factory.entities.Ingredient;

public interface EventListener {

	/**
	 * Called when oven content changes
	 * @param ovenContent
	 */
	public void onOvenChanged(List<GingerBread> ovenContent);
	
	/**
	 * Called when new ingredient gets delivered
	 * @param item
	 */
	public void onIngredientDelivered(Ingredient item);
	
	/**
	 * Called when the gingerbread status changed, gives a list of current state
	 * @param list
	 */
	public void onGingerBreadStateChange(List<GingerBread> list);

}