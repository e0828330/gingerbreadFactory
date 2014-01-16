package factory.jmsImpl.server;

import java.util.ArrayList;
import java.util.Map.Entry;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;

import factory.entities.ChargeReplyObject;
import factory.entities.GingerBread;
import factory.entities.Ingredient;
import factory.entities.Order;
import factory.utils.Messages;

public class JMSServerCommandListener implements MessageListener {

	private JMSServerInstance server;

	public JMSServerCommandListener(JMSServerInstance server) {
		this.server = server;
	}

	public void onMessage(Message message) {
		try {
			if (message instanceof TextMessage) {
				TextMessage textMessage = (TextMessage) message;

				ObjectMessage response = this.server.get_CommandSession().createObjectMessage();
				response.setJMSCorrelationID(message.getJMSCorrelationID());

				if (textMessage.getText() != null && textMessage.getText().equals(Messages.GET_INGREDIENTS)) {
					ArrayList<Ingredient> result = new ArrayList<Ingredient>();
					for (Entry<Long, Ingredient> tmp : this.server.get_total_ingredients_list().entrySet()) {
						result.add(tmp.getValue());
					}
					response.setObject(result);
					response.setStringProperty("TYPE", "ArrayList<Ingredient>");
				} else if (textMessage.getText() != null && textMessage.getText().equals(Messages.GET_GINGERBREADS)) {
					ArrayList<GingerBread> result = new ArrayList<GingerBread>();
					for (Entry<Long, GingerBread> tmp : this.server.getGingerBreads().entrySet()) {
						result.add(tmp.getValue());
					}
					response.setObject(result);
					response.setStringProperty("TYPE", "ArrayList<GingerBread>");
				} else if (textMessage.getText() != null && textMessage.getText().equals(Messages.GET_OVEN)) {
					ArrayList<GingerBread> result = new ArrayList<GingerBread>();

					for (ChargeReplyObject tmp : this.server.get_nextOvenCharges()) {
						for (GingerBread gingerBread : tmp.getCharge()) {
							result.add(gingerBread);
						}
					}
					response.setObject(result);
					response.setStringProperty("TYPE", "ArrayList<GingerBread>");
				} else if (textMessage.getText() != null && textMessage.getText().equals(Messages.GET_ORDERS)) {
					ArrayList<Order> result = new ArrayList<Order>(this.server.getOrder_list().values());
					response.setObject(result);
					response.setStringProperty("TYPE", "ArrayList<Order>");
				}
				MessageProducer producer = this.server.get_CommandSession().createProducer(message.getJMSReplyTo());
				producer.send(response);
				producer.close();
			}
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

}
