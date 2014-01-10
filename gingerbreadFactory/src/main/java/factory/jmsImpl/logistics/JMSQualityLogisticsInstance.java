package factory.jmsImpl.logistics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
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
import javax.jms.QueueReceiver;
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
import factory.jmsImpl.server.JMSServerPackagingListener;
import factory.utils.JMSMonitoringSender;
import factory.utils.JMSUtils;
import factory.utils.JMSUtils.MessageType;
import factory.utils.Messages;
import factory.utils.Utils;

public class JMSQualityLogisticsInstance implements Runnable {

	private Context ctx;
	private boolean isRunning = true;
	private Logger logger = Logger.get(getClass());

	private Long id = 1L; // TODO: Set at startup

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

	private final int MAX_PACKAGE_SIZE = 6;
	
	private final String PROPERTIES_FILE = "jms.properties";

	private ArrayList<GingerBread> currentPackage = new ArrayList<GingerBread>(MAX_PACKAGE_SIZE);
	private int counter = 0;

	public JMSQualityLogisticsInstance(Long id) throws IOException, NamingException, JMSException {
		this.id = id;
		Properties properties = new Properties();
		properties.load(this.getClass().getClassLoader().getResourceAsStream(this.PROPERTIES_FILE));
		this.ctx = new InitialContext(properties);
		
		this.monitoringSender = new JMSMonitoringSender(this.ctx);
		
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
		this.logisticsQueue_queue = (Queue) ctx.lookup("logisticsQueue");
		this.logisticsQueue_connection = queueConnectionFactory.createQueueConnection();
		this.logisticsQueue_session = this.logisticsQueue_connection.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE);
		this.logisticsQueue_consumer = this.logisticsQueue_session.createConsumer(this.logisticsQueue_queue);
		this.logisticsQueue_connection.start();
		this.logger.info("Queue for quality-control startet.", (Object[]) null);
		
		//this.packageId = Utils.getID();
	}
	
	private void setup_packagingQueue() throws NamingException, JMSException {
		QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory) ctx.lookup("qpidConnectionfactory");
		this.packagingQueue_queue = (Queue) this.ctx.lookup("packagingQueue");
		this.packagingQueue_connection = queueConnectionFactory.createQueueConnection();
		this.packagingQueue_session = this.packagingQueue_connection.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE);
		this.packagingQueue_sender = this.packagingQueue_session.createSender(this.packagingQueue_queue);
		this.packagingQueue_connection.start();
	}		

	public void run() {
		try {
			
			// TestOrders harcoded
			
			Order order1 = new Order();
			order1.setId(Utils.getID());
			order1.setNumChocolate(2);
			order1.setNumNut(3);
			order1.setNumNormal(1);
			order1.setPackages(5); // Ich will 5 packate mit jeweils 2 schoko und 3 nuss und 1 normales
			order1.setTimestamp((new Date()).getTime());
			order1.setState(Order.State.OPEN);
			
			Order order2 = new Order();
			order2.setId(Utils.getID());
			order2.setNumChocolate(1);
			order2.setNumNut(1);
			order2.setNumNormal(4);
			order2.setPackages(2); // Ich will 5 packate mit jeweils 2 schoko und 3 nuss und 1 normales
			order2.setTimestamp((new Date()).getTime());
			order2.setState(Order.State.OPEN);
			
			this.orderList.add(order1);
			this.orderList.add(order2);
			
			Message response;
			ArrayList<GingerBread> responsePackage = new ArrayList<GingerBread>();
			boolean startup = true;
			
			while (isRunning) {
				
				if (!startup) {
					// if nothing is here for packing wait for receive
					Message message = this.logisticsQueue_consumer.receive();
					message.acknowledge();
	
					if (message instanceof TextMessage) {
						TextMessage textMessage = (TextMessage) message;
						if (textMessage.getText() != null && textMessage.getText().equals(Messages.NEW_CONTROLLED_GINGERBREAD) == false) {
							continue;							
						}
					}
				}
				startup = false;
				
				
				LinkedList<GingerBread> packages = new LinkedList<GingerBread>();
				
				// orders
				boolean fallback = true;
				for (Order order : this.orderList) {
					for (int i = 0; i < order.getPackages().intValue(); i++) {
						if ((responsePackage = this.checkResponse(this.requestForPackage(order.getNumNormal().intValue(), order.getNumChocolate().intValue(), order.getNumNut().intValue()))) != null) {
							fallback = false;
							order.setState(Order.State.IN_PROGRESS);
							this.buildPackage(responsePackage, packages, order);
							if (order.getDonePackages() == null) {
								order.setDonePackages(1);
							}
							else {
								order.setDonePackages(order.getDonePackages() + 1);
							}
							if (order.getDonePackages().equals(order.getPackages())) {
								order.setState(Order.State.DONE);
							}
						}
					}
				}
				
				if (fallback) {
					// fallback
					if ((responsePackage = this.checkResponse(this.requestForPackage(2, 2, 2))) != null) {
						this.buildPackage(responsePackage, packages, null);
					}
					else if ((responsePackage = this.checkResponse(this.requestForPackage(3, 3, 0))) != null) {
						this.buildPackage(responsePackage, packages, null);
					}
					else if ((responsePackage = this.checkResponse(this.requestForPackage(0, 3, 3))) != null) {
						this.buildPackage(responsePackage, packages, null);
					}
					else if ((responsePackage = this.checkResponse(this.requestForPackage(3, 0, 3))) != null) {
						this.buildPackage(responsePackage, packages, null);
					}
					else if ((responsePackage = this.checkResponse(this.requestForPackage(6, 0, 0))) != null) {
						this.buildPackage(responsePackage, packages, null);
					}
					else if ((responsePackage = this.checkResponse(this.requestForPackage(0, 6, 0))) != null) {
						this.buildPackage(responsePackage, packages, null);
					}
					else if ((responsePackage = this.checkResponse(this.requestForPackage(0, 0, 6))) != null) {
						this.buildPackage(responsePackage, packages, null);
					}
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
		if (message instanceof TextMessage || (message.getStringProperty("TYPE") != null) || message.getStringProperty("TYPE").equals("ArrayList<GingerBread>") == false) {
			return null;
		}
		if (message instanceof ObjectMessage) {
			ObjectMessage objMessage = (ObjectMessage) message;
			@SuppressWarnings("unchecked")
			ArrayList<GingerBread> response = (ArrayList<GingerBread>) objMessage.getObject();
			if (response == null || response.size() == 0) {
				return null;
			}
			return response;
		}
		return null;
	}

	private Message requestForPackage(int normal, int chocolate, int nut) throws JMSException {
		this.logger.info("Request now at server side", (Object[]) null);
		Hashtable<String, String> properties = new Hashtable<String, String>(1);
		properties.put("LOGISTICS_ID", String.valueOf((this.id)));
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

	private void close() throws JMSException {
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
	
	
	/*if (message instanceof ObjectMessage) {
	ObjectMessage objectMessage = (ObjectMessage) message;
	if (objectMessage.getObject() instanceof GingerBread) {
		GingerBread gingerBread = (GingerBread) objectMessage.getObject();
		gingerBread.setLogisticsId(this.id);
		gingerBread.setPackageId(this.packageId);
		gingerBread.setState(State.DONE);
		this.currentPackage.add(gingerBread);
		this.counter++;
		this.logger.info("Received gingerbread with id = " + gingerBread.getId() + " from baker with id = " + gingerBread.getBakerId(), (Object[]) null);

		if (this.counter == MAX_PACKAGE_SIZE) {
			this.logger.info("Send to server for monitoring.", (Object[]) null);
			try {
				for (GingerBread tmp : this.currentPackage) {
					this.monitoringSender.sendMonitoringMessage(tmp);
				}
			} catch (NamingException e) {
				e.printStackTrace();
			}
			this.currentPackage.clear();
			this.counter = 0;
			this.packageId = Utils.getID();
		}
	}
}*/	
}
