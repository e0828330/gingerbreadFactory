package factory.jmsImpl.server;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map.Entry;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;

import org.apache.qpid.transport.util.Logger;

import factory.entities.Ingredient;
import factory.utils.Messages;

public class JMSServerIngredientsDeliveryListener implements MessageListener {

	private Logger logger = Logger.get(getClass());

	private JMSServerInstance server;

	public JMSServerIngredientsDeliveryListener(JMSServerInstance jmsServerInstance) {
		this.server = jmsServerInstance;
	}

	public void onMessage(Message message) {
		this.logger.info("Message received in ingredients queue.", (Object[]) null);
		try {
			if (message instanceof ObjectMessage) {
				ObjectMessage objectMessage = (ObjectMessage) message;

				if (objectMessage.getStringProperty("TYPE").equalsIgnoreCase("ArrayList<Ingredient>")) {
					@SuppressWarnings("unchecked")
					ArrayList<Ingredient> ingredients = (ArrayList<Ingredient>) objectMessage.getObject();
					for (Ingredient ingredient : ingredients) {
						this.server.storeIncredient(ingredient);
					}
					Hashtable<String, String> properties = new Hashtable<String, String>(2);
					properties.put("EVENT", Messages.EVENT_NEW_INGREDIENTS);
					properties.put("TYPE", "ArrayList<Ingredient>");
					ArrayList<Ingredient> result = new ArrayList<Ingredient>();
					for (Entry<Long, Ingredient> tmp : this.server.get_total_ingredients_list().entrySet()) {
						result.add(tmp.getValue());
					}
					this.server.sendEventToGUI(result, properties);
				}

				TextMessage response = this.server.getIngredientsDelivery_session().createTextMessage();
				response.setJMSCorrelationID(message.getJMSCorrelationID());
				response.setText("Thanks!");
				MessageProducer producer = this.server.getIngredientsDelivery_session().createProducer(message.getJMSReplyTo());
				producer.send(response);
				producer.close();
			}
		} catch (JMSException e) {
			e.printStackTrace();
		} catch (Exception e) {
			this.logger.error("Error occured by parsing message from ingredients queue.", (Object[]) null);
			e.printStackTrace();
		}
	}
}
