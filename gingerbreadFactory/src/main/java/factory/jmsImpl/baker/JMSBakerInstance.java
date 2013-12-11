package factory.jmsImpl.baker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
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
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.qpid.transport.util.Logger;
import org.mozartspaces.core.Entry;

import factory.entities.Charge;
import factory.entities.ChargeReplyObject;
import factory.entities.GingerBread;
import factory.entities.GingerBreadTransactionObject;
import factory.jmsImpl.server.JMSServerBakerIngredientsQueueListener;
import factory.jmsImpl.server.JMSServerOvenQueueListener;
import factory.utils.Messages;
import factory.utils.Utils;

public class JMSBakerInstance implements Runnable, MessageListener {

	private Context ctx;
	private boolean isRunning = true;
	private Logger logger = Logger.get(getClass());
	
	// Charge
	private ArrayList<GingerBread> charge;
	
	// Ingredients
	private ArrayList<GingerBreadTransactionObject> gingerBreadList;
	
	// Identifier for baker
	private Long id = 0L;
	
	// total number of charges produced
	private ConcurrentHashMap<Long, Integer> producedCharges;
	
	// Helper attributes for topic and request handling
	private AtomicBoolean serverHasMoreIngredients;
	private AtomicBoolean isWorking;

	// ingredients topic attributes
	private Topic ingredientsTopic_topic;
	private TopicConnection ingredientsTopic_connection;
	private TopicSession ingredientsTopic_session;
	private TopicSubscriber ingredientsTopic_subscriber;
	
	// baker-server queue
	private QueueConnection bakerIngredients_connection;
	private QueueSession bakerIngredients_session;
	private Queue bakerIngredients_queue;
	private QueueSender bakerIngredients_sender;	
	
	// oven queue
	private QueueConnection ovenQueue_connection;
	private QueueSession ovenQueue_session;
	private Queue ovenQueue_queue;
	private QueueSender ovenQueue_sender;
	private MessageProducer ovenProducer;
	
	// quality-control queue
	private QueueConnection qualityQueue_connection;
	private QueueSession qualityQueue_session;
	private Queue qualityQueue_queue;	
	private QueueSender qualityQueue_sender;


	public JMSBakerInstance(String propertiesFile) throws IOException, NamingException {
		this.id = Utils.getID().longValue();
		Properties properties = new Properties();
		properties.load(this.getClass().getClassLoader().getResourceAsStream(propertiesFile));
		this.ctx = new InitialContext(properties);
		this.gingerBreadList = new ArrayList<GingerBreadTransactionObject>(5);
		this.charge = new ArrayList<GingerBread>(10);
		this.producedCharges = new ConcurrentHashMap<Long, Integer>();
		try {
			// init topic for ingredients
			this.setup_ingredientsTopic();
			
			// Set queue connection for baker
			this.setup_bakerIngredientsQueue();	
			
			// Set queue for oven
			this.setup_ovenQueue();
			
			// set queue for quality control
			this.setup_qualityControlQueue();
			
			this.isWorking = new AtomicBoolean();
			this.serverHasMoreIngredients = new AtomicBoolean();
			
			this.isWorking.set(false);
			this.serverHasMoreIngredients.set(false);
			
		} catch (NamingException e) {
			e.printStackTrace();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}
	
	
	private void setup_qualityControlQueue() throws NamingException, JMSException {
		this.logger.info("Initializing queue for bakers quality-control requests...", (Object[]) null); 
		QueueConnectionFactory queueConnectionFactory = 
				  (QueueConnectionFactory) ctx.lookup("qpidConnectionfactory");
		this.qualityQueue_queue = (Queue) ctx.lookup("qualityControlQueue");
		this.qualityQueue_connection = queueConnectionFactory.createQueueConnection();
		this.qualityQueue_session = this.qualityQueue_connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
		this.qualityQueue_sender = this.qualityQueue_session.createSender(this.qualityQueue_queue);
		this.qualityQueue_connection.start();	
		this.logger.info("Queue for quality-control startet.", (Object[]) null); 		
	}	

	private void setup_ingredientsTopic() throws NamingException, JMSException {
		this.logger.info("Initializing topic for ingredients...", (Object[]) null);
		TopicConnectionFactory topicConnectionFactory = (TopicConnectionFactory) ctx.lookup("qpidConnectionfactory");
		this.ingredientsTopic_topic = (Topic) ctx.lookup("ingredientsTopic");
		this.ingredientsTopic_connection = topicConnectionFactory.createTopicConnection();
		this.ingredientsTopic_session = this.ingredientsTopic_connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
		this.ingredientsTopic_subscriber = this.ingredientsTopic_session.createSubscriber(this.ingredientsTopic_topic);
		this.ingredientsTopic_subscriber.setMessageListener(new JMSBakerIngredientsTopicListener(this));
		this.ingredientsTopic_connection.start();
	}
	
	private void setup_bakerIngredientsQueue() throws NamingException, JMSException {
		this.logger.info("Initializing queue for bakers ingredients requests...", (Object[]) null); 
		QueueConnectionFactory queueConnectionFactory = 
				  (QueueConnectionFactory) ctx.lookup("qpidConnectionfactory");
		this.bakerIngredients_queue = (Queue) ctx.lookup("bakerIngredientsQueue");
		this.bakerIngredients_connection = queueConnectionFactory.createQueueConnection();
		this.bakerIngredients_session = this.bakerIngredients_connection.createQueueSession(true, Session.AUTO_ACKNOWLEDGE);
		this.bakerIngredients_sender = this.bakerIngredients_session.createSender(this.bakerIngredients_queue);
		this.bakerIngredients_connection.start();	
		this.logger.info("Queue for baker created and connection started.", (Object[]) null); 		
	}	
	
	private void setup_ovenQueue() throws NamingException, JMSException {
		this.logger.info("Initializing queue for oven...", (Object[]) null); 
		QueueConnectionFactory queueConnectionFactory = 
				  (QueueConnectionFactory) ctx.lookup("qpidConnectionfactory");
		
		this.ovenQueue_queue = (Queue) ctx.lookup("ovenQueue");
		this.ovenQueue_connection = queueConnectionFactory.createQueueConnection();
		this.ovenQueue_session = this.ovenQueue_connection.createQueueSession(true, Session.AUTO_ACKNOWLEDGE);
		this.ovenQueue_sender = this.ovenQueue_session.createSender(this.ovenQueue_queue);
		this.ovenQueue_connection.start();
		
		this.ovenProducer = this.ovenQueue_session.createProducer(null);
		this.ovenProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		
		this.logger.info("Queue for oven created and connection started.", (Object[]) null); 
	}	

	public void run() {
		System.out.println("\n======================================");
		System.out.println("Type 'exit' to to shut down the baker");
		System.out.println("Type 'info' to show already produced charges.");
		System.out.println("Type 'state' to show the current state.");
		System.out.println("======================================\n");
		// On startup send request for ingredients
		sendRequestForIngredients(true);
		while (isRunning) {		
			try {
				BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
				String s = bufferRead.readLine();
				if (s.equals("exit")) {
					break;
				}
				else if (s.equals("info")) {
					for (Map.Entry<Long, Integer> element: this.producedCharges.entrySet()) {
						System.out.println("Charge with id = " + element.getKey() + " produced with " + element.getValue() + " gingerbreads.");
					}
				}
				else if (s.equals("state")) {
					if (this.isWorking.get()) {
						System.out.println("Baker ist working at the moment...");
					}
					else {
						System.out.println("Baker ist waiting for new ingredients.");
					}
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
	
	public synchronized void sendRequestForIngredients(boolean explicitRequest) {
		// Avoid requests while already requesting
		if (this.isWorking.get() == false || explicitRequest) {
			this.isWorking.set(true);
			try {
				Destination tempDest = this.bakerIngredients_session.createTemporaryQueue();
				MessageConsumer responseConsumer = bakerIngredients_session.createConsumer(tempDest);
				//responseConsumer.setMessageListener(this);
				
				TextMessage message = this.bakerIngredients_session.createTextMessage(Messages.INGREDIENTS_REQUEST_MESSAGE);	
				message.setLongProperty("BAKER_ID", this.id);
				message.setJMSReplyTo(tempDest);
				message.setJMSCorrelationID(String.valueOf(UUID.randomUUID().hashCode()) + String.valueOf(this.id));
				this.bakerIngredients_sender.send(message);
				this.bakerIngredients_session.commit();
				this.logger.info("Send request for ingredients to server", (Object[]) null);
				// Wait for receive
				this.logger.info("Waiting for response for new ingredients and blocking..", (Object[]) null);
				Message responseMessage = responseConsumer.receive();
				this.checkReponseMessage(responseMessage);
			}
			catch (JMSException e) {
				e.printStackTrace();
			}
		}
		else {
			this.serverHasMoreIngredients.set(true);
		}
	}

	public void shutDown() {
		this.isRunning = false;
	}

	private void close() throws JMSException {
		this.logger.info("Closing topic connection for ingredients.", (Object[]) null);
		this.ingredientsTopic_subscriber.close();
		this.ingredientsTopic_session.close();
		this.ingredientsTopic_connection.close();
		
		this.logger.info("Closing baker-server queue.", (Object[]) null);
		this.bakerIngredients_sender.close();
		this.bakerIngredients_session.close();
		this.bakerIngredients_connection.close();

		this.logger.info("Closing quality queue.", (Object[]) null);
		this.qualityQueue_sender.close();
		this.qualityQueue_session.close();
		this.qualityQueue_connection.close();
		
		this.logger.info("Closing oven queue.", (Object[]) null);
		this.ovenQueue_sender.close();
		this.ovenQueue_session.close();
		this.ovenQueue_connection.close();
		this.ovenProducer.close();
		
		this.logger.info("BakerInstance shutting down.", (Object[]) null); 		
	}

	/**
	 * Receiving responses from server to requests for ingredients
	 */
	public void onMessage(Message message) {
		if (message instanceof TextMessage) {
			TextMessage msg = (TextMessage) message;
			try {
				if (msg != null && msg.getText().equals(Messages.MESSAGE_MORE_INGREDIENTS_AVAILABLE)) {
					this.serverHasMoreIngredients.set(true);
				}
			}
			catch (JMSException e) {
				e.printStackTrace();
			} 
		}		
	}
	
	private void checkReponseMessage(Message message) {
		this.logger.info("Response of ingredient-request received.", (Object[]) null); 
		if (message instanceof ObjectMessage) {
			ObjectMessage objMessage = (ObjectMessage) message;
			try {
				if (objMessage.getStringProperty("TYPE").equals("ArrayList<GingerBreadTransactionObject>")) {
					@SuppressWarnings("unchecked")
					ArrayList<GingerBreadTransactionObject> list = (ArrayList<GingerBreadTransactionObject>) objMessage.getObject();
					for (GingerBreadTransactionObject obj : list) {
						this.gingerBreadList.add(obj);
					}
				}
				this.logger.info("Produce charge...", (Object[]) null);
				// Produce the charge
				Long chargeId = Utils.getID();
				for (GingerBreadTransactionObject obj : this.gingerBreadList) {
					
					GingerBread tmp = new GingerBread();
					tmp.setId(Utils.getID());
					tmp.setBakerId(this.id);
					tmp.setChargeId(chargeId);
					tmp.setFlourId(obj.getFlour().getId());
					tmp.setHoneyId(obj.getHoney().getId());
					tmp.setFirstEggId(obj.getEgg1().getId());
					tmp.setSecondEggId(obj.getEgg2().getId());
					tmp.setState(GingerBread.State.PRODUCED);
					this.charge.add(tmp);
					Thread.sleep(Utils.getRandomWaitTime());
				}
				this.producedCharges.put(chargeId, this.charge.size());
				
				Destination tempDest = this.ovenQueue_session.createTemporaryQueue();
				MessageConsumer responseConsumer = ovenQueue_session.createConsumer(tempDest);
				
				ObjectMessage chargeMessage = this.ovenQueue_session.createObjectMessage();
				chargeMessage.setStringProperty("TYPE", "ArrayList<GingerBread>");
				chargeMessage.setObject(this.charge);
				
				
				chargeMessage.setLongProperty("BAKER_ID", this.id);
				chargeMessage.setJMSReplyTo(tempDest);
				chargeMessage.setJMSCorrelationID(String.valueOf(UUID.randomUUID().hashCode()) + String.valueOf(this.id));
				this.ovenQueue_sender.send(chargeMessage);
				this.ovenQueue_session.commit();
				
				this.logger.info("Waiting for oven and blocking..", (Object[]) null);
				
				Message responseMessage = responseConsumer.receive();
				if (responseMessage instanceof ObjectMessage) {
					ObjectMessage responseObjectMessage = (ObjectMessage) responseMessage;
					if (responseObjectMessage.getStringProperty("TYPE") != null &&
							responseObjectMessage.getStringProperty("TYPE").equals("ArrayList<GingerBread>")) {
						@SuppressWarnings("unchecked")
						ArrayList<GingerBread> bakedCharge = (ArrayList<GingerBread>) responseObjectMessage.getObject();
						// forward to qualitycontrol
						ObjectMessage qualityObjectMessage = this.qualityQueue_session.createObjectMessage();
						qualityObjectMessage.setObject(bakedCharge);
						qualityObjectMessage.setStringProperty("TYPE", "ArrayList<GingerBread>");
						this.qualityQueue_sender.send(qualityObjectMessage);
						//this.qualityQueue_session.commit();
					}
				}
				this.logger.info("Received baked charge and forwarded to quality control.", (Object[]) null);
				
				
				if (responseMessage.getStringProperty("INFO") != null && 
						responseMessage.getStringProperty("INFO").equals(Messages.MESSAGE_MORE_INGREDIENTS_AVAILABLE)) {
					this.serverHasMoreIngredients.set(true);
				}
				
				this.reset();
			} catch (JMSException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		else if (message instanceof TextMessage) {
			TextMessage msg = (TextMessage) message;
			try {
				if (msg != null && msg.getText().equals(Messages.INGREDIENTS_RESPONSE_MESSAGE_NONE)) {
					this.logger.info("No ingredients available.", (Object[]) null);
					this.isWorking.set(false);
				}
			}
			catch (JMSException e) {
				e.printStackTrace();
			} 
		}
		
		/*try {
			this.bakerIngredients_session.commit();
		} catch (JMSException e) {
			e.printStackTrace();
		}	*/	
	}
	
	private void reset() {
		// Ready with producing, reset gingerBreadList for next request
		this.gingerBreadList.clear();
		// Reset charge for next request
		this.charge.clear();
		
		// Check if new ingredients are published in topic, if not, we are ready and
		// we can listen to the topic again.
		// Else we request to server and set the serverHasNewIngredients variable to false
		// Set isWorking to false, because current producing step is finished
		if (this.serverHasMoreIngredients.get() == true) {
			this.sendRequestForIngredients(true);
			this.serverHasMoreIngredients.set(false);
		}							
	}
	
	public boolean getIsWorking() {
		return this.isWorking.get();
	}
	
	public boolean getServerHasMoreIngredients() {
		return this.serverHasMoreIngredients.get();
	}
	
	public void setServerHasMoreIngredients(boolean value) {
		this.serverHasMoreIngredients.set(true);
	}
}