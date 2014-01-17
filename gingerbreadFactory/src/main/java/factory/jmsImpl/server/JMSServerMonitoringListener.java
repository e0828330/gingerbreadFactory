package factory.jmsImpl.server;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map.Entry;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.qpid.transport.util.Logger;

import factory.entities.GingerBread;
import factory.utils.Messages;

public class JMSServerMonitoringListener implements MessageListener {

	private JMSServerInstance server;
	
	private Logger logger = Logger.get(getClass());

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
						// save state for gingerbread
						this.server.getGingerBreads().put(gingerBread.getId(), gingerBread);
						// save controlled ones, for better access for logistics
						if (gingerBread.getState() == GingerBread.State.CONTROLLED && gingerBread.getFlavor() != null) {
							this.server.getControlledGingerBreadList().get(gingerBread.getFlavor()).add(gingerBread);
							// print for debug:
							this.logger.info("Normal controlled: " + this.server.getControlledGingerBreadList().get(GingerBread.Flavor.NORMAL).size() +
							 ", Chocolate controlled: " + this.server.getControlledGingerBreadList().get(GingerBread.Flavor.CHOCOLATE).size() + 
							 ", Nut controlled: " + this.server.getControlledGingerBreadList().get(GingerBread.Flavor.NUT).size() + "\n");
						}
						// prepare for gui
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
					// save state for gingerbreads
					for (GingerBread gingerBread : list) {
						this.server.getGingerBreads().put(gingerBread.getId(), gingerBread);
					}
					// prepare for gui
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
					
					if (message.getStringProperty("NUMBER_OF_PACKAGES") != null) {
						int value = Integer.parseInt(message.getStringProperty("NUMBER_OF_PACKAGES"));
						this.logger.info("Received " + value + " finished packages", (Object[]) null);
						this.server.getTotalFinishedPackages().addAndGet(value);
					}
					
					this.server.sendEventToGUI(result, properties);
				}
			}
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

}
