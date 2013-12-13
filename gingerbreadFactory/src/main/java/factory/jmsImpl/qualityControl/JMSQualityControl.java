package factory.jmsImpl.qualityControl;

import java.io.IOException;

import javax.jms.JMSException;
import javax.naming.NamingException;

import factory.jmsImpl.baker.JMSBakerInstance;
import factory.utils.Utils;

public class JMSQualityControl {

	public static void main(String[] args) {
		try {
			JMSQualityControlInstance qualitycontrol = new JMSQualityControlInstance(Utils.getStartupId(args));
			Thread bakerThread = new Thread(qualitycontrol);	
			bakerThread.start();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (NamingException e) {
			e.printStackTrace();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

}
