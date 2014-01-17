package factory.jmsImpl.server;

import java.io.IOException;

import javax.jms.JMSException;
import javax.naming.NamingException;

import factory.utils.JMSUtils;

public class JMSServer {


	public static void main(String argv[]) {
		try {
			JMSServerInstance jmsServerInstance = new JMSServerInstance(JMSUtils.parseFactoryID(argv, 0));
			Thread thread = new Thread(jmsServerInstance);
			thread.start();
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
