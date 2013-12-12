package factory.jmsImpl.baker;

import java.io.IOException;

import javax.naming.NamingException;

public class JMSBaker {

	public static void main(String[] args) {
		try {
			JMSBakerInstance baker = new JMSBakerInstance("jms.properties");
			Thread bakerThread = new Thread(baker);
			bakerThread.start();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NamingException e) {
			e.printStackTrace();
		}
	}
}
