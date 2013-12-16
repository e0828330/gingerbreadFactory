package factory.jmsImpl.logistics;

import java.io.IOException;

import javax.jms.JMSException;
import javax.naming.NamingException;

import factory.jmsImpl.baker.JMSBakerInstance;
import factory.utils.Utils;

public class JMSLogistics {

	public static void main(String[] args) {
		try {
			JMSQualityLogisticsInstance qualitycontrol = new JMSQualityLogisticsInstance(Utils.getStartupId(args));
			Thread logisticsThread = new Thread(qualitycontrol);	
			logisticsThread.start();
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
