package factory.jmsImpl.server;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map.Entry;

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
					Hashtable<String, String> properties = new Hashtable<String, String>();
					properties.put("TYPE", "ArrayList<GingerBread>");
					properties.put("EVENT", "Gingerbread");
					ArrayList<GingerBread> result = new ArrayList<GingerBread>();
					for (Entry<Long, GingerBread> tmp : this.server.getGingerBreads().entrySet()) {
						result.add(tmp.getValue());
					}
					this.server.sendEventToGUI(result, properties);
				}
			}
		}
		catch (JMSException e) {
			e.printStackTrace();
		}
	}

}
