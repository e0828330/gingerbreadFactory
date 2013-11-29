package factory.jmsImpl;

import java.io.IOException;

import javax.jms.JMSException;
import javax.naming.NamingException;

public class JMSServer {


	public static void main(String argv[]) {
		try {
			JMSServerInstance jmsServerInstance = new JMSServerInstance("jms.properties");
			Thread thread = new Thread(jmsServerInstance);
			thread.start();
			
			//jmsServerInstance.shutDown();
		}
		catch (IOException e) {
			// TODO handling
			e.printStackTrace();
		}
		catch (NamingException e) {
			// TODO handling
			e.printStackTrace();			
		}
		catch (JMSException e) {
			// TODO handling
			e.printStackTrace();
		}
	}
	
	
	
}
