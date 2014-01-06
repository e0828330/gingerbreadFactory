package factory.jmsImpl.server;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

import factory.utils.JMSUtils;
import factory.utils.JMSUtils.MessageType;

public class JMSServerPackagingListener implements MessageListener {

	private JMSServerInstance server;
	
	public JMSServerPackagingListener(JMSServerInstance server) {
		this.server = server;
	}

	public void onMessage(Message message) {
		try {
			System.out.println(message);
			message.acknowledge();
			JMSUtils.sendReponse(MessageType.TEXTMESSAGE, 
					"STFU!", 
					null, 
					this.server.get_PackagingQueueSession(), 
					message.getJMSCorrelationID(),
					message.getJMSReplyTo());
		} catch (JMSException e) {
			e.printStackTrace();
		}

	}

}
