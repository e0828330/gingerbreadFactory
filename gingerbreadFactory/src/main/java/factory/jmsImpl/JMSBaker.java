package factory.jmsImpl;

import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;

public class JMSBaker {

	public static void main(String[] args) {
		JMSBakerInstance baker = new JMSBaker.JMSBakerInstance();
		Thread bakerThread = new Thread(baker);	
		bakerThread.start();
	}
	
	private static class JMSBakerInstance implements Runnable {
	
		private boolean isRunning = true;
		
		// ingredients topic attributes
		private Topic ingredientsTopic_topic;
		private TopicConnection ingredientsTopic_connection;
		private TopicSession ingredientsTopic_session;
		private TopicSubscriber ingredientsTopic_subscriber;	
		
		public JMSBakerInstance() {
			// TODO Auto-generated constructor stub
		}
	
		public void run() {
			do {
				
				
			} while (isRunning);
			this.close();
		}
		
		public void shutDown() {
			this.isRunning = false;
		}
		
		private void close() {
			
		}
	}

}
