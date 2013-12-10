package factory.jmsImpl.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
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
import org.mozartspaces.core.Entry;

import factory.entities.ChargeReplyObject;
import factory.entities.GingerBread;
import factory.entities.GingerBreadTransactionObject;
import factory.entities.Ingredient;
import factory.utils.Messages;
import factory.utils.Utils;

public class JMSServerInstance implements Runnable {

	private Context ctx;
	private boolean isRunning = true;
	private Logger logger = Logger.get(getClass());
	
	// Stores the bakerid -> ingredients relationship
	private ConcurrentHashMap<Long, ArrayList<GingerBreadTransactionObject>> delivered_ingredients;
	
	// Stores the waiting charges for the oven
	private List<ChargeReplyObject> waitingList;
	
	// Oven
	private AtomicBoolean ovenIsRunning;
	private final int MAX_OVEN_CHARGE = 10;
	
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
	
	// oven queue
	private QueueConnection ovenQueue_connection;
	private QueueSession ovenQueue_session;
	private Queue ovenQueue_queue;
	private QueueReceiver ovenQueue_receiver;
	
	
	private MessageProducer bakerOven_replyProducer;
	private MessageProducer bakerIngredients_replyProducer;

	// ingredient topic
	private Topic ingredientsTopic_topic;
	private TopicConnection ingredientsTopic_connection;
	private TopicSession ingredientsTopic_session;
	private TopicPublisher ingredientsTopic_publisher;
	
	private List<Ingredient> honey_list;
	private List<Ingredient> flour_list;
	private List<Ingredient> egg_list;
	
	private AtomicInteger count_gingerBread_eggs;
	private AtomicInteger count_gingerBread_honey;
	private AtomicInteger count_gingerBread_flour;
	
	private AtomicInteger gingerBreadCounter;
	
	private JMSServerIngredientsDeliveryListener incredientsDelivery_listener;
	private JMSServerBakerIngredientsQueueListener bakerIngredientsQueue_listener;
	private JMSServerOvenQueueListener ovenQueue_listener;
	
	public JMSServerInstance(String propertiesFile) throws IOException, NamingException, JMSException {
		Properties properties = new Properties();
		properties.load(this.getClass().getClassLoader().getResourceAsStream(propertiesFile));
		this.ctx = new InitialContext(properties);
		
		// set ingredient storage
		this.honey_list = Collections.synchronizedList(new ArrayList<Ingredient>());
		this.flour_list = Collections.synchronizedList(new ArrayList<Ingredient>());
		this.egg_list = Collections.synchronizedList(new ArrayList<Ingredient>());
		
		this.waitingList = Collections.synchronizedList(new ArrayList<ChargeReplyObject>());
		
		this.delivered_ingredients = new ConcurrentHashMap<Long, ArrayList<GingerBreadTransactionObject>>();
		
		this.count_gingerBread_eggs = new AtomicInteger(0);
		this.count_gingerBread_flour = new AtomicInteger(0);
		this.count_gingerBread_honey = new AtomicInteger(0);
		this.gingerBreadCounter = new AtomicInteger(0);
		
		// Init oven
		this.ovenIsRunning = new AtomicBoolean(false);
		
		// Set queue connection for ingredients
		this.setup_ingredientsQueue();
		
		// Set queue connection for baker
		this.setup_bakerIngredientsQueue();
		
		// Set queue connection for oven
		this.setup_ovenQueue();
		
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
		this.bakerIngredients_session = this.bakerIngredients_connection.createQueueSession(true, Session.AUTO_ACKNOWLEDGE);
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
	
	private void setup_ovenQueue() throws NamingException, JMSException {
		this.logger.info("Initializing queue for oven...", (Object[]) null); 
		QueueConnectionFactory queueConnectionFactory = 
				  (QueueConnectionFactory) ctx.lookup("qpidConnectionfactory");
		
		this.ovenQueue_queue = (Queue) ctx.lookup("ovenQueue");
		this.ovenQueue_connection = queueConnectionFactory.createQueueConnection();
		this.ovenQueue_session = this.ovenQueue_connection.createQueueSession(true, Session.AUTO_ACKNOWLEDGE);
		this.ovenQueue_receiver = this.ovenQueue_session.createReceiver(this.ovenQueue_queue);
		this.ovenQueue_listener = new JMSServerOvenQueueListener(this);
		this.ovenQueue_receiver.setMessageListener(this.ovenQueue_listener);
		
		this.bakerOven_replyProducer = this.ovenQueue_session.createProducer(null);
		this.bakerOven_replyProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		
		this.ovenQueue_connection.start();
		this.logger.info("Queue for oven created and connection started.", (Object[]) null); 
	}


	public void run() {
		System.out.println("\n======================================");
		System.out.println("Type 'storage' to see the stored ingredients");
		System.out.println("Type 'oven_state' to see the state of the oven");
		System.out.println("Type 'exit' to to shut down the server");
		System.out.println("======================================\n");
		while (isRunning) {
			try {
				BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
				String s = bufferRead.readLine();
				if (s.equalsIgnoreCase("storage")) {
					this.printStorage();
				}
				else if (s.equalsIgnoreCase("oven_state")) {
					if (this.ovenIsRunning.get() == true) {
						System.out.println("Oven is running.");
					}
					else {
						System.out.println("Oven is ready.");
					}
				}
				else if (s.equals("exit")) {
					break;
				}
			}
			catch (IOException e) {
				e.printStackTrace();
				try {
					this.close();
				} catch (JMSException e1) {
					e1.printStackTrace();
				}
			}
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
		
		this.logger.info("Closing oven queue.", (Object[]) null);
		this.ovenQueue_receiver.close();
		this.bakerOven_replyProducer.close();
		this.ovenQueue_session.close();
		this.ovenQueue_connection.close();		
		
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
			this.count_gingerBread_flour.incrementAndGet();
		}
		else if (ingredient.getType() == Ingredient.Type.HONEY) {
			this.logger.info("Added honey to list.", (Object[]) null); 
			this.honey_list.add(ingredient);
			this.count_gingerBread_honey.incrementAndGet();
		}
		else if (ingredient.getType() == Ingredient.Type.EGG) {
			this.logger.info("Added egg to list.", (Object[]) null); 
			this.egg_list.add(ingredient);
			this.count_gingerBread_eggs.incrementAndGet();
		}
		// Publish new ingredient
		this.publishIngredients(ingredient);
	}
	
	
	private void publishIngredients(Ingredient ingredient) {
		if (this.count_gingerBread_eggs.get() >= 2 && this.count_gingerBread_flour.get() >= 1 && this.count_gingerBread_honey.get() >= 1) {
			this.gingerBreadCounter.incrementAndGet();
			this.count_gingerBread_eggs.decrementAndGet();
			this.count_gingerBread_eggs.decrementAndGet();
			this.count_gingerBread_flour.decrementAndGet();
			this.count_gingerBread_honey.decrementAndGet();
			try {
				TextMessage message = this.ingredientsDelivery_session.createTextMessage(Messages.INGREDIENTS_READY_MESSAGE);
				this.ingredientsTopic_publisher.publish(message);
			}
			catch (JMSException e) {
				this.logger.error("Cannot publish new gingerbread.", (Object[]) null);
				e.printStackTrace();
			}			
			this.logger.info("Published new gingerbread. Count = " + this.gingerBreadCounter, (Object[]) null);		
		}
		debugMessageStoredIngredients();
	}
	
	public QueueSession getIngredientsDelivery_session() {
		return this.ingredientsDelivery_session;
	}
	
	public QueueSession getBakerIngredients_session() {
		return this.bakerIngredients_session;
	}
	
	public QueueSession getOven_session() {
		return this.ovenQueue_session;
	}
	
	public MessageProducer getBakerIngredientsSender() {
		return this.bakerIngredients_replyProducer;
	}
	
	public synchronized ArrayList<GingerBreadTransactionObject> getGingerBreadIngredients(int max) {
		int i = 0;
		ArrayList<GingerBreadTransactionObject> tmpList = new ArrayList<GingerBreadTransactionObject>(max);
		int limit = this.gingerBreadCounter.get();
		try {
			for (; i < limit; i++) {
				if (i >= max) break;
				Ingredient egg1 = this.egg_list.remove(0);
				Ingredient egg2 = this.egg_list.remove(0);
				Ingredient flour = this.flour_list.remove(0);
				Ingredient honey = this.honey_list.remove(0);
			
				tmpList.add(new GingerBreadTransactionObject(egg1, egg2, flour, honey));
				
				this.gingerBreadCounter.decrementAndGet();
			}
			debugMessageStoredIngredients();
			return tmpList;
		}
		catch (Exception e) {
			e.printStackTrace();
			// rollback
			for (GingerBreadTransactionObject gingerBread : tmpList) {
				
				this.count_gingerBread_eggs.incrementAndGet();
				this.count_gingerBread_eggs.incrementAndGet();
				this.egg_list.add(gingerBread.getEgg1());
				this.egg_list.add(gingerBread.getEgg2());
			
				this.count_gingerBread_flour.incrementAndGet();
				this.flour_list.add(gingerBread.getFlour());
				
				this.count_gingerBread_honey.incrementAndGet();
				this.honey_list.add(gingerBread.getHoney());
				
				this.gingerBreadCounter.incrementAndGet();
			}
		}
		return null;
	}
	
	public int getGingerBreadCounter() {
		return this.gingerBreadCounter.get();
	}
	
	private void debugMessageStoredIngredients() {
		this.logger.info("\novercharged eggs = " + this.count_gingerBread_eggs + "\n"
				+ "overcharged flour = " + this.count_gingerBread_flour + "\n"
				+ "overcharged honey = " + this.count_gingerBread_honey + "\n"
				+ "gingerbreads possible = " + this.gingerBreadCounter.get() + "\n", (Object[]) null);
	}
	
	private void printStorage() {
		System.out.println("\neggs = " + this.egg_list.size() + "\n"
				+ "flour = " + this.flour_list.size() + "\n"
				+ "honey = " + this.honey_list.size() + "\n"
				+ "gingerbreads possible = " + this.gingerBreadCounter.get() + "\n");
	}

	public ConcurrentHashMap<Long, ArrayList<GingerBreadTransactionObject>> getDelivered_ingredients() {
		return delivered_ingredients;
	}
	
	public synchronized void addToOven(ChargeReplyObject replyObject) {
		this.waitingList.add(replyObject);
		if (this.ovenIsRunning.get() == false) {
			this.startOven();
		}
	}
	
	public synchronized void startOven() {
		int currentSize = 0;
		ArrayList<ChargeReplyObject> nextOvenCharges = new ArrayList<ChargeReplyObject>(10);

		int i = 0;
		for (ChargeReplyObject charge : this.waitingList) {
			int chargeSize = charge.getCharge().size();
			if (currentSize + chargeSize <= this.MAX_OVEN_CHARGE) {
				currentSize += chargeSize;
				nextOvenCharges.add(charge);
			}
		}
		// Remove charges from waiting list
		this.waitingList.removeAll(nextOvenCharges);
		
		// Start oven
		if (this.ovenIsRunning.get() == false) {
			this.ovenIsRunning.set(true);
			Thread oven = new Thread(new Oven(this, nextOvenCharges));
			oven.start();
		}
	}
	
	public synchronized void stopOven(ArrayList<ChargeReplyObject> charges) {
		for (ChargeReplyObject charge : charges) {
			this.logger.info("Oven finished charge with id = " + charge.getCharge().get(0).getChargeId(), (Object[]) null);
		}
		
		// Tell baker that his charges is alright
		try {
			for (ChargeReplyObject replyObject : charges) {
				TextMessage responseMessage = this.ovenQueue_session.createTextMessage();
				responseMessage.setJMSCorrelationID(replyObject.getId());	
				responseMessage.setText("CHARGE READY: " + replyObject.getCharge().get(0).getChargeId() + ", bakerid=" + replyObject.getCharge().get(0).getBakerId());
				
				if (this.gingerBreadCounter.get() > 0) {
					responseMessage.setStringProperty("INFO", Messages.MESSAGE_MORE_INGREDIENTS_AVAILABLE);
				}
				
				this.bakerOven_replyProducer.send(replyObject.getDestination(), responseMessage);
				this.ovenQueue_session.commit();
			}
		} catch (JMSException e) {
			e.printStackTrace();
		}
		this.ovenIsRunning.set(false);
		if (this.waitingList.size() > 0) {
			startOven();
		}
	}

}
