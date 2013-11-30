package factory.jmsImpl;

import java.io.IOException;
import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.qpid.transport.util.Logger;

public class JMSBakerInstance implements Runnable {

	private Context ctx;
	private boolean isRunning = true;
	private Logger logger = Logger.get(getClass());

	// ingredients topic attributes
	private Topic ingredientsTopic_topic;
	private TopicConnection ingredientsTopic_connection;
	private TopicSession ingredientsTopic_session;
	private TopicSubscriber ingredientsTopic_subscriber;

	public JMSBakerInstance(String propertiesFile) throws IOException,
			NamingException {
		Properties properties = new Properties();
		properties.load(this.getClass().getClassLoader()
				.getResourceAsStream(propertiesFile));
		this.ctx = new InitialContext(properties);

		// init topic for ingredients
		try {
			this.setup_ingredientsTopic();
		} catch (NamingException e) {
			e.printStackTrace();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	private void setup_ingredientsTopic() throws NamingException, JMSException {
		this.logger.info("Initializing topic for ingredients...",
				(Object[]) null);
		TopicConnectionFactory topicConnectionFactory = (TopicConnectionFactory) ctx
				.lookup("qpidConnectionfactory");

		this.ingredientsTopic_topic = (Topic) ctx.lookup("ingredientsTopic");

		this.ingredientsTopic_connection = topicConnectionFactory
				.createTopicConnection();

		this.ingredientsTopic_session = this.ingredientsTopic_connection
				.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

		this.ingredientsTopic_subscriber = this.ingredientsTopic_session
				.createSubscriber(this.ingredientsTopic_topic);

		this.ingredientsTopic_subscriber
				.setMessageListener(new JMSBakerIngredientsTopicListener(this));

		this.ingredientsTopic_connection.start();
	}

	public void run() {
		do {

		} while (isRunning);
		try {
			this.close();
		}
		catch (JMSException e) {
			e.printStackTrace();
		}
	}
	
	public void sendRequestForIngredients() {
		
	}

	public void shutDown() {
		this.isRunning = false;
	}

	private void close() throws JMSException {
		this.logger.info("Closing ingredients subscriber for topic.", (Object[]) null);
		this.ingredientsTopic_subscriber.close();
		this.logger.info("Closing ingredients session for topic.", (Object[]) null);
		this.ingredientsTopic_session.close();
		this.logger.info("Closing ingredients connection for topic.", (Object[]) null);
		this.ingredientsTopic_connection.close();
		
		this.logger.info("BakerInstance shutting down.", (Object[]) null); 		
	}
}