package factory.jmsImpl.loadBalancer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

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
import javax.print.attribute.standard.JobMediaSheetsCompleted;

import org.apache.qpid.transport.util.Logger;

import factory.entities.JMSFactoryConnection;
import factory.entities.Order;
import factory.utils.JMSUtils;
import factory.utils.Messages;
import factory.utils.JMSUtils.MessageType;

public class LoadBalancerInstance implements Runnable, MessageListener {

	private ArrayList<Integer> factories;

	private final String PROPERTIES_FILE = "jms.properties";


	private Context ctx;
	private Logger logger = Logger.get(getClass());
	
	private ArrayList<JMSFactoryConnection> factoryConnectionList;
	
	// queue for loadBalancer
	private QueueConnection lb_connection;
	private QueueSession lb_session;
	private Queue lb_queue;
	private QueueReceiver lb_receiver;
	
	private ConcurrentLinkedQueue<Order> orders;
	

	public LoadBalancerInstance(ArrayList<Integer> factories) {
		this.factories = factories;
		try {
			Properties properties = new Properties();
			properties.load(this.getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE));
			this.factoryConnectionList = new ArrayList<JMSFactoryConnection>(this.factories.size());
			
			for (Integer factoryID : factories) {
				JMSUtils.extendJMSProperties(properties, factoryID);
			}
			this.ctx = new InitialContext(properties);
			
			// create queues for all factories for communication for orders
			this.createQueues();
			
			// init orderlist
			this.orders = new ConcurrentLinkedQueue<Order>();
			
			// init lb queue
			QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory) ctx.lookup("qpidConnectionfactory");
			this.lb_queue = (Queue) ctx.lookup("loadBalancerQueue");
			this.lb_connection = queueConnectionFactory.createQueueConnection();
			this.lb_session = this.lb_connection.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE);
			this.lb_receiver = this.lb_session.createReceiver(this.lb_queue);
			this.lb_receiver.setMessageListener(this);
			this.lb_connection.start();

		} catch (NamingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}
	
	private void createQueues() throws JMSException, NamingException {
		QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory) ctx.lookup("qpidConnectionfactory");
		for (Integer fid : this.factories) {
			this.logger.info("Create queue for id = " + fid, (Object[]) null);
			Queue order_queue = (Queue) ctx.lookup("orderQueue" + String.valueOf(fid));
			QueueConnection order_connection = queueConnectionFactory.createQueueConnection();
			QueueSession order_session = order_connection.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE);
			QueueSender order_sender = order_session.createSender(order_queue);
			QueueReceiver order_receiver = order_session.createReceiver(order_queue);
			order_receiver.setMessageListener(this);
			order_connection.start();
			
			JMSFactoryConnection fc = new JMSFactoryConnection();
			fc.setId(fid);
			fc.setConnection(order_connection);
			fc.setSession(order_session);
			fc.setReceiver(order_receiver);
			fc.setSender(order_sender);
			fc.setQueue(order_queue);
			
			this.factoryConnectionList.add(fc);
		}
	}

	public void run() {
		BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
		String s;
		try {
			s = bufferRead.readLine();

			if (s.equalsIgnoreCase("exit")) { 
				this.close();
			}
		} catch (IOException e) {
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

	public void onMessage(Message message) {
		this.logger.info("Message received in lb queue.", (Object[]) null);
		try {
			message.acknowledge();
			if (message instanceof ObjectMessage) {
				ObjectMessage objectMessage = (ObjectMessage) message;
				if (objectMessage.getStringProperty("TYPE").equalsIgnoreCase("Order")) {
					Order order = (Order) objectMessage.getObject();
					this.orders.add(order);
					this.logger.info("Got order with ID =  " + order.getId(), (Object[]) null);
					
					// Send requests for this order to factories
					Hashtable<String, String> properties = new Hashtable<String, String>(1);
					
					for (JMSFactoryConnection fc : this.factoryConnectionList) {
						properties.put("ORDER_ID", String.valueOf(order.getId()));
						JMSUtils.sendMessage(MessageType.TEXTMESSAGE, 
								Messages.LOAD_REQUEST, 
								properties, 
								fc.getSession(), 
								false, 
								fc.getSender());
					}
					
				}
			}
		} catch ( JMSException e) {
			e.printStackTrace();
		}
		
	}
	
	
	private void close() throws JMSException {
		for (JMSFactoryConnection fc : this.factoryConnectionList) {
			fc.getSender().close();
			fc.getReceiver().close();
			fc.getSession().close();
			fc.getConnection().close();
		}
		
		this.lb_receiver.close();
		this.lb_session.close();
		this.lb_connection.close();
	}

}
