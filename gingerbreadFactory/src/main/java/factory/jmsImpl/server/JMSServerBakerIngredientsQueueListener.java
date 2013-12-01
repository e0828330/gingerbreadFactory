package factory.jmsImpl.server;

import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.apache.qpid.transport.util.Logger;

import factory.entities.GingerBread;
import factory.utils.Messages;

public class JMSServerBakerIngredientsQueueListener implements MessageListener {
	
	private JMSServerInstance server;
	private Logger logger = Logger.get(getClass());	
	
	public JMSServerBakerIngredientsQueueListener(JMSServerInstance server) {
		this.server = server;
	}
	
	public void onMessage(Message message) {
		this.logger.info("Received message from baker", (Object[]) null);
		try {
			if (message instanceof TextMessage) {
				String txt = ((TextMessage) message).getText();
				if (txt != null && txt.equals(Messages.INGREDIENTS_REQUEST_MESSAGE)) {
					TextMessage response = this.server.getBakerIngredients_session().createTextMessage();
					response.setJMSCorrelationID(message.getJMSCorrelationID());
					
					List<GingerBread> ingredients = this.server.getGingerBreadIngredients(5);
					
					
					response.setText("SEND Gingerbreads: " + ingredients.size());
					this.logger.info("Send response for ingredients request to baker", (Object[]) null);
					this.server.getBakerIngredientsSender().send(message.getJMSReplyTo(), response);
				}
			}
		}
		catch (JMSException e) {
			e.printStackTrace();
		}
	}
}
