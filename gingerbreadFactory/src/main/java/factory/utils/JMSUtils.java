package factory.utils;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;

import factory.spacesImpl.Server;
import factory.spacesImpl.SpaceUtils;

public class JMSUtils {

	public enum MessageType {
		OBJECTMESSAGE, TEXTMESSAGE
	}
	
	private static int factoryID;
	
	public static int getFactoryID() {
		return factoryID;
	}

	public static void setFactoryID(int factoryID) {
		JMSUtils.factoryID = factoryID;
	}	
	
	public static void sendReponse(MessageType messageType, Object payLoad, Hashtable<String, String> stringProperties, QueueSession session, String correlationID, Destination destination) throws JMSException {
		if (messageType == MessageType.TEXTMESSAGE) {
			TextMessage response = session.createTextMessage();
			response.setJMSCorrelationID(correlationID);
			response.setText((String) payLoad);
			if (stringProperties != null) {
				for (Entry<String, String> entry : stringProperties.entrySet()) {
					response.setStringProperty(entry.getKey(), entry.getValue());
				}
			}
			MessageProducer producer = session.createProducer(destination);
			producer.send(response);
			producer.close();
		}
		else if (messageType == MessageType.OBJECTMESSAGE) {
			ObjectMessage response = session.createObjectMessage();
			response.setJMSCorrelationID(correlationID);
			response.setObject((Serializable) payLoad);
			if (stringProperties != null) {
				for (Entry<String, String> entry : stringProperties.entrySet()) {
					response.setStringProperty(entry.getKey(), entry.getValue());
				}
			}
			MessageProducer producer = session.createProducer(destination);
			producer.send(response);
			producer.close();			
		}
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
			} else {
				TextMessage message = session.createTextMessage();
				message.setText((String) payLoad);
				if (stringProperties != null) {
					for (Entry<String, String> entry : stringProperties.entrySet()) {
						message.setStringProperty(entry.getKey(), entry.getValue());
					}
				}
				sender.send(message);
			}
		} else if (messageType == MessageType.OBJECTMESSAGE) {
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
			} else {
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
	
	/**
	 * Parses the factory id from the cmd line arguments
	 * 
	 * @param args
	 * @param idx
	 */
	public static int parseFactoryID(String[] args, int idx) {
		if (args.length < idx + 1) {
			System.err.println("Please supply a factory id!");
			System.exit(1);
		}
		int factoryId = 0;
		try {
			factoryId = Integer.parseInt(args[idx]);
		}
		catch (NumberFormatException e) {
			System.err.println("Please supply a valid factory id!");
			System.exit(1);
		}
		JMSUtils.factoryID = factoryId;
		return factoryId;
	}

	public static void extendJMSProperties(Properties properties, int factoryID) {
		String[] queues = new String[] {
				"ingredientsDelivery",
				"bakerIngredientsQueue",
				"ovenQueue",
				"qualityControlQueue",
				"logisticsQueue",
				"monitoringQueue",
				"commandQueue",
				"eventQueue",
				"bakerRequestQueue",
				"packagingQueue",
				"orderQueue",
		};
		
		for (String s : queues) {
			properties.put("queue." + s + String.valueOf(factoryID), "amq.queue." + s + String.valueOf(factoryID));
		}
	}	

}
