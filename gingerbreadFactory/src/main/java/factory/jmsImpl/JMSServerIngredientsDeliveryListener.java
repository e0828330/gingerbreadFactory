package factory.jmsImpl;

import javax.jms.Message;
import javax.jms.MessageListener;

public class JMSServerIngredientsDeliveryListener implements MessageListener {

	private JMSServerInstance server;
	
	public JMSServerIngredientsDeliveryListener(
			JMSServerInstance jmsServerInstance) {
		this.server = jmsServerInstance;
	}

	public void onMessage(Message arg0) {
		
	}

}
