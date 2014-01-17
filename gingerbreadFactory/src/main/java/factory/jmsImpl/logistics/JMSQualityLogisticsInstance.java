package factory.jmsImpl.logistics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

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
import factory.entities.GingerBread.State;
import factory.entities.Order;
import factory.utils.JMSMonitoringSender;
import factory.utils.JMSUtils;
import factory.utils.JMSUtils.MessageType;
import factory.utils.Messages;
import factory.utils.Utils;

public class JMSQualityLogisticsInstance implements Runnable {

	private Context ctx;
	private boolean isRunning = true;
	private Logger logger = Logger.get(getClass());

	private Long id = 1L; // Set at startup

	//private Long packageId = 0L;
	
	private TreeSet<Order> orderList;

	// For monitoring
	private JMSMonitoringSender monitoringSender;

	// QualityControlQueue QualityControl -> Logistics
	private QueueConnection logisticsQueue_connection;
	private QueueSession logisticsQueue_session;
	private MessageConsumer logisticsQueue_consumer;
	private Queue logisticsQueue_queue;
	
	// Queue for packaging requests to server
	private QueueConnection packagingQueue_connection;
	private QueueSession packagingQueue_session;
	private Queue packagingQueue_queue;
	private QueueSender packagingQueue_sender;	

	//private final int MAX_PACKAGE_SIZE = 6;
	
	private final String PROPERTIES_FILE = "jms.properties";

	//private ArrayList<GingerBread> currentPackage = new ArrayList<GingerBread>(MAX_PACKAGE_SIZE);
	//private int counter = 0;
	
	private int factoryID;

	public JMSQualityLogisticsInstance(Long id, int factoryID) throws IOException, NamingException, JMSException {
		this.id = id;
		this.factoryID = factoryID;
		this.logger.info("Start for factory id = " + this.factoryID , (Object[]) null);
		Properties properties = new Properties();
		properties.load(this.getClass().getClassLoader().getResourceAsStream(this.PROPERTIES_FILE));
		JMSUtils.extendJMSProperties(properties, this.factoryID);
		this.ctx = new InitialContext(properties);
		
		this.monitoringSender = new JMSMonitoringSender(this.ctx, this.factoryID);
		
		this.setup_logisticsQueue();
		
		this.setup_packagingQueue();
		
		this.orderList = new TreeSet<Order>(new Comparator<Order>() {
			public int compare(Order a, Order b) {
				if (a.getState().equals(b.getState())) {
					return (int)(a.getTimestamp() - b.getTimestamp());
				}
				else if (a.getState().equals(Order.State.IN_PROGRESS)) {
					return 1;
				}
				else {
					return -1;
				}
			}
		});
	}

	private void setup_logisticsQueue() throws NamingException, JMSException {
		this.logger.info("Initializing queue for logistics...", (Object[]) null);
		QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory) ctx.lookup("qpidConnectionfactory");
		this.logisticsQueue_queue = (Queue) ctx.lookup("logisticsQueue" + this.factoryID);
		this.logisticsQueue_connection = queueConnectionFactory.createQueueConnection();
		this.logisticsQueue_session = this.logisticsQueue_connection.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE);
		this.logisticsQueue_consumer = this.logisticsQueue_session.createConsumer(this.logisticsQueue_queue);
		this.logisticsQueue_connection.start();
		this.logger.info("Queue for quality-control startet.", (Object[]) null);
	}
	
	private void setup_packagingQueue() throws NamingException, JMSException {
		QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory) ctx.lookup("qpidConnectionfactory");
		this.packagingQueue_queue = (Queue) this.ctx.lookup("packagingQueue" + this.factoryID);
		this.packagingQueue_connection = queueConnectionFactory.createQueueConnection();
		this.packagingQueue_session = this.packagingQueue_connection.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE);
		this.packagingQueue_sender = this.packagingQueue_session.createSender(this.packagingQueue_queue);
		this.packagingQueue_connection.start();
	}		

	@SuppressWarnings("unchecked")
	public void run() {
		try {

			Message response;
			ArrayList<GingerBread> responsePackage = new ArrayList<GingerBread>();

			while (isRunning) {
			
				Hashtable<String, String> properties = new Hashtable<String, String>(1);
				properties.put("REQTYPE", "GET_ORDERS");
				properties.put("LOGISTICS_ID", String.valueOf(this.id));
				this.logger.info("Waiting for order-list and blocking...", (Object[]) null);
				response = JMSUtils.sendMessage(MessageType.TEXTMESSAGE, 
						Messages.GET_ORDERS, 
						properties, 
						this.packagingQueue_session, 
						true, 
						this.packagingQueue_sender);
				if (response instanceof ObjectMessage) {
					ObjectMessage objMessage = (ObjectMessage) response;
					if (objMessage.getStringProperty("TYPE") != null && objMessage.getStringProperty("TYPE").equals("LinkedList<Order>")) {
						this.orderList.clear();
						for (Order order : (LinkedList<Order>) objMessage.getObject()) {
							this.orderList.add(order);
						}
					}
				}
				
				this.logger.info("There are " + this.orderList.size() + " orders.", (Object[]) null);
				
				LinkedList<GingerBread> packages = new LinkedList<GingerBread>();
				
				// orders
				boolean fallback = true;
				if (!JMSUtils.BENCHMARK) {
					for (Order order : this.orderList) {
						if (order.getDonePackages() == null) {
							order.setDonePackages(0);
						}
						for (int i = order.getDonePackages().intValue(); i < order.getPackages().intValue(); i++) {
							if ((responsePackage = this.checkResponse(this.requestForPackage(order.getNumNormal().intValue(), order.getNumChocolate().intValue(), order.getNumNut().intValue()))) != null) {
								fallback = false;
								order.setState(Order.State.IN_PROGRESS);
								this.buildPackage(responsePackage, packages, order);
					
								order.setDonePackages(order.getDonePackages() + 1);
								this.logger.info("Package " + order.getDonePackages() + " of " + order.getPackages() + " packed.", (Object[]) null);
							
								if (order.getDonePackages().equals(order.getPackages())) {
									this.logger.info("Finished order with id=" + order.getId(), (Object[]) null);
									order.setState(Order.State.DONE);
								}
							}
						}
					}
				}
				
				if (fallback) {
					// fallback
					if ((responsePackage = this.checkResponse(this.requestForPackage(2, 2, 2))) != null) {
						this.logger.info("Package with 2x normal, 2x chocolate, 2x nut.", (Object[]) null);
						this.buildPackage(responsePackage, packages, null);
					}
					else if ((responsePackage = this.checkResponse(this.requestForPackage(3, 3, 0))) != null) {
						this.logger.info("Package with 3x normal, 3x chocolate.", (Object[]) null);
						this.buildPackage(responsePackage, packages, null);
					}
					else if ((responsePackage = this.checkResponse(this.requestForPackage(0, 3, 3))) != null) {
						this.logger.info("Package with 3x chocolate, 3x nut.", (Object[]) null);
						this.buildPackage(responsePackage, packages, null);
					}
					else if ((responsePackage = this.checkResponse(this.requestForPackage(3, 0, 3))) != null) {
						this.logger.info("Package with 3x normal, 3x nut.", (Object[]) null);
						this.buildPackage(responsePackage, packages, null);
					}
					else if ((responsePackage = this.checkResponse(this.requestForPackage(6, 0, 0))) != null) {
						this.logger.info("Package with 6x normal.", (Object[]) null);
						this.buildPackage(responsePackage, packages, null);
					}
					else if ((responsePackage = this.checkResponse(this.requestForPackage(0, 6, 0))) != null) {
						this.logger.info("Package with 6x chocolate.", (Object[]) null);
						this.buildPackage(responsePackage, packages, null);
					}
					else if ((responsePackage = this.checkResponse(this.requestForPackage(0, 0, 6))) != null) {
						this.logger.info("Package with 6x nut.", (Object[]) null);
						this.buildPackage(responsePackage, packages, null);
					}
				}
				
				// send order back to server:
				this.logger.info("Send orderlist back to server...", (Object[]) null);
				properties = new Hashtable<String, String>(2);
				properties.put("TYPE", "ArrayList<Order>");
				ArrayList<Order> orderArrayList = new ArrayList<Order>(this.orderList.size());
				for (Order o : this.orderList) {
					orderArrayList.add(o);
				}
				JMSUtils.sendMessage(MessageType.OBJECTMESSAGE, 
						orderArrayList, 
						properties, 
						this.packagingQueue_session, 
						false, 
						this.packagingQueue_sender);
				
				
				// if no packages were build, because there are no offers or to less
				// ingredients ,just wait for new controlled gingerbreads and on receive
				// continue with whole procedure
				if (packages.size() == 0) {
					this.logger.info("Nothing to do... Waiting for new controlled gingerbreads and blocking...", (Object[]) null);
					Message message = this.logisticsQueue_consumer.receive();
					message.acknowledge();
	
					if (message instanceof TextMessage) {
						TextMessage textMessage = (TextMessage) message;
						if (textMessage.getText() != null && textMessage.getText().equals(Messages.NEW_CONTROLLED_GINGERBREAD) == false) {
							continue;							
						}
					}
				}
				else {
					// send to server and gui
					this.logger.info("Packages build.", (Object[]) null);
					
					Hashtable<String, String> propertiesM = new Hashtable<String, String>(1);
					propertiesM.put("NUMBER_OF_PACKAGES", String.valueOf(packages.size() / JMSUtils.PACKAGE_SIZE));
					this.monitoringSender.sendMonitoringMessage(new ArrayList<GingerBread>(packages), propertiesM);
					
					/*for (GingerBread b : packages) {
						this.monitoringSender.sendMonitoringMessage(b);
						
					}*/
				}	
				
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
	
	private void buildPackage(ArrayList<GingerBread> responsePackage, List<GingerBread> packages, Order order) {
		Long packageId = Utils.getID();
		for (GingerBread g : responsePackage) {
			g.setLogisticsId(this.id);
			g.setPackageId(packageId);
			g.setOrderId(order == null ? null : order.getId());
			g.setState(State.DONE);
			packages.add(g);
		}
	}
	
	private ArrayList<GingerBread> checkResponse(Message message) throws JMSException {
		if (message instanceof TextMessage || (message.getStringProperty("TYPE") == null) || message.getStringProperty("TYPE").equals("ArrayList<GingerBread>") == false) {
			return null;
		}
		if (message instanceof ObjectMessage) {
			ObjectMessage objMessage = (ObjectMessage) message;
			@SuppressWarnings("unchecked")
			ArrayList<GingerBread> responseList = (ArrayList<GingerBread>) objMessage.getObject();
			this.logger.info("Received package with " + responseList.size() + " gingerbreads.", (Object[]) null);

			if (responseList == null || responseList.size() == 0) {
				return null;
			}
			return responseList;
		}
		return null;
	}

	private Message requestForPackage(int normal, int chocolate, int nut) throws JMSException {
		this.logger.info("Request now at server side", (Object[]) null);
		Hashtable<String, String> properties = new Hashtable<String, String>(6);
		properties.put("LOGISTICS_ID", String.valueOf((this.id)));
		properties.put("REQTYPE", "GET_PACKAGE");
		properties.put(Messages.FLAVOR_NORMAL, String.valueOf(normal));
		properties.put(Messages.FLAVOR_CHOCOLATE,  String.valueOf(chocolate));
		properties.put(Messages.FLAVOR_NUT,  String.valueOf(nut));
		Message response = JMSUtils.sendMessage(MessageType.TEXTMESSAGE, 
				Messages.PACKAGE_ORDER, 
				properties, 
				this.packagingQueue_session, 
				true, 
				this.packagingQueue_sender);
		response.acknowledge();
		return response;
	}

	public void close() throws JMSException {
		this.logger.info("Closing quality-control queue.", (Object[]) null);
		this.monitoringSender.closeConnection();
		this.logisticsQueue_consumer.close();
		this.logisticsQueue_session.close();
		this.logisticsQueue_connection.close();
		
		this.logger.info("Closing packaging queue.", (Object[]) null);
		this.packagingQueue_sender.close();
		this.packagingQueue_session.close();
		this.packagingQueue_connection.close();			
	}

	public void shutDown() {
		this.isRunning = false;
	}
}
