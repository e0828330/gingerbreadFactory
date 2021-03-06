package factory.jmsImpl.supplier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Properties;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.qpid.transport.util.Logger;

import factory.entities.Ingredient;
import factory.entities.Ingredient.Type;
import factory.interfaces.Supplier;
import factory.utils.JMSUtils;
import factory.utils.JMSUtils.MessageType;
import factory.utils.Utils;

public class JMSSupplierInstance implements Supplier {

	private final String PROPERTIES_FILE = "jms.properties";
	
	private Long id = 0L;
	private int amount;
	private Type type;

	private Context ctx;
	private Logger logger = Logger.get(getClass());

	// ingredient queue
	private QueueConnection ingredientsDelivery_connection;
	private QueueSession ingredientsDelivery_session;
	private Queue ingredientsDelivery_queue;

	private QueueSender ingredientsDelivery_sender;

	// produced ingredients
	private ArrayList<Ingredient> ingredients;

	private int factoryID;
	
	public JMSSupplierInstance(int factoryID) {
		try {
			this.factoryID = factoryID;
			this.logger.info("Starting with factoryID = " + this.factoryID, (Object[]) null);
			Properties properties = new Properties();
			properties.load(this.getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE));
			JMSUtils.extendJMSProperties(properties, this.factoryID);
			this.ctx = new InitialContext(properties);
	
			// init ingredient list
			this.ingredients = new ArrayList<Ingredient>(this.amount);
	
			// Set queue connection for communication with server
			this.setup_ingredientsQueue();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setup_ingredientsQueue() throws IOException, NamingException, JMSException {
		this.logger.info("Initializing queue for ingredients...", (Object[]) null);
		QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory) ctx.lookup("qpidConnectionfactory");
		this.ingredientsDelivery_queue = (Queue) ctx.lookup("ingredientsDelivery" + this.factoryID);
		this.ingredientsDelivery_connection = queueConnectionFactory.createQueueConnection();
		this.ingredientsDelivery_session = this.ingredientsDelivery_connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
		this.ingredientsDelivery_sender = this.ingredientsDelivery_session.createSender(ingredientsDelivery_queue);
		this.ingredientsDelivery_sender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		this.ingredientsDelivery_connection.start();
		this.logger.info("Queue for incredients created and connection started.", (Object[]) null);
	}

	public void run() {

		try {
			// produce all ingredients
			for (int i = 0; i < amount; i++) {
				Ingredient item = new Ingredient(id, Utils.getID(), type);
				Hashtable<String, String> properties = new Hashtable<String, String>();
				properties.put("TYPE", "ArrayList<Ingredient>");
				this.ingredients.add(item);
				JMSUtils.sendMessage(MessageType.OBJECTMESSAGE, 
						ingredients, 
						properties,
						this.ingredientsDelivery_session, 
						true, 
						this.ingredientsDelivery_sender);
				this.ingredients.clear();
				Thread.sleep(Utils.getRandomWaitTime());
			}
			
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (JMSException e) {
			e.printStackTrace();
		}
		try {
			this.close();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	private void close() throws JMSException {
		this.logger.info("Closing connection for queue.", (Object[]) null);
		this.ingredientsDelivery_sender.close();
		this.ingredientsDelivery_session.close();
		this.ingredientsDelivery_connection.close();
		this.logger.info("SupplierInstance shutting down.", (Object[]) null);
	}

	public void setId(Long id) {
		this.id = id;

	}

	public void placeOrder(int amount, Type type) {
		this.amount = amount;
		this.type = type;

	}

}
