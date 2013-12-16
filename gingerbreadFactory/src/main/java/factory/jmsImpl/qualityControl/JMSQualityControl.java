package factory.jmsImpl.qualityControl;

import java.io.IOException;

import javax.jms.JMSException;
import javax.naming.NamingException;

import factory.utils.Utils;

public class JMSQualityControl {

	public static void main(String[] args) {
		try {
			JMSQualityControlInstance qualitycontrol = new JMSQualityControlInstance(Utils.getStartupId(args), Utils.getStartupDefectRate(args));
			Thread qualityControl = new Thread(qualitycontrol);
			qualityControl.start();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NamingException e) {
			e.printStackTrace();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

}
