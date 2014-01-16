package factory.jmsImpl.logistics;

import java.util.Hashtable;
import java.util.Properties;

import javax.jms.JMSException;
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

import factory.entities.Order;
import factory.interfaces.LogisticsOrder;
import factory.utils.JMSUtils;
import factory.utils.JMSUtils.MessageType;

public class JMSLogisticsOrderImpl implements LogisticsOrder {

	private Order order;

	private final String PROPERTIES_FILE = "jms.properties";

	private Context ctx;
	private Logger logger = Logger.get(getClass());

	// queue for new orders
	private QueueConnection order_connection;
	private QueueSession order_session;
	private Queue order_queue;
	private QueueSender order_sender;

	public JMSLogisticsOrderImpl() {
		try {
			Properties properties = new Properties();
			properties.load(this.getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE));
			this.ctx = new InitialContext(properties);

			// Set queue connection for communication with server
			this.setup_orderQueue();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void run() {
		try {
			Hashtable<String, String> properties = new Hashtable<String, String>(1);
			properties.put("TYPE", "Order");
			JMSUtils.sendMessage(MessageType.OBJECTMESSAGE, 
					this.order, 
					properties, 
					this.order_session, 
					true, 
					this.order_sender);
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

	private void setup_orderQueue() throws NamingException, JMSException {
		this.logger.info("Initializing queue for orders...", (Object[]) null);
		QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory) ctx.lookup("qpidConnectionfactory");
		this.order_queue = (Queue) ctx.lookup("orderQueue");
		this.order_connection = queueConnectionFactory.createQueueConnection();
		this.order_session = this.order_connection.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE);
		this.order_sender = this.order_session.createSender(this.order_queue);
		this.order_connection.start();
		this.logger.info("Queue for orders created and connection started.", (Object[]) null);
	}

	private void close() throws JMSException {
		this.logger.info("Closing connection for queue.", (Object[]) null);
		this.logger.info("Closing queue for orders", (Object[]) null);
		this.order_sender.close();
		this.order_session.close();
		this.order_connection.close();
	}

}
