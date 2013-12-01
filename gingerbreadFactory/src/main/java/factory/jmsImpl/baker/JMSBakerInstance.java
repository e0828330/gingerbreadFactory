package factory.jmsImpl.baker;

import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
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

import factory.entities.GingerBreadTransactionObject;
import factory.jmsImpl.server.JMSServerBakerIngredientsQueueListener;
import factory.utils.Messages;
import factory.utils.Utils;

public class JMSBakerInstance implements Runnable, MessageListener {

	private Context ctx;
	private boolean isRunning = true;
	private Logger logger = Logger.get(getClass());
	
	// Identifier for baker
	private Long id = 0L;
	
	// Helper attributes for topic and request handling
	private boolean serverHasNewIngredients = false;
	private boolean isWorking = false;

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

	public JMSBakerInstance(String propertiesFile) throws IOException,
			NamingException {
		Properties properties = new Properties();
		properties.load(this.getClass().getClassLoader()
				.getResourceAsStream(propertiesFile));
		this.ctx = new InitialContext(properties);
		
		try {
			// init topic for ingredients
			this.setup_ingredientsTopic();
			
			// Set queue connection for baker
			this.setup_bakerIngredientsQueue();			
		} catch (NamingException e) {
			e.printStackTrace();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	private void setup_ingredientsTopic() throws NamingException, JMSException {
		this.logger.info("Initializing topic for ingredients...",
				(Object[]) null);
		TopicConnectionFactory topicConnectionFactory = (TopicConnectionFactory) ctx
				.lookup("qpidConnectionfactory");
		this.ingredientsTopic_topic = (Topic) ctx.lookup("ingredientsTopic");
		this.ingredientsTopic_connection = topicConnectionFactory
				.createTopicConnection();
		this.ingredientsTopic_session = this.ingredientsTopic_connection
				.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
		this.ingredientsTopic_subscriber = this.ingredientsTopic_session
				.createSubscriber(this.ingredientsTopic_topic);
		this.ingredientsTopic_subscriber
				.setMessageListener(new JMSBakerIngredientsTopicListener(this));
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

	public void run() {
		// On startup send request for ingredients
		sendRequestForIngredients();
		do {	
		} while (isRunning);
		try {
			this.close();
		}
		catch (JMSException e) {
			e.printStackTrace();
		}
	}
	
	public void sendRequestForIngredients() {
		// Avoid requests while already requesting
		if (this.isWorking == false) {
			this.isWorking = true;
			try {
				Destination tempDest = this.bakerIngredients_session.createTemporaryQueue();
				MessageConsumer responseConsumer = bakerIngredients_session.createConsumer(tempDest);
				responseConsumer.setMessageListener(this);
				
				TextMessage message = this.bakerIngredients_session.createTextMessage(Messages.INGREDIENTS_REQUEST_MESSAGE);	
				message.setJMSReplyTo(tempDest);
				message.setJMSCorrelationID(String.valueOf(UUID.randomUUID().hashCode()) + String.valueOf(this.id));
				this.bakerIngredients_sender.send(message);
				this.bakerIngredients_session.commit();
				this.logger.info("Send request for ingredients to server", (Object[]) null);
			}
			catch (JMSException e) {
				e.printStackTrace();
			}
		}
		else {
			this.serverHasNewIngredients = true;
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
		
		this.logger.info("BakerInstance shutting down.", (Object[]) null); 		
	}

	/**
	 * Receiving responses from server to requests for ingredients
	 */
	public void onMessage(Message message) {
		this.logger.info("Response of ingredient-request received.", (Object[]) null); 
		if (message instanceof ObjectMessage) {
			ObjectMessage objMessage = (ObjectMessage) message;
			try {
				if (objMessage.getObject() instanceof GingerBreadTransactionObject) {
					GingerBreadTransactionObject obj = (GingerBreadTransactionObject) objMessage.getObject();
					System.out.println(obj.getEgg1().getType().toString() + " from Supplier with id = " + 
					obj.getEgg1().getSupplierId());
				}
			} catch (JMSException e) {
				e.printStackTrace();
			}
		}
		else if (message instanceof TextMessage) {
			TextMessage msg = (TextMessage) message;
			try {
				if (msg != null && msg.getText().equals(Messages.INGREDIENTS_RESPONSE_MESSAGE_NONE)) {
					this.logger.info("No ingredients available.", (Object[]) null);
				}
				else if (msg != null && msg.getText().equals(Messages.MESSAGE_END)) {
					// Bake
					
					// Check if new ingredients are published in topic, if not, we are ready and
					// we can listen to the topic again.
					// Else we request to server and set the serverHasNewIngredients variable to false
					if (this.serverHasNewIngredients == false) {
						this.isWorking = false;
					}
					else {
						this.serverHasNewIngredients = false;
						this.sendRequestForIngredients();
					}
				}
			}
			catch (JMSException e) {
				e.printStackTrace();
			}
		}
		
		try {
			this.bakerIngredients_session.commit();
		} catch (JMSException e) {
			e.printStackTrace();
		}

		// TODO: If ready, ask again for new ingredients
		// TODO: If no new ingredients are returend, Reconnect to Topic
	}
}