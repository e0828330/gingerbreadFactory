package factory.jmsImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.qpid.transport.util.Logger;

import factory.entities.Ingredient;

public class JMSServerInstance implements Runnable {

	private Context ctx;
	private boolean isRunning = true;
	private Logger logger = Logger.get(getClass());
	
	// ingredient queue
	private QueueConnection ingredientsDelivery_connection;
	private QueueSession ingredientsDelivery_session;
	private Queue ingredientsDelivery_queue;
	
	//private QueueSender ingredientsDelivery_sender;
	private QueueReceiver ingredientsDelivery_receiver;

	// ingredient topic
	private Topic ingredientsTopic_topic;
	private TopicConnection ingredientsTopic_connection;
	private TopicSession ingredientsTopic_session;
	private TopicPublisher ingredientsTopic_publisher;
	
	private ArrayList<Ingredient> honey_list;
	private ArrayList<Ingredient> flour_list;
	private ArrayList<Ingredient> egg_list;
	
	private JMSServerIngredientsDeliveryListener incredientsDelivery_listener;
	
	public JMSServerInstance(String propertiesFile) throws IOException, NamingException, JMSException {
		Properties properties = new Properties();
		properties.load(this.getClass().getClassLoader().getResourceAsStream(propertiesFile));
		this.ctx = new InitialContext(properties);
		
		// set ingredient storage
		this.honey_list = new ArrayList<Ingredient>();
		this.flour_list = new ArrayList<Ingredient>();
		this.egg_list = new ArrayList<Ingredient>();
		
		// Set queue connection for ingredients
		this.setup_ingredientsQueue();
		
		// Set topic for ingredients
		this.setup_ingredientsTopic();

		
	}
	
	private void setup_ingredientsTopic() throws NamingException, JMSException {
		this.logger.info("Initializing topic for ingredients...", (Object[]) null); 
		TopicConnectionFactory topicConnectionFactory = 
				  (TopicConnectionFactory) ctx.lookup("qpidConnectionfactory");
		
		this.ingredientsTopic_topic = (Topic) ctx.lookup("ingredientsTopic");
		
		this.ingredientsTopic_connection = topicConnectionFactory.createTopicConnection();
		
		this.ingredientsTopic_session = this.ingredientsTopic_connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
	
		this.ingredientsTopic_publisher = this.ingredientsTopic_session.createPublisher(this.ingredientsTopic_topic);
	
		
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
		
		//this.ingredientsDelivery_sender = this.ingredientsDelivery_session.createSender(ingredientsDelivery_queue);
		
		this.ingredientsDelivery_connection.start();	
		this.logger.info("Queue for incredients created and connection started.", (Object[]) null); 
	}


	public void run() {
		while (isRunning) {

			
		}
		try {
			this.close();
		}
		catch (JMSException e) {
			e.printStackTrace();
		}
	}
	
	private void close() throws JMSException {
		//this.logger.info("Closing ingredients sender for queue.", (Object[]) null);
		//this.ingredientsDelivery_sender.close();
		this.logger.info("Closing ingredients receiver for queue.", (Object[]) null);
		this.ingredientsDelivery_receiver.close();
		this.logger.info("Closing ingredients session for queue.", (Object[]) null);
		this.ingredientsDelivery_session.close();
		this.logger.info("Closing ingredients connection for queue.", (Object[]) null);
		this.ingredientsDelivery_connection.close();
		
		this.logger.info("Closing ingredients publisher for topic.", (Object[]) null);
		this.ingredientsTopic_publisher.close();
		this.logger.info("Closing ingredients session for topic.", (Object[]) null);
		this.ingredientsTopic_session.close();
		this.logger.info("Closing ingredients connection for topic.", (Object[]) null);
		this.ingredientsTopic_connection.close();
		
		this.logger.info("ServerInstance shutting down.", (Object[]) null); 
	}
	
	public void shutDown() {
		this.isRunning = false;
	}
	
	public void storeIncredient(Ingredient ingredient) {
		if (ingredient.getType() == Ingredient.Type.FLOUR) {
			this.logger.info("Added flour to list.", (Object[]) null); 
			this.flour_list.add(ingredient);
		}
		else if (ingredient.getType() == Ingredient.Type.HONEY) {
			this.logger.info("Added honey to list.", (Object[]) null); 
			this.honey_list.add(ingredient);
		}
		else if (ingredient.getType() == Ingredient.Type.EGG) {
			this.logger.info("Added egg to list.", (Object[]) null); 
			this.egg_list.add(ingredient);
		}
		// Publish new ingredient
		this.publishIngredient(ingredient);
	}
	
	private void publishIngredient(Ingredient ingredient) {
		try {
			ObjectMessage objectMessage = this.ingredientsDelivery_session.createObjectMessage();
			objectMessage.setObject(ingredient.getType());
			this.ingredientsTopic_publisher.publish(objectMessage);
		}
		catch (JMSException e) {
			this.logger.error("Cannot publish ingredient " + ingredient.getType().toString(), (Object[]) null);
			e.printStackTrace();
		}
		this.logger.info("Published " + ingredient.getType().toString(), (Object[]) null);
	}
	
	public QueueSession getIngredientsDelivery_session() {
		return this.ingredientsDelivery_session;
	}

}
