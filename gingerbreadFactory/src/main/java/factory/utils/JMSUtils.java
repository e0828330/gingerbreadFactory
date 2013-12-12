package factory.utils;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.UUID;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;

public class JMSUtils {

	public enum MessageType {
		OBJECTMESSAGE, TEXTMESSAGE
	}

	public static Message sendMessage(MessageType messageType, Object payLoad, Hashtable<String, String> stringProperties, QueueSession session, boolean hasReplyQueue, QueueSender sender)
			throws JMSException {

		if (messageType == MessageType.TEXTMESSAGE) {
			if (hasReplyQueue) {
				Destination tempDest = session.createTemporaryQueue();
				MessageConsumer consumer = session.createConsumer(tempDest);

				TextMessage message = session.createTextMessage((String) payLoad);
				if (stringProperties != null) {
					for (Entry<String, String> entry : stringProperties.entrySet()) {
						message.setStringProperty(entry.getKey(), entry.getValue());
					}
				}

				message.setJMSReplyTo(tempDest);
				message.setJMSCorrelationID(String.valueOf(UUID.randomUUID().hashCode()));
				sender.send(message);
				Message response = consumer.receive();
				consumer.close();
				return response;
			}
			else {
				TextMessage message = session.createTextMessage();
				message.setText((String) payLoad);
				if (stringProperties != null) {
					for (Entry<String, String> entry : stringProperties.entrySet()) {
						message.setStringProperty(entry.getKey(), entry.getValue());
					}
				}
				sender.send(message);
			}
		}
		else if (messageType == MessageType.OBJECTMESSAGE) {
			if (hasReplyQueue) {
				Destination tempDest = session.createTemporaryQueue();
				MessageConsumer consumer = session.createConsumer(tempDest);

				ObjectMessage message = session.createObjectMessage();
				message.setObject((Serializable) payLoad);
				if (stringProperties != null) {
					for (Entry<String, String> entry : stringProperties.entrySet()) {
						message.setStringProperty(entry.getKey(), entry.getValue());
					}
				}

				message.setJMSReplyTo(tempDest);
				message.setJMSCorrelationID(String.valueOf(UUID.randomUUID().hashCode()));
				sender.send(message);
				Message response = consumer.receive();
				consumer.close();
				return response;
			}
			else {
				ObjectMessage message = session.createObjectMessage();
				message.setObject((Serializable) payLoad);
				if (stringProperties != null) {
					for (Entry<String, String> entry : stringProperties.entrySet()) {
						message.setStringProperty(entry.getKey(), entry.getValue());
					}
				}
				sender.send(message);
			}
		}
		
		return null;

	}

}
