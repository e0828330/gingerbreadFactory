package factory.jmsImpl.baker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.qpid.transport.util.Logger;

import factory.entities.GingerBread;
import factory.entities.GingerBreadTransactionObject;
import factory.utils.Messages;
import factory.utils.Utils;

public class JMSBakerInstance implements Runnable {

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

	// baker-server queue
	private QueueConnection bakerIngredients_connection;
	private QueueSession bakerIngredients_session;
	private Queue bakerIngredients_queue;
	private QueueSender bakerIngredients_sender;
	private MessageConsumer bakerIngredients_consumer;

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
			// this.setup_ingredientsTopic();

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
		QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory) ctx.lookup("qpidConnectionfactory");
		this.qualityQueue_queue = (Queue) ctx.lookup("qualityControlQueue");
		this.qualityQueue_connection = queueConnectionFactory.createQueueConnection();
		this.qualityQueue_session = this.qualityQueue_connection.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE);
		this.qualityQueue_sender = this.qualityQueue_session.createSender(this.qualityQueue_queue);
		this.qualityQueue_connection.start();
		this.logger.info("Queue for quality-control startet.", (Object[]) null);
	}

	private void setup_bakerIngredientsQueue() throws NamingException, JMSException {
		this.logger.info("Initializing queue for bakers ingredients requests...", (Object[]) null);
		QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory) ctx.lookup("qpidConnectionfactory");
		this.bakerIngredients_queue = (Queue) ctx.lookup("bakerIngredientsQueue");
		this.bakerIngredients_connection = queueConnectionFactory.createQueueConnection();
		this.bakerIngredients_session = this.bakerIngredients_connection.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE);
		this.bakerIngredients_sender = this.bakerIngredients_session.createSender(this.bakerIngredients_queue);
		this.bakerIngredients_connection.start();
		this.logger.info("Queue for baker created and connection started.", (Object[]) null);
	}

	private void setup_ovenQueue() throws NamingException, JMSException {
		this.logger.info("Initializing queue for oven...", (Object[]) null);
		QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory) ctx.lookup("qpidConnectionfactory");

		this.ovenQueue_queue = (Queue) ctx.lookup("ovenQueue");
		this.ovenQueue_connection = queueConnectionFactory.createQueueConnection();
		this.ovenQueue_session = this.ovenQueue_connection.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE);
		this.ovenQueue_sender = this.ovenQueue_session.createSender(this.ovenQueue_queue);
		this.ovenQueue_connection.start();

		this.ovenProducer = this.ovenQueue_session.createProducer(null);
		this.ovenProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

		this.logger.info("Queue for oven created and connection started.", (Object[]) null);
	}

	public void run() {
		while (isRunning) {
			try {
				Destination tempDest = this.bakerIngredients_session.createTemporaryQueue();
				this.bakerIngredients_consumer = bakerIngredients_session.createConsumer(tempDest);

				TextMessage message = this.bakerIngredients_session.createTextMessage(Messages.INGREDIENTS_REQUEST_MESSAGE);
				message.setLongProperty("BAKER_ID", this.id);
				message.setJMSReplyTo(tempDest);
				message.setJMSCorrelationID(String.valueOf(UUID.randomUUID().hashCode()) + String.valueOf(this.id));
				this.bakerIngredients_sender.send(message);
				this.logger.info("Send request for ingredients to server", (Object[]) null);
				// Wait for receive
				this.logger.info("Waiting for response for new ingredients and blocking..", (Object[]) null);
				Message responseMessage = this.bakerIngredients_consumer.receive();
				this.logger.info("Received answer..", (Object[]) null);

				this.checkReponseMessage(responseMessage);

			} catch (JMSException e) {
				e.printStackTrace();
			}
		}
		try {
			this.close();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	public void shutDown() {
		this.isRunning = false;
	}

	private void close() throws JMSException {
		this.logger.info("Closing topic connection for ingredients.", (Object[]) null);

		this.logger.info("Closing baker-server queue.", (Object[]) null);
		this.bakerIngredients_sender.close();
		this.bakerIngredients_consumer.close();
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
					// acknowledge for receiving ingredients
					message.acknowledge();

					// sending to oven
					Destination tempDest = this.ovenQueue_session.createTemporaryQueue();
					MessageConsumer responseConsumer = ovenQueue_session.createConsumer(tempDest);

					ObjectMessage chargeMessage = this.ovenQueue_session.createObjectMessage();
					chargeMessage.setStringProperty("TYPE", "ArrayList<GingerBread>");
					chargeMessage.setObject(this.charge);

					chargeMessage.setLongProperty("BAKER_ID", this.id);
					chargeMessage.setJMSReplyTo(tempDest);
					chargeMessage.setJMSCorrelationID(String.valueOf(UUID.randomUUID().hashCode()) + String.valueOf(this.id));
					this.logger.info("Send charge : " + this.charge.size() + ", " + this.charge.get(0).getId());
					this.ovenQueue_sender.send(chargeMessage);

					this.logger.info("Waiting for oven and blocking..", (Object[]) null);

					Message responseMessage = responseConsumer.receive();
					if (responseMessage instanceof ObjectMessage) {
						ObjectMessage responseObjectMessage = (ObjectMessage) responseMessage;
						if (responseObjectMessage.getStringProperty("TYPE") != null && responseObjectMessage.getStringProperty("TYPE").equals("ArrayList<GingerBread>")) {

							@SuppressWarnings("unchecked")
							ArrayList<GingerBread> bakedCharge = (ArrayList<GingerBread>) responseObjectMessage.getObject();
							// forward to qualitycontrol
							ObjectMessage qualityObjectMessage = this.qualityQueue_session.createObjectMessage();
							qualityObjectMessage.setObject(bakedCharge);
							qualityObjectMessage.setStringProperty("TYPE", "ArrayList<GingerBread>");
							this.qualityQueue_sender.send(qualityObjectMessage);
							this.logger.info("Received baked charge of size= " + bakedCharge.size() + " and forwarded to quality control.", (Object[]) null);
						}
					}
					// acknowledge for receiving baked charge
					responseMessage.acknowledge();
					this.reset();
				}
			} catch (JMSException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void reset() {
		// Ready with producing, reset gingerBreadList for next request
		this.gingerBreadList.clear();
		// Reset charge for next request
		this.charge.clear();
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