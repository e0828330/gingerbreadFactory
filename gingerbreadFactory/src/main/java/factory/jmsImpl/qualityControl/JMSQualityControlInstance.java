package factory.jmsImpl.qualityControl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
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
import org.mozartspaces.core.Entry;
import org.mozartspaces.core.MzsConstants;

import factory.entities.GingerBread;
import factory.entities.GingerBread.State;

public class JMSQualityControlInstance implements Runnable, MessageListener {

	private Context ctx;
	private boolean isRunning = true;
	private Logger logger = Logger.get(getClass());	
	
	private float defectRate = 0.2f; // TODO: Set at startup
	private Long id = 1L; // TODO: Set at startup	
	
	// QualityControlQueue Baker -> QualityControl
	private QueueConnection qualityQueue_connection;
	private QueueSession qualityQueue_session;
	private Queue qualityQueue_queue;
	
	private boolean needsCheck = true;
	
	
	public JMSQualityControlInstance(String propertiesFile) throws IOException, NamingException, JMSException {
		Properties properties = new Properties();
		properties.load(this.getClass().getClassLoader().getResourceAsStream(propertiesFile));
		this.ctx = new InitialContext(properties);
		
		this.setup_qualityControlQueue();
	}
	
	private void setup_qualityControlQueue() throws NamingException, JMSException {
		this.logger.info("Initializing queue for bakers quality-control requests...", (Object[]) null); 
		QueueConnectionFactory queueConnectionFactory = 
				  (QueueConnectionFactory) ctx.lookup("qpidConnectionfactory");
		this.qualityQueue_queue = (Queue) ctx.lookup("qualityControlQueue");
		this.qualityQueue_connection = queueConnectionFactory.createQueueConnection();
		this.qualityQueue_session = this.qualityQueue_connection.createQueueSession(true, Session.AUTO_ACKNOWLEDGE);
		this.qualityQueue_session.setMessageListener(this);
		this.qualityQueue_connection.start();	
		this.logger.info("Queue for quality-control startet.", (Object[]) null); 		
	}	
	
	public void run() {
		System.out.println("\n======================================");
		System.out.println("Type 'exit' to to shut down the baker");
		System.out.println("======================================\n");

		while (isRunning) {		
			try {
				BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
				String s = bufferRead.readLine();
				if (s.equals("exit")) {
					break;
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			
			try {
				this.close();
			} catch (JMSException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void close() throws JMSException {
		this.logger.info("Closing quality-control queue.", (Object[]) null);
		this.qualityQueue_session.close();
		this.qualityQueue_connection.close();
	}
	
	public void shutDown() {
		this.isRunning = false;
	}

	public void onMessage(Message message) {
		try {
			this.qualityQueue_session.commit();
			this.qualityQueue_connection.stop();
			if (message instanceof ObjectMessage) {
				ObjectMessage objectMessage = (ObjectMessage) message;
				if (objectMessage.getStringProperty("TYPE") != null &&
						objectMessage.getStringProperty("TYPE").equals("ArrayList<GingerBread>")) {
					
					@SuppressWarnings("unchecked")
					ArrayList<GingerBread> testList = (ArrayList<GingerBread>) objectMessage.getObject();
					
					// We don't need to check this round
					if (this.needsCheck == false) {
						this.logger.info("This charge is just forwarded...", (Object[]) null);
						for (GingerBread tested : testList) {
							tested.setState(State.CONTROLLED);
						}						
						this.forwardCharge(testList);
						needsCheck = !needsCheck;
						this.qualityQueue_connection.start();						
						return;
					}
					
					// We need to check this round
									
					// Shuffle to have a random selection for testing
					Collections.shuffle(testList);
					
					// Eat he yummmy yummmy gingerbread
					if (Math.random() < defectRate) {
						this.logger.info("Whole charge is garbage because of sucky tasting gingerbread.", (Object[]) null);					
						for (GingerBread tested : testList) {
							tested.setState(State.GARBAGE);
						}
					}
					else {
						this.logger.info("This charge is fine.", (Object[]) null);
						for (GingerBread tested : testList) {
							tested.setState(State.CONTROLLED);
						}
					}
					testList.remove(0); // remove the eaten one
					this.forwardCharge(testList);
					// Toggle needs check state
					needsCheck = !needsCheck;
					this.qualityQueue_connection.start();
				}
			}				
		}
		catch (JMSException e) {
			e.printStackTrace();
		}		
	}
	
	private void forwardCharge(ArrayList<GingerBread> charge) {
		System.out.println("Forward to Logistik");
	}

}
