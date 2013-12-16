package factory.jmsImpl.server;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map.Entry;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import factory.entities.GingerBread;
import factory.utils.Messages;

public class JMSServerMonitoringListener implements MessageListener {

	private JMSServerInstance server;

	public JMSServerMonitoringListener(JMSServerInstance server) {
		this.server = server;
	}

	public void onMessage(Message message) {
		try {
			if (message instanceof ObjectMessage) {
				ObjectMessage objectMessage = (ObjectMessage) message;
				if (message.getStringProperty("TYPE") != null && message.getStringProperty("TYPE").equals("GingerBread")) {
					if (objectMessage.getObject() instanceof GingerBread) {
						GingerBread gingerBread = (GingerBread) objectMessage.getObject();
						this.server.getGingerBreads().put(gingerBread.getId(), gingerBread);
						Hashtable<String, String> properties = new Hashtable<String, String>();
						properties.put("TYPE", "ArrayList<GingerBread>");
						properties.put("EVENT", Messages.EVENT_GINGERBREAD_STATE_CHANGED);
						ArrayList<GingerBread> result = new ArrayList<GingerBread>();
						for (Entry<Long, GingerBread> tmp : this.server.getGingerBreads().entrySet()) {
							result.add(tmp.getValue());
						}
						this.server.sendEventToGUI(result, properties);
					}
				}
				else  if (message.getStringProperty("TYPE") != null && message.getStringProperty("TYPE").equals("ArrayList<GingerBread>")) {
					@SuppressWarnings("unchecked")
					ArrayList<GingerBread> list = (ArrayList<GingerBread>) objectMessage.getObject();
					for (GingerBread gingerBread : list) {
						this.server.getGingerBreads().put(gingerBread.getId(), gingerBread);
					}
					Hashtable<String, String> properties = new Hashtable<String, String>();
					properties.put("TYPE", "ArrayList<GingerBread>");
					properties.put("EVENT", Messages.EVENT_GINGERBREAD_STATE_CHANGED);
					ArrayList<GingerBread> result = new ArrayList<GingerBread>();
					for (Entry<Long, GingerBread> tmp : this.server.getGingerBreads().entrySet()) {
						result.add(tmp.getValue());
					}
					String bid = message.getStringProperty("BAKER_ID");
					if (bid != null) {
						Long bakerID = new Long(bid);
						this.server.getBakerProducedGingerBread_tmpList().put(bakerID, list);
						
						if (this.server.getDelivered_ingredients().containsKey(bakerID)) {
							this.server.getDelivered_ingredients().remove(bakerID);
						}
					}
					
					this.server.sendEventToGUI(result, properties);
				}
			}
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

}
