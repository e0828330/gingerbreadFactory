package factory.jmsImpl.server;

import java.util.Hashtable;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;

import org.apache.qpid.transport.util.Logger;

import factory.entities.Order;
import factory.utils.JMSUtils;
import factory.utils.JMSUtils.MessageType;
import factory.utils.Messages;

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
			else if (message instanceof TextMessage) {
				TextMessage textMessage = (TextMessage) message;
				if (textMessage.getText() != null && textMessage.getText().equals(Messages.LOAD_REQUEST)) {
					Long orderID = 0L;
					if (message.getStringProperty("ORDER_ID") != null) {
						orderID = Long.valueOf(message.getStringProperty("ORDER_ID"));
					}
					int numberOfTotalOpenPackages = 0;
					for (Order o : this.server.getOpenOrders()) {
						if (o.getDonePackages() == null) {
							numberOfTotalOpenPackages += o.getPackages();
						}
						else {
							numberOfTotalOpenPackages += o.getPackages() - o.getDonePackages();
						}
					}
					
					Hashtable<String, String> properties = new Hashtable<String, String>(3);
					properties.put("ORDER_ID", String.valueOf(orderID));
					properties.put("LOAD", String.valueOf(numberOfTotalOpenPackages));
					properties.put("FACTORY_ID", String.valueOf(this.server.getFactoryID()));
					JMSUtils.sendMessage(MessageType.TEXTMESSAGE, 
							Messages.LOAD, 
							properties, 
							this.server.getLb_session(), 
							false, 
							this.server.getLb_sender());
				}
			}
		} catch ( JMSException e) {
			e.printStackTrace();
		}
		
	}

}
