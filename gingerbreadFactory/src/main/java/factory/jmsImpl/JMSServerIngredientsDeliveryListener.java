package factory.jmsImpl;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.qpid.transport.util.Logger;

import factory.entities.Ingredient;

public class JMSServerIngredientsDeliveryListener implements MessageListener {

	private Logger logger = Logger.get(getClass());
	
	private JMSServerInstance server;
	
	public JMSServerIngredientsDeliveryListener(
			JMSServerInstance jmsServerInstance) {
		this.server = jmsServerInstance;
	}

	public void onMessage(Message message) {
		this.logger.info("Message received in ingredients queue.", (Object[]) null);
		
		ObjectMessage objectMessage = (ObjectMessage) message;
		
		try {
			if (objectMessage.getObject() instanceof Ingredient) {
				this.server.storeIncredient((Ingredient) objectMessage.getObject());
			}
			this.server.getIngredientsDelivery_session().commit();		
		} catch (JMSException e) {
			e.printStackTrace();
		} catch (Exception e) {
			this.logger.error("Error occured by parsing message from ingredients queue.", (Object[]) null);
			e.printStackTrace();
		}
		
		
	}
}
