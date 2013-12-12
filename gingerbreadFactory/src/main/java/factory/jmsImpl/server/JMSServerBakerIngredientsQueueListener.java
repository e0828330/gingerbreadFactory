package factory.jmsImpl.server;

import java.util.ArrayList;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;

import org.apache.qpid.transport.util.Logger;

import factory.entities.BakerWaitingObject;
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
						this.logger.info("Enqueue baker into waiting list because no ingredients are available.", (Object[]) null);
						this.server.getBakerWaitingList().add(new BakerWaitingObject(message.getJMSCorrelationID(), message.getJMSReplyTo()));

					} else {
						ObjectMessage response = this.server.getBakerIngredients_session().createObjectMessage();
						response.setJMSCorrelationID(message.getJMSCorrelationID());
						response.setObject(ingredients);
						response.setStringProperty("TYPE", "ArrayList<GingerBreadTransactionObject>");

						if (bakerID != null) {
							this.server.getDelivered_ingredients().put(bakerID, ingredients);
						}
						this.server.getBakerIngredientsProducer().send(message.getJMSReplyTo(), response);
					}

				}
			}
			message.acknowledge();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}
}
