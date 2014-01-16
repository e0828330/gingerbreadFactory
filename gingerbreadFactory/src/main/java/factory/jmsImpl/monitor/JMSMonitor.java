package factory.jmsImpl.monitor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
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

import factory.entities.GingerBread;
import factory.entities.Ingredient;
import factory.entities.Order;
import factory.interfaces.EventListener;
import factory.interfaces.Monitor;
import factory.utils.JMSUtils;
import factory.utils.JMSUtils.MessageType;
import factory.utils.Messages;

public class JMSMonitor implements Monitor, MessageListener {

	private Logger logger = Logger.get(getClass());

	private EventListener eventListener;

	private Context ctx;

	// command queue
	// Sends commands to the server
	private QueueConnection command_connection;
	private QueueSession command_session;
	private Queue command_queue;
	private QueueSender command_sender;

	// event queue (sending events to gui)
	private QueueConnection eventQueue_connection;
	private QueueSession eventQueue_session;
	private Queue eventQueue_queue;
	private QueueReceiver eventQueue_receiver;

	public JMSMonitor() {
		try {
			Properties properties = new Properties();
			properties.load(this.getClass().getClassLoader().getResourceAsStream("jms.properties"));
			this.ctx = new InitialContext(properties);

			this.setup_commandQueue();
			this.setup_eventQueue();
		} catch (NamingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	private void setup_commandQueue() throws NamingException, JMSException {
		QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory) ctx.lookup("qpidConnectionfactory");
		this.command_queue = (Queue) ctx.lookup("commandQueue");
		this.command_connection = queueConnectionFactory.createQueueConnection();
		this.command_session = this.command_connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
		this.command_sender = this.command_session.createSender(this.command_queue);
		this.command_connection.start();
	}

	private void setup_eventQueue() throws NamingException, JMSException {
		QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory) ctx.lookup("qpidConnectionfactory");
		this.eventQueue_queue = (Queue) ctx.lookup("eventQueue");
		this.eventQueue_connection = queueConnectionFactory.createQueueConnection();
		this.eventQueue_session = this.eventQueue_connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
		this.eventQueue_receiver = this.eventQueue_session.createReceiver(this.eventQueue_queue);
		this.eventQueue_receiver.setMessageListener(this);
		this.eventQueue_connection.start();
	}

	public void setListener(EventListener listener) {
		this.eventListener = listener;
	}

	@SuppressWarnings("unchecked")
	public List<GingerBread> getGingerBreads() {
		List<GingerBread> result = new ArrayList<GingerBread>();
		try {
			Message response = JMSUtils.sendMessage(MessageType.TEXTMESSAGE, Messages.GET_GINGERBREADS, null, this.command_session, true, this.command_sender);
			if (response instanceof ObjectMessage) {
				ObjectMessage objMessage = (ObjectMessage) response;
				if (response.getStringProperty("TYPE") != null && response.getStringProperty("TYPE").equals("ArrayList<GingerBread>")) {
					result = (ArrayList<GingerBread>) objMessage.getObject();
				}
			}
		} catch (JMSException e) {
			System.err.println("GETGINGERBREADS");
			e.printStackTrace();
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public List<Ingredient> getIngredients() {
		List<Ingredient> result = new ArrayList<Ingredient>();
		try {
			Message response = JMSUtils.sendMessage(MessageType.TEXTMESSAGE, Messages.GET_INGREDIENTS, null, this.command_session, true, this.command_sender);
			if (response instanceof ObjectMessage) {
				ObjectMessage objMessage = (ObjectMessage) response;
				if (response.getStringProperty("TYPE") != null && response.getStringProperty("TYPE").equals("ArrayList<Ingredient>")) {
					result = (ArrayList<Ingredient>) objMessage.getObject();
				}
			}
		} catch (JMSException e) {
			System.err.println("GETINGREDEINTS");
			e.printStackTrace();
		}
		return result;
	}

	private void close() throws JMSException {
		this.logger.info("Closing command queue.", (Object[]) null);
		this.command_sender.close();
		this.command_session.close();
		this.command_connection.close();

		this.logger.info("Closing event queue.", (Object[]) null);
		this.eventQueue_receiver.close();
		this.eventQueue_session.close();
		this.eventQueue_connection.close();
	}

	@SuppressWarnings("unchecked")
	public List<GingerBread> getOvenContent() {
		List<GingerBread> result = new ArrayList<GingerBread>();
		try {
			Message response = JMSUtils.sendMessage(MessageType.TEXTMESSAGE, Messages.GET_OVEN, null, this.command_session, true, this.command_sender);
			if (response instanceof ObjectMessage) {
				ObjectMessage objMessage = (ObjectMessage) response;
				if (response.getStringProperty("TYPE") != null && response.getStringProperty("TYPE").equals("ArrayList<GingerBread>")) {
					result = (ArrayList<GingerBread>) objMessage.getObject();
				}
			}
		} catch (JMSException e) {
			System.err.println("GETOVENCONTENT");
			e.printStackTrace();
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public void onMessage(Message message) {
		try {
			if (message instanceof ObjectMessage) {
				ObjectMessage objectMessage = (ObjectMessage) message;
				if (objectMessage.getStringProperty("EVENT") != null && objectMessage.getStringProperty("EVENT").equals(Messages.EVENT_GINGERBREAD_STATE_CHANGED)) {
					ArrayList<GingerBread> result = (ArrayList<GingerBread>) objectMessage.getObject();
					if (result != null)	this.eventListener.onGingerBreadStateChange(result);
				} else if (objectMessage.getStringProperty("EVENT") != null && objectMessage.getStringProperty("EVENT").equals(Messages.EVENT_NEW_INGREDIENTS)) {
					ArrayList<Ingredient> result = (ArrayList<Ingredient>) objectMessage.getObject();
					if (result != null)	this.eventListener.onIngredientChanged(result);
				} else if (objectMessage.getStringProperty("EVENT") != null && objectMessage.getStringProperty("EVENT").equals(Messages.EVENT_ORDERLIST_CHANGED)) {
					ArrayList<Order> result = (ArrayList<Order>) objectMessage.getObject();
					if (result != null)	this.eventListener.onOrderChanged(result);
				} else if (objectMessage.getStringProperty("EVENT") != null && objectMessage.getStringProperty("EVENT").equals(Messages.EVENT_NEW_OVENT_CHARGE)) {
						ArrayList<GingerBread> result = (ArrayList<GingerBread>) objectMessage.getObject();
						if (result != null)	this.eventListener.onOvenChanged(result);
				}
			}
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public List<Order> getOrders() {
		List<Order> result = new ArrayList<Order>();
		try {
			Message response = JMSUtils.sendMessage(MessageType.TEXTMESSAGE, Messages.GET_ORDERS, null, this.command_session, true, this.command_sender);
			if (response instanceof ObjectMessage) {
				ObjectMessage objMessage = (ObjectMessage) response;
				if (response.getStringProperty("TYPE") != null && response.getStringProperty("TYPE").equals("ArrayList<Order>")) {
					result = (ArrayList<Order>) objMessage.getObject();
				}
			}
		} catch (JMSException e) {
			System.err.println("GET_ORDER_EVENT");
			e.printStackTrace();
		}
		return result;
	}

}
