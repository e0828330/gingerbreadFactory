package factory.jmsImpl.server;

import java.util.ArrayList;
import java.util.Hashtable;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.apache.qpid.transport.util.Logger;

import factory.entities.GingerBread;
import factory.utils.JMSUtils;
import factory.utils.JMSUtils.MessageType;
import factory.utils.Messages;

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
				
				//TODO: get gingerbreads for order
				ArrayList<GingerBread> list = this.server.getPackage(nr_normal, nr_chocolate, nr_nut);
				if (list.size() == 0) {
					this.sendNoDataMessage(message);
				}
				else {
					Hashtable<String, String> properties = new Hashtable<String, String>(2);
					properties.put("TYPE", "ArrayList<GingerBread>");
					JMSUtils.sendReponse(MessageType.OBJECTMESSAGE, 
							list, 
							properties, 
							this.server.get_PackagingQueueSession(), 
							message.getJMSCorrelationID(),
							message.getJMSReplyTo());			
				}
				
				
			}
			else {
				this.sendNoDataMessage(message);
			}
		} catch (JMSException e) {
			e.printStackTrace();
		}

	}
	
	private void sendNoDataMessage(Message message) throws JMSException {
		JMSUtils.sendReponse(MessageType.TEXTMESSAGE, 
				Messages.NO_STORED_DATA, 
				null, 
				this.server.get_PackagingQueueSession(), 
				message.getJMSCorrelationID(),
				message.getJMSReplyTo());	
	}

}
