package factory.jmsImpl.server;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map.Entry;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.qpid.transport.util.Logger;

import factory.entities.ChargeReplyObject;
import factory.entities.GingerBread;
import factory.entities.Ingredient;
import factory.utils.Messages;

public class JMSServerOvenQueueListener implements MessageListener {

	private JMSServerInstance server;
	private Logger logger = Logger.get(getClass());

	public JMSServerOvenQueueListener(JMSServerInstance server) {
		this.server = server;
	}

	public void onMessage(Message message) {
		this.logger.info("Received message from baker for oven", (Object[]) null);
		try {
			if (message instanceof ObjectMessage) {
				ObjectMessage objMessage = (ObjectMessage) message;
				if (objMessage.getStringProperty("TYPE").equals("ArrayList<GingerBread>")) {
					@SuppressWarnings("unchecked")
					ArrayList<GingerBread> charge = (ArrayList<GingerBread>) objMessage.getObject();
				
					for (GingerBread tmp : charge) {
						this.server.get_total_ingredients_list().remove(tmp.getFirstEggId());
						this.server.get_total_ingredients_list().remove(tmp.getSecondEggId());
						this.server.get_total_ingredients_list().remove(tmp.getHoneyId());
						this.server.get_total_ingredients_list().remove(tmp.getFlourId());
					}
					
					ChargeReplyObject replyObject = new ChargeReplyObject(charge, message.getJMSCorrelationID(), message.getJMSReplyTo());

					
					
					if (message.getStringProperty("BAKER_ID") != null) {
						Long bakerID = new Long(message.getStringProperty("BAKER_ID"));
						replyObject.setBakerID(bakerID);
						if (bakerID != null) {
							if (this.server.getDelivered_ingredients().containsKey(bakerID)) {
								this.server.getDelivered_ingredients().remove(bakerID);
							}
						}
					}
					
					// send to oven
					this.server.addToOven(replyObject);
					
					Hashtable<String, String> properties = new Hashtable<String, String>(2);
					properties.put("TYPE", "ArrayList<Ingredient>");
					properties.put("EVENT", Messages.EVENT_NEW_INGREDIENTS);
					
					ArrayList<Ingredient> result = new ArrayList<Ingredient>();
					for (Entry<Long, Ingredient> tmp : this.server.get_total_ingredients_list().entrySet()){
						result.add(tmp.getValue());
					}
					
					this.server.sendEventToGUI(result, properties);
				}
			}
			message.acknowledge();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}
}
