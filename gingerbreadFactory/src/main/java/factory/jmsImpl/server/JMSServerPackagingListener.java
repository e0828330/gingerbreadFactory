package factory.jmsImpl.server;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.apache.qpid.transport.util.Logger;

import factory.utils.JMSUtils;
import factory.utils.Messages;
import factory.utils.JMSUtils.MessageType;

public class JMSServerPackagingListener implements MessageListener {

	private JMSServerInstance server;
	
	private Logger logger = Logger.get(getClass());
	
	public JMSServerPackagingListener(JMSServerInstance server) {
		this.server = server;
	}

	public void onMessage(Message message) {
		try {
			message.acknowledge();
			if (message.getStringProperty("LOGISTICS_ID") != null) {
				Long logisticsID = Long.valueOf(message.getStringProperty("LOGISTICS_ID"));
				
				// set values of packages
				int nr_normal = 0;
				int nr_nut = 0;
				int nr_chocolate = 0;
				if (message.getStringProperty(Messages.FLAVOR_NORMAL) != null) {
					nr_normal = Integer.valueOf(message.getStringProperty(Messages.FLAVOR_NORMAL)).intValue();
				}
				if (message.getStringProperty(Messages.FLAVOR_CHOCOLATE) != null) {
					nr_chocolate = Integer.valueOf(message.getStringProperty(Messages.FLAVOR_CHOCOLATE)).intValue();
				}
				if (message.getStringProperty(Messages.FLAVOR_NUT) != null) {
					nr_nut = Integer.valueOf(message.getStringProperty(Messages.FLAVOR_NUT)).intValue();
				}
				
				this.logger.info("Request for " + nr_normal + "x Normal, " + nr_nut + "x Nuts and " + nr_chocolate + "x Chocolate.", (Object[]) null);
				JMSUtils.sendReponse(MessageType.TEXTMESSAGE, 
						"MHHKAY", 
						null, 
						this.server.get_PackagingQueueSession(), 
						message.getJMSCorrelationID(),
						message.getJMSReplyTo());					
				
				
			}
			else {
				JMSUtils.sendReponse(MessageType.TEXTMESSAGE, 
						Messages.NO_STORED_DATA, 
						null, 
						this.server.get_PackagingQueueSession(), 
						message.getJMSCorrelationID(),
						message.getJMSReplyTo());	
			}
		} catch (JMSException e) {
			e.printStackTrace();
		}

	}

}
