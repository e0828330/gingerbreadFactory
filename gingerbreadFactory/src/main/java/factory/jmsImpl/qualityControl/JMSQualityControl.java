package factory.jmsImpl.qualityControl;

import java.io.IOException;
import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import factory.jmsImpl.baker.JMSBaker;
import factory.jmsImpl.baker.JMSBakerInstance;
import factory.utils.JMSUtils;
import factory.utils.Messages;
import factory.utils.Utils;

public class JMSQualityControl {

	// benchmark topic
	private static TopicConnection benchmarkTopicConnection;
	private static TopicSession benchmarkSession;
	private static Topic benchmarkTopic;
	private static TopicSubscriber benchmarkSubscriber;

	private static Context ctx;

	public static void main(String[] args) {

		try {
			Properties properties = new Properties();
			properties.load(JMSBaker.class.getClassLoader().getResourceAsStream("jms.properties"));
			ctx = new InitialContext(properties);
			final JMSQualityControlInstance qualitycontrol = new JMSQualityControlInstance(Utils.getStartupId(args), Utils.getStartupDefectRate(args), JMSUtils.parseFactoryID(args, 2));

			if (JMSUtils.BENCHMARK) {
				System.out.println("Benchmark-Mode: Waiting for start-signal.");
				TopicConnectionFactory topicConnectionFactory = (TopicConnectionFactory) ctx.lookup("qpidConnectionfactory");
				benchmarkTopic = (Topic) ctx.lookup("benchmarkTopic");
				benchmarkTopicConnection = topicConnectionFactory.createTopicConnection();
				benchmarkSession = benchmarkTopicConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
				benchmarkSubscriber = benchmarkSession.createSubscriber(benchmarkTopic);
				benchmarkTopicConnection.start();

				Message message = benchmarkSubscriber.receive();

				if (message instanceof TextMessage) {

					TextMessage textMessage = (TextMessage) message;
					if (textMessage.getText() != null && textMessage.getText().equals(Messages.BENCHMARK_START)) {

						
						Thread qualityControl = new Thread(qualitycontrol);
						qualityControl.start();

						Thread stop = new Thread(new Runnable() {

							public void run() {
								try {
									Message message = benchmarkSubscriber.receive();

									if (message instanceof TextMessage) {
										TextMessage textMessage = (TextMessage) message;
										if (textMessage.getText() != null && textMessage.getText().equals(Messages.BENCHMARK_STOP)) {
											System.exit(0);
										}
									}
								} catch (JMSException e) {
									e.printStackTrace();
								}
							}
						});

						stop.start();
					}
				}

			} else {
				Thread qualityControl = new Thread(qualitycontrol);
				qualityControl.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NamingException e) {
			e.printStackTrace();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

}
