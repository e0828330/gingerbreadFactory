package factory.interfaces;

import java.util.List;

import factory.entities.GingerBread;
import factory.entities.Ingredient;

public interface Monitor {
	/**
	 * Adds a listener for events (so that GUI does not have to poll)
	 * 
	 * @param listener
	 */
	public void setListener(EventListener listener);

	/**
	 * Gets current gingerbread list, useful at startup
	 * updates should be done using events
	 * 
	 * @return
	 */
	public List<GingerBread> getGingerBreads();
	
	/**
	 * Gets current ingredient list, useful at startup
	 * updates should be done using events
	 * 
	 * @return
	 */
	public List<Ingredient> getIngredients();
	
	/**
	 * Gets current oven content, useful at startup
	 * updates should be done using events
	 * 
	 * @return
	 */
	public List<GingerBread> getOvenContent();
}
