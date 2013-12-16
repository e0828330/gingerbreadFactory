package factory.jmsImpl.baker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
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
import factory.utils.JMSMonitoringSender;
import factory.utils.JMSUtils;
import factory.utils.JMSUtils.MessageType;
import factory.utils.Messages;
import factory.utils.Utils;

public class JMSBakerInstance implements Runnable {

	private final String PROPERTIES_FILE = "jms.properties";

	private Context ctx;
	private boolean isRunning = true;
	private Logger logger = Logger.get(getClass());

	// For monitoring
	private JMSMonitoringSender monitoringSender;

	// Charge
	private ArrayList<GingerBread> charge;

	// Ingredients
	private ArrayList<GingerBreadTransactionObject> gingerBreadList;

	// Identifier for baker
	private Long id = 0L;

	// total number of charges produced
	private ConcurrentHashMap<Long, Integer> producedCharges;

	// baker-server queue for ingredients
	private QueueConnection bakerIngredients_connection;
	private QueueSession bakerIngredients_session;
	private Queue bakerIngredients_queue;
	private QueueSender bakerIngredients_sender;
	private MessageConsumer bakerIngredients_consumer;

	// baker request queue (if baker died and ingredients or elements are baked
	// and were not delivered)
	private QueueConnection bakerRequest_connection;
	private QueueSession bakerRequest_session;
	private Queue bakerRequest_queue;
	private QueueSender bakerRequest_sender;

	// oven queue
	private QueueConnection ovenQueue_connection;
	private QueueSession ovenQueue_session;
	private Queue ovenQueue_queue;
	private QueueSender ovenQueue_sender;

	// quality-control queue
	private QueueConnection qualityQueue_connection;
	private QueueSession qualityQueue_session;
	private Queue qualityQueue_queue;
	private QueueSender qualityQueue_sender;

	public JMSBakerInstance(Long id) throws IOException, NamingException {
		this.id = id;
		Properties properties = new Properties();
		properties.load(this.getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE));
		this.ctx = new InitialContext(properties);
		this.gingerBreadList = new ArrayList<GingerBreadTransactionObject>(5);
		this.charge = new ArrayList<GingerBread>(10);
		this.producedCharges = new ConcurrentHashMap<Long, Integer>();
		try {
			this.monitoringSender = new JMSMonitoringSender(this.ctx);

			// Set queue connection for baker
			this.setup_bakerIngredientsQueue();

			// Set queue for oven
			this.setup_ovenQueue();

			// set queue for quality control
			this.setup_qualityControlQueue();

			// set baker request queue
			this.setup_bakerRequestQueue();

		} catch (NamingException e) {
			e.printStackTrace();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	private void setup_bakerRequestQueue() throws NamingException, JMSException {
		QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory) ctx.lookup("qpidConnectionfactory");
		this.bakerRequest_queue = (Queue) this.ctx.lookup("bakerRequestQueue");
		this.bakerRequest_connection = queueConnectionFactory.createQueueConnection();
		this.bakerRequest_session = this.bakerRequest_connection.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE);
		this.bakerRequest_sender = this.bakerRequest_session.createSender(this.bakerRequest_queue);
		this.bakerRequest_connection.start();
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

		this.logger.info("Queue for oven created and connection started.", (Object[]) null);
	}

	public void run() {
		try {
			// On startup, check if something for this baker is on server
			// (ingredients or baked charge)
			Hashtable<String, String> properties = new Hashtable<String, String>();
			properties.put("BAKER_ID", String.valueOf(this.id));
			this.logger.info("Starting request, if something for me is already on the server...", (Object[]) null);
			Message responseMessage = JMSUtils.sendMessage(MessageType.TEXTMESSAGE, Messages.BAKER_GENERAL_REQUEST_MESSAGE, properties, this.bakerRequest_session, true, this.bakerRequest_sender);

			this.checkReponseMessage(responseMessage);
		} catch (JMSException e) {
			e.printStackTrace();
		}

		while (isRunning) {
			try {
				Hashtable<String, String> properties = new Hashtable<String, String>();
				properties.put("BAKER_ID", String.valueOf(this.id));
				this.logger.info("Waiting for response for new ingredients and blocking..", (Object[]) null);
				Message responseMessage = JMSUtils.sendMessage(MessageType.TEXTMESSAGE, Messages.INGREDIENTS_REQUEST_MESSAGE, properties, this.bakerIngredients_session, true,
						this.bakerIngredients_sender);
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
		this.logger.info("BakerInstance shutting down.", (Object[]) null);

		this.logger.info("Closing queue for baker request queue", (Object[]) null);
		this.bakerRequest_sender.close();
		this.bakerRequest_session.close();
		this.bakerRequest_connection.close();

		this.monitoringSender.closeConnection();
	}

	private void checkReponseMessage(Message message) {
		this.logger.info("Response received.", (Object[]) null);
		if (message instanceof ObjectMessage) {
			ObjectMessage objMessage = (ObjectMessage) message;
			try {
				if (objMessage.getStringProperty("TYPE") != null && objMessage.getStringProperty("TYPE").equals("ArrayList<GingerBreadTransactionObject>")) {

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

						tmp.setFirstEggSupplierId(obj.getEgg1().getSupplierId());
						tmp.setSecondEggSupplierId(obj.getEgg2().getSupplierId());
						tmp.setHoneySupplierId(obj.getHoney().getSupplierId());
						tmp.setFlourSupplierId(obj.getFlour().getSupplierId());

						tmp.setState(GingerBread.State.PRODUCED);
						this.charge.add(tmp);
						Thread.sleep(Utils.getRandomWaitTime());
					}
					Hashtable<String, String> properties1 = new Hashtable<String, String>();
					properties1.put("BAKER_ID", String.valueOf(this.id));
					this.monitoringSender.sendMonitoringMessage(this.charge, properties1);
					this.producedCharges.put(chargeId, this.charge.size());
					// acknowledge for receiving ingredients
					message.acknowledge();

					// send to oven
					Message responseMessage = this.sendToOven();

					if (responseMessage instanceof ObjectMessage) {
						ObjectMessage responseObjectMessage = (ObjectMessage) responseMessage;
						if (responseObjectMessage.getStringProperty("TYPE") != null && responseObjectMessage.getStringProperty("TYPE").equals("ArrayList<GingerBread>")) {
							// acknowledge for receiving baked charge
							@SuppressWarnings("unchecked")
							ArrayList<GingerBread> bakedCharge = (ArrayList<GingerBread>) responseObjectMessage.getObject();

							// send to qualitycontrol
							this.sendToQualitControl(bakedCharge);
						}
					}
				}
				// received stored oven gingerbreads
				else if (objMessage.getStringProperty("TYPE") != null && objMessage.getStringProperty("TYPE").equals("ArrayList<GingerBread>")) {
					if (objMessage.getStringProperty("STATE") != null && objMessage.getStringProperty("STATE").equals("BAKED")) {
						@SuppressWarnings("unchecked")
						ArrayList<GingerBread> bakedCharge = (ArrayList<GingerBread>) objMessage.getObject();
						// send to qualitycontrol
						this.sendToQualitControl(bakedCharge);
					} else if (objMessage.getStringProperty("STATE") != null && objMessage.getStringProperty("STATE").equals("PRODUCED")) {
						// send to oven
						Message responseMessage = this.sendToOven();
						if (responseMessage instanceof ObjectMessage) {
							ObjectMessage responseObjectMessage = (ObjectMessage) responseMessage;
							if (responseObjectMessage.getStringProperty("TYPE") != null && responseObjectMessage.getStringProperty("TYPE").equals("ArrayList<GingerBread>")) {
								// acknowledge for receiving baked charge
								@SuppressWarnings("unchecked")
								ArrayList<GingerBread> bakedCharge = (ArrayList<GingerBread>) responseObjectMessage.getObject();

								// send to qualitycontrol
								this.sendToQualitControl(bakedCharge);
							}
						}						
					}
				}

			} catch (JMSException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else if (message instanceof TextMessage) {
			try {
				TextMessage textMessage = (TextMessage) message;
				if (textMessage.getText() != null && textMessage.getText().equals(Messages.NO_STORED_DATA)) {
					logger.info("No stored data.", (Object[]) null);
				}
				message.acknowledge();
			} catch (JMSException e) {
				e.printStackTrace();
			}
		}
	}

	private void sendToQualitControl(ArrayList<GingerBread> bakedCharge) {
		try {

			// forward to qualitycontrol
			Hashtable<String, String> properties = new Hashtable<String, String>();
			properties.put("TYPE", "ArrayList<GingerBread>");
			JMSUtils.sendMessage(MessageType.OBJECTMESSAGE, bakedCharge, properties, this.qualityQueue_session, false, this.qualityQueue_sender);
			this.logger.info("Received baked charge of size= " + bakedCharge.size() + " and forwarded to quality control.", (Object[]) null);

			// inform server that the stored baked gingerbreads can now be
			// removed
			properties = new Hashtable<String, String>();
			properties.put("BAKER_ID", String.valueOf(this.id));
			JMSUtils.sendMessage(MessageType.TEXTMESSAGE, Messages.SERVER_REMOVE_STORED_BAKED_GINGERBREADS, properties, this.bakerRequest_session, false, this.bakerRequest_sender);

		} catch (JMSException e) {
			e.printStackTrace();
		}
		this.reset();
	}

	private Message sendToOven() {
		try {
			Hashtable<String, String> properties = new Hashtable<String, String>();
			properties.put("BAKER_ID", String.valueOf(this.id));
			properties.put("TYPE", "ArrayList<GingerBread>");
			this.logger.info("Waiting for oven and blocking..", (Object[]) null);
			Message responseMessage = JMSUtils.sendMessage(MessageType.OBJECTMESSAGE, this.charge, properties, this.ovenQueue_session, true, this.ovenQueue_sender);
			responseMessage.acknowledge();
			return responseMessage;
		} catch (JMSException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void reset() {
		// Ready with producing, reset gingerBreadList for next request
		this.gingerBreadList.clear();
		// Reset charge for next request
		this.charge.clear();
	}
}