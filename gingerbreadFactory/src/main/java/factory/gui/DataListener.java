package factory.gui;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;

import factory.entities.GingerBread;
import factory.entities.Ingredient;
import factory.interfaces.EventListener;

public class DataListener implements EventListener {

	private MainWindow window;

	public DataListener(MainWindow window) {
		this.window = window;
	}

	public void onOvenChanged(List<GingerBread> ovenContent) {
		try {
			window.updateTable("ovenTable", ovenContent);
		} catch (JsonGenerationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public class IngredientCount implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = -4827497775346780930L;
		private int amount;
		private String name;

		public int getAmount() {
			return amount;
		}

		public void setAmount(int amount) {
			this.amount = amount;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void incAmount() {
			this.amount++;
		}
	}

	public void onIngredientChanged(List<Ingredient> list) {
		// TODO Auto-generated method stub
		HashMap<Ingredient.Type, IngredientCount> countMap = new HashMap<Ingredient.Type, DataListener.IngredientCount>();
		for (Ingredient item : list) {
			if (countMap.containsKey(item.getType())) {
				countMap.get(item.getType()).incAmount();
			} else {
				IngredientCount tmp = new IngredientCount();
				tmp.setAmount(1);
				if (item.getType().equals(Ingredient.Type.HONEY)) {
					tmp.setName("Honig");
				} else if (item.getType().equals(Ingredient.Type.FLOUR)) {
					tmp.setName("Mehl");
				} else if (item.getType().equals(Ingredient.Type.EGG)) {
					tmp.setName("Eier");
				}
				countMap.put(item.getType(), tmp);
			}
		}

		if (!countMap.containsKey(Ingredient.Type.HONEY)) {
			IngredientCount tmp = new IngredientCount();
			tmp.setName("Honig");
			tmp.setAmount(0);
			countMap.put(Ingredient.Type.HONEY, tmp);
		}
		if (!countMap.containsKey(Ingredient.Type.FLOUR)) {
			IngredientCount tmp = new IngredientCount();
			tmp.setName("Mehl");
			tmp.setAmount(0);
			countMap.put(Ingredient.Type.FLOUR, tmp);
		}
		if (!countMap.containsKey(Ingredient.Type.EGG)) {
			IngredientCount tmp = new IngredientCount();
			tmp.setName("Eier");
			tmp.setAmount(0);
			countMap.put(Ingredient.Type.EGG, tmp);
		}

		try {
			window.updateTable("ingredientsTable", new ArrayList<IngredientCount>(countMap.values()));
		} catch (JsonGenerationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void onGingerBreadStateChange(List<GingerBread> list) {
		try {
			window.updateTable("gingerBreadTable", list);
		} catch (JsonGenerationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
