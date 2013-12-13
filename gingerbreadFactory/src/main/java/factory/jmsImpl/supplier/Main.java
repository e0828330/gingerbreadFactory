package factory.jmsImpl.supplier;

import java.io.IOException;

import javax.jms.JMSException;
import javax.naming.NamingException;

import factory.entities.Ingredient;
import factory.interfaces.Supplier;

public class Main {

	public static void main(String[] args) {
		Thread supplier1;
		Thread supplier2;
		Thread supplier3;

			Supplier jmsSupplier1 = new JMSSupplierInstance();
			jmsSupplier1.setId(1L);
			jmsSupplier1.placeOrder(7, Ingredient.Type.HONEY);
			supplier1 = new Thread(jmsSupplier1);
			supplier1.start();
			
			
			Supplier jmsSupplier2 = new JMSSupplierInstance();
			jmsSupplier2.setId(2L);
			jmsSupplier2.placeOrder(14, Ingredient.Type.EGG);
			supplier2 = new Thread(jmsSupplier2);
			supplier2.start();
			
			
			Supplier jmsSupplier3 = new JMSSupplierInstance();
			jmsSupplier3.setId(3L);
			jmsSupplier3.placeOrder(7, Ingredient.Type.FLOUR);
			supplier3 = new Thread(jmsSupplier3);
			supplier3.start();			
		
	}

}
