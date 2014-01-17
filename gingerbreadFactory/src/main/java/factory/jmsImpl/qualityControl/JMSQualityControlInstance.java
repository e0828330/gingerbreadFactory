package factory.jmsImpl.qualityControl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

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
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.qpid.transport.util.Logger;

import factory.entities.GingerBread;
import factory.entities.GingerBread.State;
import factory.utils.JMSMonitoringSender;
import factory.utils.JMSUtils;
import factory.utils.JMSUtils.MessageType;
import factory.utils.Messages;

public class JMSQualityControlInstance implements Runnable {

	private final String PROPERTIES_FILE = "jms.properties";

	private Context ctx;
	private boolean isRunning = true;
	private Logger logger = Logger.get(getClass());

	private float defectRate = 0.4f;
	private Long id = 1L; // TODO: Set at startup

	// QualityControlQueue Baker -> QualityControl
	private QueueConnection qualityQueue_connection;
	private QueueSession qualityQueue_session;
	private MessageConsumer qualityQueue_consumer;
	private Queue qualityQueue_queue;

	// QualityControlQueue QualityControl -> Logistics
	private QueueConnection logisticsQueue_connection;
	private QueueSession logisticsQueue_session;
	private Queue logisticsQueue_queue;
	private QueueSender logisticsQueue_sender;

	// For monitoring
	private JMSMonitoringSender monitoringSender;

	private boolean needsCheck = true;
	
	private int factoryID;

	public JMSQualityControlInstance(Long id, float defectRate, int factoryID) throws IOException, NamingException, JMSException {
		this.logger.info("Starting for factory with id = " + this.factoryID, (Object[]) null);
		this.factoryID = factoryID;
		Properties properties = new Properties();
		properties.load(this.getClass().getClassLoader().getResourceAsStream(this.PROPERTIES_FILE));
		JMSUtils.extendJMSProperties(properties, this.factoryID);
		this.ctx = new InitialContext(properties);

		this.monitoringSender = new JMSMonitoringSender(this.ctx, this.factoryID);

		this.id = id;
		this.defectRate = defectRate;

		this.setup_qualityControlQueue();

		this.setup_logisticsQueue();
	}

	private void setup_logisticsQueue() throws NamingException, JMSException {
		this.logger.info("Initializing queue for logistics...", (Object[]) null);
		QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory) ctx.lookup("qpidConnectionfactory");
		this.logisticsQueue_queue = (Queue) ctx.lookup("logisticsQueue" + this.factoryID);
		this.logisticsQueue_connection = queueConnectionFactory.createQueueConnection();
		this.logisticsQueue_session = this.logisticsQueue_connection.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE);
		this.logisticsQueue_sender = this.logisticsQueue_session.createSender(this.logisticsQueue_queue);
		this.logisticsQueue_connection.start();
		this.logger.info("Queue for quality-control startet.", (Object[]) null);
	}

	private void setup_qualityControlQueue() throws NamingException, JMSException {
		this.logger.info("Initializing queue for bakers quality-control requests...", (Object[]) null);
		QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory) ctx.lookup("qpidConnectionfactory");
		this.qualityQueue_queue = (Queue) ctx.lookup("qualityControlQueue" + this.factoryID);
		this.qualityQueue_connection = queueConnectionFactory.createQueueConnection();
		this.qualityQueue_session = this.qualityQueue_connection.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE);
		this.qualityQueue_consumer = this.qualityQueue_session.createConsumer(this.qualityQueue_queue);
		this.qualityQueue_connection.start();
		this.logger.info("Queue for quality-control startet.", (Object[]) null);
	}

	public void run() {
		try {
			while (isRunning) {
				// REMOVE AFTER DEBUGGING
				//this.forwardCharge(new ArrayList<GingerBread>(), false);
				Message message = this.qualityQueue_consumer.receive();
				this.logger.info("Received message...", (Object[]) null);
				boolean isGarbage = false;
				if (message instanceof ObjectMessage) {
					ObjectMessage objectMessage = (ObjectMessage) message;
					if (objectMessage.getStringProperty("TYPE") != null && objectMessage.getStringProperty("TYPE").equals("ArrayList<GingerBread>")) {
						@SuppressWarnings("unchecked")
						ArrayList<GingerBread> testList = (ArrayList<GingerBread>) objectMessage.getObject();

						// We don't need to check this round
						if (this.needsCheck == false) {
							this.logger.info("This charge is just forwarded...", (Object[]) null);
							for (GingerBread tested : testList) {
								tested.setState(State.CONTROLLED);
								tested.setQaId(this.id);
							}
							this.forwardCharge(testList, isGarbage);
							needsCheck = !needsCheck;
							continue;
						}

						// We need to check this round

						// Shuffle to have a random selection for testing
						if (!JMSUtils.BENCHMARK) {
							Collections.shuffle(testList);
						}

						// Eat he yummmy yummmy gingerbread
						if (Math.random() < defectRate) {
							this.logger.info("Whole charge is garbage because of sucky tasting gingerbread.", (Object[]) null);
							for (GingerBread tested : testList) {
								tested.setState(State.GARBAGE);
								tested.setQaId(this.id);
								isGarbage = true;
							}
						} else {
							this.logger.info("This charge is fine.", (Object[]) null);
							for (GingerBread tested : testList) {
								tested.setState(State.CONTROLLED);
								tested.setQaId(this.id);
							}
						}

						// testList.remove(0); // remove the eaten one
						testList.get(0).setState(State.EATEN);
						this.forwardCharge(testList, isGarbage);
						// Toggle needs check state
						needsCheck = !needsCheck;
					}
				}
				message.acknowledge();
			}
		} catch (JMSException e) {
			e.printStackTrace();
		}

		try {
			this.close();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	public void close() throws JMSException {
		this.monitoringSender.closeConnection();

		this.logger.info("Closing quality-control queue.", (Object[]) null);
		this.qualityQueue_consumer.close();
		this.qualityQueue_session.close();
		this.qualityQueue_connection.close();

		this.logger.info("Closing logistics queue.", (Object[]) null);
		this.logisticsQueue_sender.close();
		this.logisticsQueue_session.close();
		this.logisticsQueue_connection.close();
	}

	public void shutDown() {
		this.isRunning = false;
	}

	private void forwardCharge(ArrayList<GingerBread> charge, boolean isGarbage) {
		try {
			this.logger.info("Send charge to server for monitoring.", (Object[]) null);
			for (GingerBread gingerBread : charge) {
				this.monitoringSender.sendMonitoringMessage(gingerBread);
			}
			// forward to logistic
			if (isGarbage == false) {
				this.logger.info("Send info to logistic", (Object[]) null);
				//for (GingerBread gingerBread : charge) {
					JMSUtils.sendMessage(MessageType.TEXTMESSAGE, Messages.NEW_CONTROLLED_GINGERBREAD, null, this.logisticsQueue_session, false, this.logisticsQueue_sender);
				//}
			}
		} catch (JMSException e) {
			e.printStackTrace();
		} catch (NamingException e) {
			e.printStackTrace();
		}
	}

}
