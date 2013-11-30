package factory.jmsImpl;

import javax.jms.Message;
import javax.jms.MessageListener;

import factory.interfaces.Supplier;

public class JMSSupplierInstanceDeliveryListener implements MessageListener {

	private Supplier supplier;
	
	public JMSSupplierInstanceDeliveryListener(Supplier supplier) {
		this.supplier = supplier;
	}
	
	public void onMessage(Message arg0) {
		// TODO Auto-generated method stub
		
	}

}
