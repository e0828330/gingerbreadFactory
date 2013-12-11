package factory.jmsImpl.logistics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.qpid.transport.util.Logger;

import factory.entities.GingerBread;
import factory.entities.GingerBread.State;

public class JMSQualityLogisticsInstance implements Runnable {

	private Context ctx;
	private boolean isRunning = true;
	private Logger logger = Logger.get(getClass());	
	
	private Long id = 1L; // TODO: Set at startup	
	
	// QualityControlQueue QualityControl -> Logistics
	private QueueConnection logisticsQueue_connection;
	private QueueSession logisticsQueue_session;
	private MessageConsumer logisticsQueue_consumer;
	private Queue logisticsQueue_queue;
	
	private final int MAX_PACKAGE_SIZE = 6;
	
	private ArrayList<GingerBread> currentPackage = new ArrayList<GingerBread>(MAX_PACKAGE_SIZE);
	private int counter = 0;
	
	public JMSQualityLogisticsInstance(String propertiesFile) throws IOException, NamingException, JMSException {
		Properties properties = new Properties();
		properties.load(this.getClass().getClassLoader().getResourceAsStream(propertiesFile));
		this.ctx = new InitialContext(properties);
		
		this.setup_logisticsQueue();
	}
	
	private void setup_logisticsQueue() throws NamingException, JMSException {
		this.logger.info("Initializing queue for logistics...", (Object[]) null); 
		QueueConnectionFactory queueConnectionFactory = 
				  (QueueConnectionFactory) ctx.lookup("qpidConnectionfactory");
		this.logisticsQueue_queue = (Queue) ctx.lookup("logisticsQueue");
		this.logisticsQueue_connection = queueConnectionFactory.createQueueConnection();
		this.logisticsQueue_session = this.logisticsQueue_connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
		this.logisticsQueue_consumer = this.logisticsQueue_session.createConsumer(this.logisticsQueue_queue);
		this.logisticsQueue_connection.start();	
		this.logger.info("Queue for quality-control startet.", (Object[]) null); 		
	}	
	
	public void run() {
		try {
			while (isRunning) {	
				Message message = this.logisticsQueue_consumer.receive();
				if (message instanceof ObjectMessage) {
					ObjectMessage objectMessage = (ObjectMessage) message;
					if (objectMessage.getObject() instanceof GingerBread) {
						GingerBread gingerBread = (GingerBread) objectMessage.getObject();
						gingerBread.setLogisticsId(this.id);
						gingerBread.setState(State.DONE);
						this.currentPackage.add(gingerBread);
						this.counter++;
						this.logger.info("Received gingerbread with id = " + gingerBread.getId() + 
								" from baker with id = " + gingerBread.getBakerId(), (Object[]) null);
						
						if (this.counter == MAX_PACKAGE_SIZE) {
							// TODO send to server
							this.logger.info("Send to server for monitoring.", (Object[]) null);
							this.currentPackage.clear();
							this.counter = 0;
						}
					}
				}
			}
		}
		catch (JMSException e) {
			e.printStackTrace();
		}
		
		try {
			this.close();
		} catch (JMSException e) {
			e.printStackTrace();
		}		
	}
	
	private void close() throws JMSException {
		this.logger.info("Closing quality-control queue.", (Object[]) null);
		this.logisticsQueue_consumer.close();
		this.logisticsQueue_session.close();
		this.logisticsQueue_connection.close();
	}
	
	public void shutDown() {
		this.isRunning = false;
	}
}
