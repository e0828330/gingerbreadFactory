package factory.jmsImpl.server;

import java.util.ArrayList;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;

import org.apache.qpid.transport.util.Logger;

import factory.entities.GingerBreadTransactionObject;
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
				Long bakerID = ((TextMessage) message).getLongProperty("BAKER_ID");
				if (txt != null && txt.equals(Messages.INGREDIENTS_REQUEST_MESSAGE)) {
					
					ArrayList<GingerBreadTransactionObject> ingredients = this.server.getGingerBreadIngredients(5);
					
					if (ingredients == null || (ingredients != null && ingredients.size() == 0)) {
						TextMessage response = this.server.getBakerIngredients_session().createTextMessage();
						response.setJMSCorrelationID(message.getJMSCorrelationID());
						response.setText(Messages.INGREDIENTS_RESPONSE_MESSAGE_NONE);
						this.server.getBakerIngredientsSender().send(message.getJMSReplyTo(), response);
						this.server.getBakerIngredients_session().commit();
					}
					else {
						ObjectMessage response = this.server.getBakerIngredients_session().createObjectMessage();
						response.setJMSCorrelationID(message.getJMSCorrelationID());
						response.setObject(ingredients);
						response.setStringProperty("TYPE", "ArrayList<GingerBreadTransactionObject>");
						this.server.getBakerIngredientsSender().send(message.getJMSReplyTo(), response);

						if (this.server.getGingerBreadCounter() > 0) {
							TextMessage responseMore = this.server.getBakerIngredients_session().createTextMessage();
							responseMore.setJMSCorrelationID(message.getJMSCorrelationID());
							responseMore.setText(Messages.MESSAGE_MORE_INGREDIENTS_AVAILABLE);
							this.server.getBakerIngredientsSender().send(message.getJMSReplyTo(), responseMore);	
						}
						
						if (bakerID != null) {
							this.server.getDelivered_ingredients().put(bakerID, ingredients);
						}
						
						this.server.getBakerIngredients_session().commit();
					}
					
				}
			}
		}
		catch (JMSException e) {
			e.printStackTrace();
		}
	}
}
