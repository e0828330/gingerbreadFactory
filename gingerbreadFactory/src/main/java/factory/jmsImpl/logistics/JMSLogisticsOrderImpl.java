package factory.jmsImpl.logistics;

import java.util.Hashtable;
import java.util.Properties;

import javax.jms.JMSException;
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

import factory.entities.Order;
import factory.interfaces.LogisticsOrder;
import factory.utils.JMSUtils;
import factory.utils.JMSUtils.MessageType;

public class JMSLogisticsOrderImpl implements LogisticsOrder {

	private Order order;

	private final String PROPERTIES_FILE = "jms.properties";

	private Context ctx;
	private Logger logger = Logger.get(getClass());

	// queue for loadBalancer
	private QueueConnection lb_connection;
	private QueueSession lb_session;
	private Queue lb_queue;
	private QueueSender lb_sender;

	private int factoryID;
	
	public JMSLogisticsOrderImpl() {
		try {
			Properties properties = new Properties();
			properties.load(this.getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE));
			this.ctx = new InitialContext(properties);

			// Set queue connection for communication with server
			this.setup_lbQueue();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void run() {
		try {
			Hashtable<String, String> properties = new Hashtable<String, String>(1);
			properties.put("TYPE", "Order");
			this.logger.info("Send order to load balancer.", (Object[]) null);
			JMSUtils.sendMessage(MessageType.OBJECTMESSAGE, 
					this.order, 
					properties, 
					this.lb_session, 
					false, 
					this.lb_sender);
			this.logger.info("Placed order on server-side.", (Object[]) null);
		} catch (JMSException e) {
			e.printStackTrace();
		}
		try {
			this.close();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	public void placeOrder(Order order) {
		this.order = order;
	}

	private void setup_lbQueue() throws NamingException, JMSException {
		this.logger.info("Initializing queue for lb...", (Object[]) null);
		QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory) ctx.lookup("qpidConnectionfactory");
		this.lb_queue = (Queue) ctx.lookup("loadBalancerQueue");
		this.lb_connection = queueConnectionFactory.createQueueConnection();
		this.lb_session = this.lb_connection.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE);
		this.lb_sender = this.lb_session.createSender(this.lb_queue);
		this.lb_connection.start();
		this.logger.info("Queue for lb created and connection started.", (Object[]) null);
	}

	private void close() throws JMSException {
		this.logger.info("Closing connection for queue.", (Object[]) null);
		this.logger.info("Closing queue for lb", (Object[]) null);
		this.lb_sender.close();
		this.lb_session.close();
		this.lb_connection.close();
	}

}
