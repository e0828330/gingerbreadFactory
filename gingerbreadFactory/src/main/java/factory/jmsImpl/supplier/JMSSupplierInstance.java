package factory.jmsImpl.supplier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.UUID;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.qpid.transport.util.Logger;

import factory.entities.Ingredient;
import factory.entities.Ingredient.Type;
import factory.interfaces.Supplier;
import factory.utils.Messages;
import factory.utils.Utils;

public class JMSSupplierInstance implements Supplier {

	private Long id;
	private int amount;
	private Type type;

	private Context ctx;
	private boolean isRunning = false;
	private Logger logger = Logger.get(getClass());

	// ingredient queue
	private QueueConnection ingredientsDelivery_connection;
	private QueueSession ingredientsDelivery_session;
	private Queue ingredientsDelivery_queue;

	private QueueSender ingredientsDelivery_sender;

	// produced ingredients
	private ArrayList<Ingredient> ingredients;

	public JMSSupplierInstance(String propertiesFile) throws IOException, NamingException, JMSException {
		Properties properties = new Properties();
		properties.load(this.getClass().getClassLoader().getResourceAsStream(propertiesFile));
		this.ctx = new InitialContext(properties);

		// init ingredient list
		this.ingredients = new ArrayList<Ingredient>(this.amount);

		// Set queue connection for communication with server
		this.setup_ingredientsQueue();
	}

	private void setup_ingredientsQueue() throws IOException, NamingException, JMSException {
		this.logger.info("Initializing queue for ingredients...", (Object[]) null);
		QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory) ctx.lookup("qpidConnectionfactory");
		this.ingredientsDelivery_queue = (Queue) ctx.lookup("ingredientsDelivery");
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
				this.ingredients.add(item);
				Thread.sleep(Utils.getRandomWaitTime());
			}
			ObjectMessage objectMessage = this.ingredientsDelivery_session.createObjectMessage();
			objectMessage.setObject(this.ingredients);
			objectMessage.setStringProperty("TYPE", "ArrayList<Ingredient>");

			Destination tempDest = this.ingredientsDelivery_session.createTemporaryQueue();
			MessageConsumer consumer = this.ingredientsDelivery_session.createConsumer(tempDest);
			objectMessage.setJMSReplyTo(tempDest);
			objectMessage.setJMSCorrelationID(String.valueOf(UUID.randomUUID().hashCode()) + String.valueOf(this.id));
			this.ingredientsDelivery_sender.send(objectMessage);

			TextMessage textMessage = (TextMessage) consumer.receive();
			System.out.println("received = " + textMessage);

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

	public void shutDown() {
		this.isRunning = false;
	}

}
