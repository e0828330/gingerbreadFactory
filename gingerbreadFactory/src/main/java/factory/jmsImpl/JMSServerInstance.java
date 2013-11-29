package factory.jmsImpl;

import java.io.IOException;
import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.qpid.transport.util.Logger;

import factory.entities.Ingredient;

public class JMSServerInstance implements Runnable {

	private Context ctx;
	private boolean isRunning = true;
	private Logger logger = Logger.get(getClass());
	
	// ingredient queue
	private QueueConnection ingredientsDelivery_connection;
	private QueueSession ingredientsDelivery_session;
	private QueueReceiver ingredientsDelivery_receiver;
	private Queue ingredientsDelivery_queue;
	
	
	private JMSServerIngredientsDeliveryListener incredientsDelivery_listener;
	
	public JMSServerInstance(String propertiesFile) throws IOException, NamingException, JMSException {
		Properties properties = new Properties();
		properties.load(this.getClass().getClassLoader().getResourceAsStream(propertiesFile));
		this.ctx = new InitialContext(properties);
		
		// Set queue connection for communication with server
		this.setup_ingredientsQueue();

		
	}
	
	private void setup_ingredientsQueue() throws IOException, NamingException, JMSException {
		this.logger.info("Initializing queue for ingredients...", (Object[]) null); 
		QueueConnectionFactory queueConnectionFactory = 
				  (QueueConnectionFactory) ctx.lookup("qpidConnectionfactory");
		
		this.ingredientsDelivery_queue = (Queue) ctx.lookup("ingredientsDelivery");
		
		this.ingredientsDelivery_connection = queueConnectionFactory.createQueueConnection();
		
		this.ingredientsDelivery_session = this.ingredientsDelivery_connection.createQueueSession(true, Session.AUTO_ACKNOWLEDGE);
		
		this.incredientsDelivery_listener = new JMSServerIngredientsDeliveryListener(this);
		
		this.ingredientsDelivery_receiver = this.ingredientsDelivery_session.createReceiver(this.ingredientsDelivery_queue);
		
		this.ingredientsDelivery_receiver.setMessageListener(incredientsDelivery_listener);
		
		this.ingredientsDelivery_connection.start();	
		this.logger.info("Queue for incredients created and connection started.", (Object[]) null); 
	}


	public void run() {
		while (isRunning) {
			System.out.println("SERVER RUNS");
		}
		try {
			this.close();
		}
		catch (JMSException e) {
			e.printStackTrace();
		}
	}
	
	private void close() throws JMSException {
		this.logger.info("Closing ingredients receiver.", (Object[]) null);
		this.ingredientsDelivery_receiver.close();
		this.logger.info("Closing ingredients session.", (Object[]) null);
		this.ingredientsDelivery_session.close();
		this.logger.info("Closing ingredients connection.", (Object[]) null);
		this.ingredientsDelivery_connection.close();
		this.logger.info("ServerInstance shutting down.", (Object[]) null); 
	}
	
	public void shutDown() {
		this.isRunning = false;
	}
	
	public void storeIncredient(Ingredient incredient) {
		
	}

}
