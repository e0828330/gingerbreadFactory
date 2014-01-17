package factory.jmsImpl.loadBalancer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
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
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.print.attribute.standard.JobMediaSheetsCompleted;

import org.apache.qpid.transport.util.Logger;

import factory.entities.JMSFactoryConnection;
import factory.entities.LoadEntity;
import factory.entities.Order;
import factory.utils.JMSUtils;
import factory.utils.Messages;
import factory.utils.JMSUtils.MessageType;

public class LoadBalancerInstance implements Runnable, MessageListener {

	private ArrayList<Integer> factories;

	private final String PROPERTIES_FILE = "jms.properties";

	private boolean isOnlyOneFactory = false;

	private Context ctx;
	private Logger logger = Logger.get(getClass());
	
	private ConcurrentHashMap<Integer, JMSFactoryConnection> factoryConnectionList;
	
	// queue for loadBalancer
	private QueueConnection lb_connection;
	private QueueSession lb_session;
	private Queue lb_queue;
	private QueueReceiver lb_receiver;
	
	private ConcurrentHashMap<Long, Order> orders;
	
	// stores the responses for the load requests
	private ConcurrentHashMap<Long, Integer> responseCounter;
	
	private ConcurrentHashMap<Long, ArrayList<LoadEntity>> loads;
	

	public LoadBalancerInstance(ArrayList<Integer> factories) {
		this.factories = factories;
		this.isOnlyOneFactory = (this.factories.size() == 1);
		try {
			Properties properties = new Properties();
			properties.load(this.getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE));
			this.factoryConnectionList = new ConcurrentHashMap<Integer, JMSFactoryConnection>(this.factories.size());
			
			for (Integer factoryID : factories) {
				JMSUtils.extendJMSProperties(properties, factoryID);
			}
			this.ctx = new InitialContext(properties);
			
			// create queues for all factories for communication for orders
			this.createQueues();
			
			// init orderlist
			this.orders = new ConcurrentHashMap<Long, Order>();
			
			this.responseCounter = new ConcurrentHashMap<Long, Integer>();
			
			this.loads = new ConcurrentHashMap<Long, ArrayList<LoadEntity>>();
			
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
			order_connection.start();
			
			JMSFactoryConnection fc = new JMSFactoryConnection();
			fc.setId(fid);
			fc.setConnection(order_connection);
			fc.setSession(order_session);
			fc.setSender(order_sender);
			fc.setQueue(order_queue);
			
			this.factoryConnectionList.put(fid, fc);
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
					// if we have only 1 factory:
					if (isOnlyOneFactory) {
						Hashtable<String, String> properties = new Hashtable<String, String>(1);
						properties.put("TYPE", "Order");
						JMSUtils.sendMessage(MessageType.OBJECTMESSAGE, 
								order, 
								properties, 
								this.factoryConnectionList.get(order.getFactoryId()).getSession(), 
								false, 
								this.factoryConnectionList.get(order.getFactoryId()).getSender());
						return;
					}
					
					
					
					this.orders.put(order.getId(), order);
					this.responseCounter.put(order.getId(), 0);
					this.loads.put(order.getId(), new ArrayList<LoadEntity>(this.factories.size()));
	
					this.logger.info("Got order with ID =  " + order.getId(), (Object[]) null);
					
					// Send requests for this order to factories
					Hashtable<String, String> properties = new Hashtable<String, String>(1);
					
					for (JMSFactoryConnection fc : this.factoryConnectionList.values()) {
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
			else if (message instanceof TextMessage) {
				TextMessage textMessage = (TextMessage) message;
				if (textMessage.getText() != null && textMessage.getText().equals(Messages.LOAD)) {
					Long orderID = Long.valueOf(textMessage.getStringProperty("ORDER_ID"));
					int load = Integer.parseInt(textMessage.getStringProperty("LOAD"));
					int factoryID = Integer.parseInt(textMessage.getStringProperty("FACTORY_ID")); 
					this.logger.info("Received LOAD-Message for order = " + orderID + " with load of = " + load
							+ "from factory " + factoryID);
				
					// update counter for response and add load
					LoadEntity loadEntity = new LoadEntity();
					loadEntity.setFactoryID(factoryID);
					loadEntity.setLoad(load);
					loadEntity.setOrderID(orderID);
					
					this.loads.get(orderID).add(loadEntity);
									
					this.responseCounter.put(orderID, this.responseCounter.get(orderID) + 1);
					
					// ok we received all answers, lets calculate the avg load
					int factorySize = this.factories.size();
					if (this.responseCounter.get(orderID) == factorySize) {
						this.logger.info("Received all answers for order = " + orderID);
						int totalLoad = 0;
						int smallestLoad = Integer.MAX_VALUE;
						int alternativeReceiverID = 0;
						int defaultReceiverLoad = 0;
						ArrayList<LoadEntity> tmp = this.loads.get(orderID);
						for (LoadEntity entity : tmp) {
							// add to total load
							totalLoad += entity.getLoad();
							// save the factory with smalles load
							if (entity.getLoad() < smallestLoad) {
								smallestLoad = entity.getLoad();
								alternativeReceiverID = entity.getFactoryID();
							}
							// store the load of the default receiver
							if (entity.getFactoryID() == this.orders.get(orderID).getFactoryId()) {
								defaultReceiverLoad = entity.getLoad();
							}
						}
						
						
						double avg = totalLoad / factorySize;
						this.logger.info("defaultload < avg * 1.25 === " + defaultReceiverLoad + " < " + avg + " * 1.25 (" + avg * 1.25 + ")");
						if (defaultReceiverLoad < avg * 1.25 || totalLoad == 0) {
							// send to default factory
							this.logger.info("Sending to default factory with id = " + this.orders.get(orderID).getFactoryId());
							Hashtable<String, String> properties = new Hashtable<String, String>(1);
							properties.put("TYPE", "Order");
							Order order = this.orders.remove(orderID);
							JMSUtils.sendMessage(MessageType.OBJECTMESSAGE, 
									order, 
									properties, 
									this.factoryConnectionList.get(order.getFactoryId()).getSession(), 
									false, 
									this.factoryConnectionList.get(order.getFactoryId()).getSender());
						}
						else {
							this.logger.info("Switching order to other factory...");
							this.logger.info("Sending to other factory with id = " + alternativeReceiverID + 
									" instead of default " + this.orders.get(orderID).getFactoryId());
							//  send to min packages
							Hashtable<String, String> properties = new Hashtable<String, String>(1);
							properties.put("TYPE", "Order");
							Order order = this.orders.remove(orderID);
							JMSUtils.sendMessage(MessageType.OBJECTMESSAGE, 
									order, 
									properties, 
									this.factoryConnectionList.get(alternativeReceiverID).getSession(), 
									false, 
									this.factoryConnectionList.get(alternativeReceiverID).getSender());
							
							// send moved to default
							order.setState(Order.State.MOVED);
							JMSUtils.sendMessage(MessageType.OBJECTMESSAGE, 
									order, 
									properties, 
									this.factoryConnectionList.get(order.getFactoryId()).getSession(), 
									false, 
									this.factoryConnectionList.get(order.getFactoryId()).getSender());
						}
						
					}
					
				}
			}
		} catch ( JMSException e) {
			e.printStackTrace();
		}
		
	}
	
	
	private void close() throws JMSException {
		for (JMSFactoryConnection fc : this.factoryConnectionList.values()) {
			fc.getSender().close();
			fc.getSession().close();
			fc.getConnection().close();
		}
		
		this.lb_receiver.close();
		this.lb_session.close();
		this.lb_connection.close();
	}

}
