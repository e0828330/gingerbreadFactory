package factory.jmsImpl.server;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.apache.qpid.transport.util.Logger;

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
				if (txt != null && txt.equals("INGREDIENTS_REQUEST")) {
					System.err.println("TODO: Send ingredients");
					TextMessage response = this.server.getBakerIngredients_session().createTextMessage();
					response.setJMSCorrelationID(message.getJMSCorrelationID());
					// TODO
					response.setText("TODO: SEND INGREDIENT DATA");
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
