package factory.jmsImpl.baker;

import java.io.IOException;

import javax.naming.NamingException;

import factory.utils.JMSUtils;
import factory.utils.Utils;

public class JMSBaker {

	public static void main(String[] args) {
		try {
			JMSBakerInstance baker = new JMSBakerInstance(Utils.getStartupId(args), JMSUtils.parseFactoryID(args, 1));
			Thread bakerThread = new Thread(baker);
			bakerThread.start();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NamingException e) {
			e.printStackTrace();
		}
	}
}
