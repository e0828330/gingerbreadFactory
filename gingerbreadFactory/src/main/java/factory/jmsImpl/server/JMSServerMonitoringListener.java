package factory.jmsImpl.server;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import factory.entities.GingerBread;

public class JMSServerMonitoringListener implements MessageListener {

	private JMSServerInstance server;
	
	public JMSServerMonitoringListener(JMSServerInstance server) {
		this.server = server;
	}

	public void onMessage(Message message) {
		try {
			if (message instanceof ObjectMessage) {
				ObjectMessage objectMessage = (ObjectMessage) message;
				if (objectMessage.getObject() instanceof GingerBread) {
					GingerBread gingerBread = (GingerBread) objectMessage.getObject();
					this.server.getGingerBreads().put(gingerBread.getId(), gingerBread);
				}
			}
		}
		catch (JMSException e) {
			e.printStackTrace();
		}
	}

}
