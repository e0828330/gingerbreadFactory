package factory.jmsImpl.server;

import java.util.ArrayList;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.qpid.transport.util.Logger;

import factory.entities.ChargeReplyObject;
import factory.entities.GingerBread;

public class JMSServerOvenQueueListener implements MessageListener {
	
	private JMSServerInstance server;
	private Logger logger = Logger.get(getClass());	
	
	public JMSServerOvenQueueListener(JMSServerInstance server) {
		this.server = server;
	}
	
	public void onMessage(Message message) {
		this.logger.info("Received message from baker for oven", (Object[]) null);
		if (message instanceof ObjectMessage) {
			ObjectMessage objMessage = (ObjectMessage) message;
			try {
				if (objMessage.getStringProperty("TYPE").equals("ArrayList<GingerBread>")) {
					System.out.println("Received charge for oven...");
					@SuppressWarnings("unchecked")
					ArrayList<GingerBread> charge = (ArrayList<GingerBread>) objMessage.getObject();
					
					ChargeReplyObject replyObject = new ChargeReplyObject(charge, message.getJMSCorrelationID(), message.getJMSReplyTo());
					
					
					this.server.addToOven(replyObject);
					this.server.getOven_session().commit();
	
				}
			}
			catch (JMSException e) {
				e.printStackTrace();
			}
		}
	}
}
