package factory.jmsImpl.server;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.qpid.transport.util.Logger;

import factory.entities.Order;

public class JMSServerOrderQueueListener implements MessageListener {

	private JMSServerInstance server;
	
	private Logger logger = Logger.get(getClass());
	
	public JMSServerOrderQueueListener(JMSServerInstance server) {
		this.server = server;
	}
	
	public void onMessage(Message message) {
		this.logger.info("Message received in order queue.", (Object[]) null);
		try {
			message.acknowledge();
			if (message instanceof ObjectMessage) {
				ObjectMessage objectMessage = (ObjectMessage) message;
				if (objectMessage.getStringProperty("TYPE").equalsIgnoreCase("Order")) {
					Order order = (Order) objectMessage.getObject();
					this.server.storeOrder(order);
					this.logger.info("Stored order with ID =  " + order.getId(), (Object[]) null);
				}
			}
		} catch ( JMSException e) {
			e.printStackTrace();
		}
		
	}

}
