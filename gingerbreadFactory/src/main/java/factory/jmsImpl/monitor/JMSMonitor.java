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
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import factory.entities.GingerBread;
import factory.entities.Ingredient;
import factory.interfaces.EventListener;
import factory.interfaces.Monitor;
import factory.utils.JMSUtils;
import factory.utils.Messages;
import factory.utils.JMSUtils.MessageType;

public class JMSMonitor implements Monitor, MessageListener {

	EventListener eventListener;

	private Context ctx;

	// command queue
	// Sends commands to the server
	private QueueConnection command_connection;
	private QueueSession command_session;
	private Queue command_queue;
	private QueueSender command_sender;

	public JMSMonitor() {
		try {
			Properties properties = new Properties();
			properties.load(this.getClass().getClassLoader().getResourceAsStream("jms.properties"));
			this.ctx = new InitialContext(properties);
			QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory) ctx.lookup("qpidConnectionfactory");
			this.command_queue = (Queue) ctx.lookup("commandQueue");
			this.command_connection = queueConnectionFactory.createQueueConnection();
			this.command_session = this.command_connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			this.command_sender = this.command_session.createSender(this.command_queue);
			this.command_connection.start();
		} catch (NamingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	public void setListener(EventListener listener) {
		this.eventListener = listener;
	}

	@SuppressWarnings("unchecked")
	public List<GingerBread> getGingerBreads() {
		List<GingerBread> result = new ArrayList<GingerBread>();
		try {
			Message response = JMSUtils.sendMessage(MessageType.TEXTMESSAGE, 
				Messages.GET_GINGERBREADS, 
				null, 
				this.command_session, 
				true, 
				this.command_sender);
			if (response instanceof ObjectMessage) {
				ObjectMessage objMessage = (ObjectMessage) response;
				if (response.getStringProperty("TYPE") != null && response.getStringProperty("TYPE").equals("ArrayList<GingerBread>")) {
					result = (ArrayList<GingerBread>) objMessage.getObject();
				}
			}
		}
		catch (JMSException e) {
			e.printStackTrace();
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public List<Ingredient> getIngredients() {
		List<Ingredient> result = new ArrayList<Ingredient>();
		try {
			Message response = JMSUtils.sendMessage(MessageType.TEXTMESSAGE, 
				Messages.GET_INGREDIENTS, 
				null, 
				this.command_session, 
				true, 
				this.command_sender);
			if (response instanceof ObjectMessage) {
				ObjectMessage objMessage = (ObjectMessage) response;
				if (response.getStringProperty("TYPE") != null && response.getStringProperty("TYPE").equals("ArrayList<Ingredient>")) {
					result = (ArrayList<Ingredient>) objMessage.getObject();
				}
			}
		}
		catch (JMSException e) {
			e.printStackTrace();
		}
		return result;
	}

	public List<GingerBread> getOvenContent() {
		List<GingerBread> result = new ArrayList<GingerBread>();
		try {
			Message response = JMSUtils.sendMessage(MessageType.TEXTMESSAGE, 
				Messages.GET_OVEN, 
				null, 
				this.command_session, 
				true, 
				this.command_sender);
			if (response instanceof ObjectMessage) {
				ObjectMessage objMessage = (ObjectMessage) response;
				if (response.getStringProperty("TYPE") != null && response.getStringProperty("TYPE").equals("ArrayList<GingerBread>")) {
					result = (ArrayList<GingerBread>) objMessage.getObject();
				}
			}
		}
		catch (JMSException e) {
			e.printStackTrace();
		}
		return result;
	}

	public void onMessage(Message message) {
	
	}

}
