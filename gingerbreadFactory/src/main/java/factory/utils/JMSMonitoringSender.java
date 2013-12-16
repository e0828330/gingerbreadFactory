package factory.utils;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map.Entry;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.NamingException;

import factory.entities.GingerBread;

public class JMSMonitoringSender {

		private final String FACTORY = "qpidConnectionfactory";
		private final String MONITORING_QUEUE = "monitoringQueue";
	
		
		private QueueConnectionFactory queueConnectionFactory;
		private QueueConnection monitoring_connection;
		private QueueSession monitoring_session;
		private Queue monitoring_queue;
		private QueueSender monitoring_sender;	
		
		public JMSMonitoringSender(Context ctx) throws JMSException, NamingException {
				
		queueConnectionFactory = (QueueConnectionFactory) ctx.lookup(this.FACTORY);
		monitoring_queue = (Queue) ctx.lookup(this.MONITORING_QUEUE);
		monitoring_connection = queueConnectionFactory.createQueueConnection();
		monitoring_session = monitoring_connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
		monitoring_sender = monitoring_session.createSender(monitoring_queue);
		monitoring_connection.start();
		}
		
		
		public void closeConnection() throws JMSException {
			monitoring_sender.close();
			monitoring_session.close();
			monitoring_connection.close();
		}
		
		public void sendMonitoringMessage(GingerBread gingerBread) throws JMSException, NamingException {
			ObjectMessage message = monitoring_session.createObjectMessage();
			message.setStringProperty("TYPE", "GingerBread");
			message.setObject(gingerBread);
			monitoring_sender.send(message);
		}


		public void sendMonitoringMessage(ArrayList<GingerBread> charge, Hashtable<String, String> properties) throws JMSException {
			ObjectMessage message = monitoring_session.createObjectMessage();
			message.setStringProperty("TYPE", "ArrayList<GingerBread>");
			for (Entry<String, String> property : properties.entrySet()) {
				message.setStringProperty(property.getKey(), property.getValue());
			}
			message.setObject(charge);
			monitoring_sender.send(message);
		}
	
}
