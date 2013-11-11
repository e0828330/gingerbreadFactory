package factory.factory;

import java.io.IOException;
import java.util.Properties;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import factory.entities.Ingredient;

public class AppJMS {

	private Connection connection;
	private Context context;
	
	public static void main(String[] args) throws IOException, NamingException, JMSException {
		AppJMS test = new AppJMS();
		test.run();

	}
	
	public void run() throws IOException, NamingException, JMSException {
		Properties properties = new Properties();
		properties.load(this.getClass().getClassLoader().getResourceAsStream("jms.properties"));
		context = new InitialContext(properties);

		ConnectionFactory connectionFactory = (ConnectionFactory) context.lookup("qpidConnectionfactory");
		connection = connectionFactory.createConnection();
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Queue queue = (Queue) context.lookup("ingredients");
		MessageProducer messageProducer = session.createProducer(queue);
		connection.start();
		
		ObjectMessage msg = session.createObjectMessage();
		msg.setObject(new Ingredient(1l, Ingredient.Type.EGG));
		messageProducer.send(msg);
		session.close();
		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		MessageConsumer reader = session.createConsumer(queue);
		System.out.println(((ObjectMessage)reader.receive()).getObject());
		connection.close();
		context.close();
	}

}
