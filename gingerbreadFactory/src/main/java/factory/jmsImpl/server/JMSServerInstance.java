package factory.jmsImpl.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.qpid.transport.util.Logger;

import factory.entities.BakerWaitingObject;
import factory.entities.ChargeReplyObject;
import factory.entities.GingerBread;
import factory.entities.GingerBreadTransactionObject;
import factory.entities.Ingredient;
import factory.utils.JMSUtils;
import factory.utils.JMSUtils.MessageType;
import factory.utils.Messages;

public class JMSServerInstance implements Runnable {

	private Context ctx;
	private boolean isRunning = true;
	private Logger logger = Logger.get(getClass());

	// Stores the bakerid -> ingredients relationship
	private ConcurrentHashMap<Long, ArrayList<GingerBreadTransactionObject>> delivered_ingredients;

	// Stores the gingerbread-ID -> gingerbread
	private ConcurrentHashMap<Long, GingerBread> gingerBreads;

	// Stores the waiting charges for the oven
	private List<ChargeReplyObject> ovenWaitingList;

	// Stores all bakers which are waiting for new ingredients
	private LinkedList<BakerWaitingObject> bakerWaitingList;

	// Stores the current charges in the oven
	ArrayList<ChargeReplyObject> nextOvenCharges;

	// Oven
	private AtomicBoolean ovenIsRunning;
	private final int MAX_OVEN_CHARGE = 10;

	// monitoring queue
	// Stores the states of the gingerbreads
	private QueueConnection monitoring_connection;
	private QueueSession monitoring_session;
	private Queue monitoring_queue;
	private QueueReceiver monitoring_receiver;

	// command queue
	// Receives commands from the monitor/gui
	private QueueConnection command_connection;
	private QueueSession command_session;
	private Queue command_queue;
	private QueueReceiver command_receiver;

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

	// event queue (sending events to gui)
	private QueueConnection eventQueue_connection;
	private QueueSession eventQueue_session;
	private Queue eventQueue_queue;
	private QueueSender eventQueue_sender;

	private List<Ingredient> honey_list;
	private List<Ingredient> flour_list;
	private List<Ingredient> egg_list;
	private ConcurrentHashMap<Long, Ingredient> total_ingredients_list;

	private AtomicInteger count_gingerBread_eggs;
	private AtomicInteger count_gingerBread_honey;
	private AtomicInteger count_gingerBread_flour;

	private AtomicInteger gingerBreadCounter;

	private JMSServerIngredientsDeliveryListener incredientsDelivery_listener;
	private JMSServerBakerIngredientsQueueListener bakerIngredientsQueue_listener;
	private JMSServerOvenQueueListener ovenQueue_listener;
	private JMSServerMonitoringListener monitoring_listener;
	private JMSServerCommandListener command_listener;
	
	private final String PROPERTIES_FILE = "jms.properties";

	public JMSServerInstance() throws IOException, NamingException, JMSException {
		Properties properties = new Properties();
		properties.load(this.getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE));
		this.ctx = new InitialContext(properties);

		// set ingredient storage
		this.honey_list = Collections.synchronizedList(new ArrayList<Ingredient>(50));
		this.flour_list = Collections.synchronizedList(new ArrayList<Ingredient>(50));
		this.egg_list = Collections.synchronizedList(new ArrayList<Ingredient>(50));
		this.total_ingredients_list = new ConcurrentHashMap<Long, Ingredient>(150);
		this.setBakerWaitingList(new LinkedList<BakerWaitingObject>());

		this.ovenWaitingList = Collections.synchronizedList(new ArrayList<ChargeReplyObject>());

		this.delivered_ingredients = new ConcurrentHashMap<Long, ArrayList<GingerBreadTransactionObject>>(40);

		this.nextOvenCharges = new ArrayList<ChargeReplyObject>(10);

		this.setGingerBreads(new ConcurrentHashMap<Long, GingerBread>(100));

		this.count_gingerBread_eggs = new AtomicInteger(0);
		this.count_gingerBread_flour = new AtomicInteger(0);
		this.count_gingerBread_honey = new AtomicInteger(0);
		this.gingerBreadCounter = new AtomicInteger(0);

		// Init oven
		this.ovenIsRunning = new AtomicBoolean(false);

		// Init all queues
		this.initQueues();
	}

	private void initQueues() throws IOException, NamingException, JMSException {
		// set queue for monitoring
		this.setup_monitoringQueue();

		// Set queue connection for ingredients
		this.setup_ingredientsQueue();

		// Set queue connection for baker
		this.setup_bakerIngredientsQueue();

		// Set queue connection for oven
		this.setup_ovenQueue();

		// set command queue
		this.setup_commandQueue();

		// set event queue
		this.setup_eventQueue();
	}

	private void setup_eventQueue() throws NamingException, JMSException {
		QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory) ctx.lookup("qpidConnectionfactory");
		this.eventQueue_queue = (Queue) ctx.lookup("eventQueue");
		this.eventQueue_connection = queueConnectionFactory.createQueueConnection();
		this.eventQueue_session = this.eventQueue_connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
		this.eventQueue_sender = this.eventQueue_session.createSender(this.eventQueue_queue);
		this.eventQueue_connection.start();
	}

	private void setup_commandQueue() throws NamingException, JMSException {
		QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory) ctx.lookup("qpidConnectionfactory");
		this.command_queue = (Queue) ctx.lookup("commandQueue");
		this.command_connection = queueConnectionFactory.createQueueConnection();
		this.command_session = this.command_connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
		this.command_receiver = this.command_session.createReceiver(this.command_queue);
		this.command_listener = new JMSServerCommandListener(this);
		this.command_receiver.setMessageListener(this.command_listener);
		this.command_connection.start();
	}

	private void setup_monitoringQueue() throws JMSException, NamingException {
		this.logger.info("Initializing queue for monitoring...", (Object[]) null);
		QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory) ctx.lookup("qpidConnectionfactory");
		this.monitoring_queue = (Queue) ctx.lookup("monitoringQueue");
		this.monitoring_connection = queueConnectionFactory.createQueueConnection();
		this.monitoring_session = this.monitoring_connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
		this.monitoring_receiver = this.monitoring_session.createReceiver(this.monitoring_queue);
		this.monitoring_listener = new JMSServerMonitoringListener(this);
		this.monitoring_receiver.setMessageListener(monitoring_listener);
		this.monitoring_connection.start();
		this.logger.info("Queue monitoring created and connection started.", (Object[]) null);
	}

	private void setup_bakerIngredientsQueue() throws NamingException, JMSException {
		this.logger.info("Initializing queue for bakers ingredients requests...", (Object[]) null);
		QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory) ctx.lookup("qpidConnectionfactory");
		this.bakerIngredients_queue = (Queue) ctx.lookup("bakerIngredientsQueue");
		this.bakerIngredients_connection = queueConnectionFactory.createQueueConnection();
		this.bakerIngredients_session = this.bakerIngredients_connection.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE);
		this.bakerIngredientsQueue_listener = new JMSServerBakerIngredientsQueueListener(this);
		this.bakerIngredients_receiver = this.bakerIngredients_session.createReceiver(this.bakerIngredients_queue);
		this.bakerIngredients_receiver.setMessageListener(this.bakerIngredientsQueue_listener);
		this.bakerIngredients_connection.start();
		this.logger.info("Queue for baker created and connection started.", (Object[]) null);
	}

	private void setup_ingredientsQueue() throws IOException, NamingException, JMSException {
		this.logger.info("Initializing queue for ingredients...", (Object[]) null);
		QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory) ctx.lookup("qpidConnectionfactory");
		this.ingredientsDelivery_queue = (Queue) ctx.lookup("ingredientsDelivery");
		this.ingredientsDelivery_connection = queueConnectionFactory.createQueueConnection();
		this.ingredientsDelivery_session = this.ingredientsDelivery_connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
		this.incredientsDelivery_listener = new JMSServerIngredientsDeliveryListener(this);
		this.ingredientsDelivery_receiver = this.ingredientsDelivery_session.createReceiver(this.ingredientsDelivery_queue);
		this.ingredientsDelivery_receiver.setMessageListener(this.incredientsDelivery_listener);
		this.ingredientsDelivery_connection.start();
		this.logger.info("Queue for incredients created and connection started.", (Object[]) null);
	}

	private void setup_ovenQueue() throws NamingException, JMSException {
		this.logger.info("Initializing queue for oven...", (Object[]) null);
		QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory) ctx.lookup("qpidConnectionfactory");
		this.ovenQueue_queue = (Queue) ctx.lookup("ovenQueue");
		this.ovenQueue_connection = queueConnectionFactory.createQueueConnection();
		this.ovenQueue_session = this.ovenQueue_connection.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE);
		this.ovenQueue_receiver = this.ovenQueue_session.createReceiver(this.ovenQueue_queue);
		this.ovenQueue_listener = new JMSServerOvenQueueListener(this);
		this.ovenQueue_receiver.setMessageListener(this.ovenQueue_listener);
		this.ovenQueue_connection.start();
		this.logger.info("Queue for oven created and connection started.", (Object[]) null);
	}

	public void run() {
		System.out.println("\n======================================");
		System.out.println("Type 'storage' to see the stored ingredients");
		System.out.println("Type 'oven_state' to see the state of the oven");
		System.out.println("Type 'monitor' to see the state of gingerbreads");
		System.out.println("Type 'exit' to to shut down the server");
		System.out.println("======================================\n");
		while (isRunning) {
			try {
				BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
				String s = bufferRead.readLine();
				if (s.equalsIgnoreCase("storage")) {
					this.printStorage();
				} else if (s.equalsIgnoreCase("oven_state")) {
					if (this.ovenIsRunning.get() == true) {
						System.out.println("Oven is running.");
					} else {
						System.out.println("Oven is ready.");
					}

				} else if (s.equals("monitor")) {
					System.out.print("\n");
					System.out.println("GINGERBREAD_ID \t\t CHARGE_ID \t\t BAKER_ID \t\t STATE");
					for (Entry<Long, GingerBread> gingerBread : this.gingerBreads.entrySet()) {
						System.out.println(gingerBread.getKey() + "\t\t" + gingerBread.getValue().getChargeId() + "\t\t" + gingerBread.getValue().getBakerId() + "\t\t"
								+ gingerBread.getValue().getState().toString());
					}
					System.out.print("\n");
				} else if (s.equals("exit")) {
					break;
				}
			} catch (IOException e) {
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
		} catch (JMSException e) {
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
		this.bakerIngredients_session.close();
		this.bakerIngredients_connection.close();

		this.logger.info("Closing oven queue.", (Object[]) null);
		this.ovenQueue_receiver.close();
		this.ovenQueue_session.close();
		this.ovenQueue_connection.close();

		this.logger.info("Closing event queue.", (Object[]) null);
		this.eventQueue_sender.close();
		this.eventQueue_session.close();
		this.eventQueue_connection.close();

		this.logger.info("Closing monitoring queue.", (Object[]) null);
		this.monitoring_receiver.close();
		this.monitoring_session.close();
		this.monitoring_connection.close();

		this.logger.info("Closing command queue.", (Object[]) null);
		this.command_receiver.close();
		this.command_session.close();
		this.command_connection.close();

		this.logger.info("ServerInstance shutting down.", (Object[]) null);
	}

	public void shutDown() {
		this.isRunning = false;
	}

	public synchronized void storeIncredient(Ingredient ingredient) {
		this.total_ingredients_list.put(ingredient.getId(), ingredient);
		this.logger.info("Stored " + ingredient.getType().toString(), (Object[]) null);
		if (ingredient.getType() == Ingredient.Type.FLOUR) {
			this.logger.info("Added flour to list.", (Object[]) null);
			this.flour_list.add(ingredient);
			this.count_gingerBread_flour.incrementAndGet();
		} else if (ingredient.getType() == Ingredient.Type.HONEY) {
			this.logger.info("Added honey to list.", (Object[]) null);
			this.honey_list.add(ingredient);
			this.count_gingerBread_honey.incrementAndGet();
		} else if (ingredient.getType() == Ingredient.Type.EGG) {
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
			this.logger.info("Created new gingerbread. Count = " + this.gingerBreadCounter, (Object[]) null);
		}

		if (this.bakerWaitingList.size() > 0 && this.gingerBreadCounter.get() > 0) {
			this.logger.info("Dequeue baker from waiting list and send him ingredients.", (Object[]) null);
			try {
				ArrayList<GingerBreadTransactionObject> ingredients = this.getGingerBreadIngredients(5);

				BakerWaitingObject baker = bakerWaitingList.pop();

				ObjectMessage response = this.bakerIngredients_session.createObjectMessage();
				response.setJMSCorrelationID(baker.getId());
				response.setObject(ingredients);
				response.setStringProperty("TYPE", "ArrayList<GingerBreadTransactionObject>");
				MessageProducer producer = this.bakerIngredients_session.createProducer(baker.getDestination());
				producer.send(response);
				producer.close();
			} catch (JMSException e) {
				e.printStackTrace();
			}
		}
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

	public synchronized ArrayList<GingerBreadTransactionObject> getGingerBreadIngredients(int max) {
		int i = 0;
		ArrayList<GingerBreadTransactionObject> tmpList = new ArrayList<GingerBreadTransactionObject>(max);
		int limit = this.gingerBreadCounter.get();
		try {
			for (; i < limit; i++) {
				if (i >= max)
					break;
				Ingredient egg1 = this.egg_list.remove(0);
				Ingredient egg2 = this.egg_list.remove(0);
				Ingredient flour = this.flour_list.remove(0);
				Ingredient honey = this.honey_list.remove(0);

				tmpList.add(new GingerBreadTransactionObject(egg1, egg2, flour, honey));

				this.gingerBreadCounter.decrementAndGet();

				this.total_ingredients_list.remove(egg1.getId());
				this.total_ingredients_list.remove(egg2.getId());
				this.total_ingredients_list.remove(flour.getId());
				this.total_ingredients_list.remove(honey.getId());
			}

			debugMessageStoredIngredients();
			return tmpList;
		} catch (Exception e) {
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
		this.logger.info("\novercharged eggs = " + this.count_gingerBread_eggs + "\n" + "overcharged flour = " + this.count_gingerBread_flour + "\n" + "overcharged honey = "
				+ this.count_gingerBread_honey + "\n" + "gingerbreads possible = " + this.gingerBreadCounter.get() + "\n", (Object[]) null);
	}

	private void printStorage() {
		System.out.println("\neggs = " + this.egg_list.size() + "\n" + "flour = " + this.flour_list.size() + "\n" + "honey = " + this.honey_list.size() + "\n" + "gingerbreads possible = "
				+ this.gingerBreadCounter.get() + "\n");
	}

	public ConcurrentHashMap<Long, ArrayList<GingerBreadTransactionObject>> getDelivered_ingredients() {
		return delivered_ingredients;
	}

	public synchronized void addToOven(ChargeReplyObject replyObject) {
		this.ovenWaitingList.add(replyObject);
		if (this.ovenIsRunning.get() == false) {
			this.startOven();
		}
	}

	public synchronized void startOven() {
		int currentSize = 0;
		this.nextOvenCharges.clear();

		for (ChargeReplyObject charge : this.ovenWaitingList) {
			int chargeSize = charge.getCharge().size();
			if (currentSize + chargeSize <= this.MAX_OVEN_CHARGE) {
				currentSize += chargeSize;
				this.nextOvenCharges.add(charge);
			}
		}

		// Remove charges from waiting list
		this.ovenWaitingList.removeAll(this.nextOvenCharges);

		// Start oven
		if (this.ovenIsRunning.get() == false) {
			this.ovenIsRunning.set(true);
			Hashtable<String, String> properties = new Hashtable<String, String>(2);
			properties.put("EVENT", Messages.EVENT_NEW_OVENT_CHARGE);
			properties.put("TYPE", "ArrayList<GingerBread>");
			this.sendEventToGUI(this.nextOvenCharges, properties);
			Thread oven = new Thread(new Oven(this, this.nextOvenCharges));
			oven.start();
		}
	}

	public synchronized void stopOven(ArrayList<ChargeReplyObject> charges) {
		for (ChargeReplyObject charge : charges) {
			this.logger.info("Oven finished charge with id = " + charge.getCharge().get(0).getChargeId(), (Object[]) null);
		}

		// Tell baker that his charges is ready
		try {
			for (ChargeReplyObject replyObject : charges) {
				ObjectMessage responseMessage = this.ovenQueue_session.createObjectMessage();
				responseMessage.setJMSCorrelationID(replyObject.getId());
				responseMessage.setStringProperty("TYPE", "ArrayList<GingerBread>");
				responseMessage.setObject(replyObject.getCharge());
				MessageProducer producer = this.ovenQueue_session.createProducer(replyObject.getDestination());
				producer.send(responseMessage);
				producer.close();
			}
		} catch (JMSException e) {
			e.printStackTrace();
		}
		this.ovenIsRunning.set(false);
		if (this.ovenWaitingList.size() > 0) {
			startOven();
		}
	}

	public LinkedList<BakerWaitingObject> getBakerWaitingList() {
		return bakerWaitingList;
	}

	public void setBakerWaitingList(LinkedList<BakerWaitingObject> bakerWaitingList) {
		this.bakerWaitingList = bakerWaitingList;
	}

	public ConcurrentHashMap<Long, GingerBread> getGingerBreads() {
		return gingerBreads;
	}

	public void setGingerBreads(ConcurrentHashMap<Long, GingerBread> gingerBreads) {
		this.gingerBreads = gingerBreads;
	}

	public ConcurrentHashMap<Long, Ingredient> get_total_ingredients_list() {
		return this.total_ingredients_list;
	}

	public QueueSession get_CommandSession() {
		return this.command_session;
	}

	public ArrayList<ChargeReplyObject> get_nextOvenCharges() {
		return this.nextOvenCharges;
	}

	public void sendEventToGUI(Object payLoad, Hashtable<String, String> properties) {
		try {
			JMSUtils.sendMessage(MessageType.OBJECTMESSAGE, payLoad, properties, this.eventQueue_session, false, this.eventQueue_sender);
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

}
