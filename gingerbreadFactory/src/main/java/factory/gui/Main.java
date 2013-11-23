package factory.gui;

import factory.entities.Ingredient;
import factory.interfaces.Supplier;
import factory.spacesImpl.SupplierImpl;

public class Main {

	public static void main(String[] args) {
		// TODO: Implement GUI, dummy for now
		Supplier supplier = new SupplierImpl();
		supplier.setId(1L);
		supplier.placeOrder(5, Ingredient.Type.FLOUR);
		new Thread(supplier).start();
		
		Supplier supplier2 = new SupplierImpl();
		supplier2.setId(2L);
		supplier2.placeOrder(4, Ingredient.Type.EGG);
		new Thread(supplier2).start();
		
		
		Supplier supplier3 = new SupplierImpl();
		supplier3.setId(3L);
		supplier3.placeOrder(4, Ingredient.Type.HONEY);
		new Thread(supplier3).start();
		
		
	}

}
