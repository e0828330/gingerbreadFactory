package factory.jmsImpl.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
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
	private QueueReceiver ingredientsDelivery_receiver;
	
	// baker queue
	private QueueConnection bakerIngredients_connection;
	private QueueSession bakerIngredients_session;
	private Queue bakerIngredients_queue;
	private QueueReceiver bakerIngredients_receiver;
	
	private MessageProducer bakerIngredients_replyProducer;

	// ingredient topic
	private Topic ingredientsTopic_topic;
	private TopicConnection ingredientsTopic_connection;
	private TopicSession ingredientsTopic_session;
	private TopicPublisher ingredientsTopic_publisher;
	
	private ArrayList<Ingredient> honey_list;
	private ArrayList<Ingredient> flour_list;
	private ArrayList<Ingredient> egg_list;
	
	private int count_gingerBread_eggs = 0;
	private int count_gingerBread_honey = 0;
	private int count_gingerBread_flour = 0;
	
	private int gingerBreadCounter = 0;
	
	private JMSServerIngredientsDeliveryListener incredientsDelivery_listener;
	private JMSServerBakerIngredientsQueueListener bakerIngredientsQueue_listener;
	
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
		
		// Set queue connection for baker
		this.setup_bakerIngredientsQueue();
		
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
	
	private void setup_bakerIngredientsQueue() throws NamingException, JMSException {
		this.logger.info("Initializing queue for bakers ingredients requests...", (Object[]) null); 
		QueueConnectionFactory queueConnectionFactory = 
				  (QueueConnectionFactory) ctx.lookup("qpidConnectionfactory");
		this.bakerIngredients_queue = (Queue) ctx.lookup("bakerIngredientsQueue");
		this.bakerIngredients_connection = queueConnectionFactory.createQueueConnection();
		this.bakerIngredients_session = this.bakerIngredients_connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
		this.bakerIngredientsQueue_listener = new JMSServerBakerIngredientsQueueListener(this);
		this.bakerIngredients_receiver = this.bakerIngredients_session.createReceiver(this.bakerIngredients_queue);
		this.bakerIngredients_receiver.setMessageListener(this.bakerIngredientsQueue_listener);
		// reply message producer for ingredient requests of baker
		this.bakerIngredients_replyProducer = this.bakerIngredients_session.createProducer(null);
		this.bakerIngredients_replyProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		
		this.bakerIngredients_connection.start();	
		this.logger.info("Queue for baker created and connection started.", (Object[]) null); 		
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
		this.ingredientsDelivery_receiver.setMessageListener(this.incredientsDelivery_listener);
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
		this.logger.info("Closing ingredients queue.", (Object[]) null);
		this.ingredientsDelivery_receiver.close();
		this.ingredientsDelivery_session.close();
		this.ingredientsDelivery_connection.close();
		
		this.logger.info("Closing baker queue.", (Object[]) null);
		this.bakerIngredients_receiver.close();
		this.bakerIngredients_replyProducer.close();
		this.bakerIngredients_session.close();
		this.bakerIngredients_connection.close();		
		
		this.logger.info("Closing ingredients topic.", (Object[]) null);
		this.ingredientsTopic_publisher.close();
		this.ingredientsTopic_session.close();
		this.ingredientsTopic_connection.close();
		
		this.logger.info("ServerInstance shutting down.", (Object[]) null); 
	}
	
	public void shutDown() {
		this.isRunning = false;
	}
	
	
	public synchronized void storeIncredient(Ingredient ingredient) {
		this.logger.info("Stored " + ingredient.getType().toString(), (Object[]) null);
		if (ingredient.getType() == Ingredient.Type.FLOUR) {
			this.logger.info("Added flour to list.", (Object[]) null); 
			this.flour_list.add(ingredient);
			this.count_gingerBread_flour++;
		}
		else if (ingredient.getType() == Ingredient.Type.HONEY) {
			this.logger.info("Added honey to list.", (Object[]) null); 
			this.honey_list.add(ingredient);
			this.count_gingerBread_honey++;
		}
		else if (ingredient.getType() == Ingredient.Type.EGG) {
			this.logger.info("Added egg to list.", (Object[]) null); 
			this.egg_list.add(ingredient);
			this.count_gingerBread_eggs++;
		}
		// Publish new ingredient
		this.publishIngredients(ingredient);
	}
	
	
	private void publishIngredients(Ingredient ingredient) {
		if (this.count_gingerBread_eggs >= 2 && this.count_gingerBread_flour >= 1 && this.count_gingerBread_honey >= 1) {
			this.gingerBreadCounter++;
			this.count_gingerBread_eggs -= 2;
			this.count_gingerBread_flour -= 1;
			this.count_gingerBread_honey -= 1;
			try {
				TextMessage message = this.ingredientsDelivery_session.createTextMessage("INGREDIENTS_READY");
				this.ingredientsTopic_publisher.publish(message);
			}
			catch (JMSException e) {
				this.logger.error("Cannot publish new gingerbread.", (Object[]) null);
				e.printStackTrace();
			}			
			this.logger.info("Published new gingerbread. Count = " + this.gingerBreadCounter, (Object[]) null);		
		}
		this.logger.info("eggs = " + this.count_gingerBread_eggs + "\n"
					+ "flour = " + this.count_gingerBread_flour + "\n"
					+ "honey = " + this.count_gingerBread_honey + "\n", (Object[]) null);
	}
	
	public QueueSession getIngredientsDelivery_session() {
		return this.ingredientsDelivery_session;
	}
	
	public QueueSession getBakerIngredients_session() {
		return this.bakerIngredients_session;
	}
	
	public MessageProducer getBakerIngredientsSender() {
		return this.bakerIngredients_replyProducer;
	}

}
