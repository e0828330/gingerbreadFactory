package factory.jmsImpl;

import java.io.IOException;

import javax.jms.JMSException;
import javax.naming.NamingException;

import factory.entities.Ingredient;
import factory.interfaces.Supplier;
import factory.utils.Utils;

public class Main {

	public static void main(String[] args) {
		Thread supplier1;
		Thread supplier2;
		Thread supplier3;
		try {
			Supplier jmsSupplier1 = new JMSSupplierImpl("jms.properties");
			jmsSupplier1.setId(1L);
			jmsSupplier1.placeOrder(2, Ingredient.Type.HONEY);
			supplier1 = new Thread(jmsSupplier1);
			supplier1.start();
			
			
			Supplier jmsSupplier2 = new JMSSupplierImpl("jms.properties");
			jmsSupplier2.setId(2L);
			jmsSupplier2.placeOrder(3, Ingredient.Type.EGG);
			supplier2 = new Thread(jmsSupplier2);
			supplier2.start();
			
			
			Supplier jmsSupplier3 = new JMSSupplierImpl("jms.properties");
			jmsSupplier3.setId(3L);
			jmsSupplier3.placeOrder(3, Ingredient.Type.FLOUR);
			supplier3 = new Thread(jmsSupplier3);
			supplier3.start();			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
