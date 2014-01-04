package factory.jmsImpl.server;

import java.util.Hashtable;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;

import org.apache.qpid.transport.util.Logger;

import factory.utils.JMSUtils;
import factory.utils.Messages;
import factory.utils.JMSUtils.MessageType;

public class JMSServerBakerGeneralRequestListener implements MessageListener {

	private JMSServerInstance server;

	private Logger logger = Logger.get(getClass());

	public JMSServerBakerGeneralRequestListener(JMSServerInstance server) {
		this.server = server;
	}

	public void onMessage(Message message) {
		System.out.println(message);
		try {
			if (message instanceof TextMessage) {
				TextMessage textMessage = (TextMessage) message;
				String bid = message.getStringProperty("BAKER_ID");
				if (bid != null) {
					Long bakerID = new Long(bid);
					if (textMessage.getText() != null && textMessage.getText().equals(Messages.BAKER_GENERAL_REQUEST_MESSAGE)) {
						if (this.server.getDelivered_ingredients().containsKey(bakerID)) {
							//ObjectMessage responseMessage = this.server.get_BakerRequestsession().createObjectMessage();
							//responseMessage.setObject(this.server.getDelivered_ingredients().get(bakerID));
							//responseMessage.setStringProperty("TYPE", "ArrayList<GingerBreadTransactionObject>");
							message.acknowledge();
							//MessageProducer producer = this.server.get_BakerRequestsession().createProducer(message.getJMSReplyTo());
							//responseMessage.setJMSCorrelationID(message.getJMSCorrelationID());
							//producer.send(responseMessage);
							//producer.close();
							
							Hashtable<String, String> properties = new Hashtable<String, String>(1);
							properties.put("TYPE", "ArrayList<GingerBreadTransactionObject>");
							
							JMSUtils.sendReponse(MessageType.OBJECTMESSAGE, 
									this.server.getDelivered_ingredients().get(bakerID), 
									properties, 
									this.server.get_BakerRequestsession(), 
									message.getJMSCorrelationID(), 
									message.getJMSReplyTo());
							
							return;
						} else if (this.server.getBakerProducedGingerBread_tmpList().containsKey(bakerID)) {
							/*ObjectMessage responseMessage = this.server.get_BakerRequestsession().createObjectMessage();
							responseMessage.setStringProperty("TYPE", "ArrayList<GingerBread>");
							responseMessage.setStringProperty("STATE", "PRODUCED");
							responseMessage.setObject(this.server.getBakerProducedGingerBread_tmpList().get(bakerID));
							message.acknowledge();
							MessageProducer producer = this.server.get_BakerRequestsession().createProducer(message.getJMSReplyTo());
							responseMessage.setJMSCorrelationID(message.getJMSCorrelationID());
							producer.send(responseMessage);
							producer.close();*/
							message.acknowledge();
							Hashtable<String, String> properties = new Hashtable<String, String>(2);
							properties.put("TYPE", "ArrayList<GingerBread>");
							properties.put("STATE", "PRODUCED");
							JMSUtils.sendReponse(MessageType.OBJECTMESSAGE, 
									this.server.getBakerProducedGingerBread_tmpList().get(bakerID), 
									properties, 
									this.server.get_BakerRequestsession(), 
									message.getJMSCorrelationID(), 
									message.getJMSReplyTo());
							
							return;
						} else if (this.server.getBakersChargeInOven().containsKey(bakerID)) {
							/*ObjectMessage responseMessage = this.server.get_BakerRequestsession().createObjectMessage();
							responseMessage.setStringProperty("STATE", "BAKED");
							responseMessage.setStringProperty("TYPE", "ArrayList<GingerBread>");
							responseMessage.setObject(this.server.getBakersChargeInOven().get(bakerID));
							message.acknowledge();
							MessageProducer producer = this.server.get_BakerRequestsession().createProducer(message.getJMSReplyTo());
							responseMessage.setJMSCorrelationID(message.getJMSCorrelationID());
							producer.send(responseMessage);
							producer.close();*/
							message.acknowledge();
							Hashtable<String, String> properties = new Hashtable<String, String>(2);
							properties.put("TYPE", "ArrayList<GingerBread>");
							properties.put("STATE", "BAKED");
							JMSUtils.sendReponse(MessageType.OBJECTMESSAGE, 
									this.server.getBakersChargeInOven().get(bakerID), 
									properties, 
									this.server.get_BakerRequestsession(), 
									message.getJMSCorrelationID(), 
									message.getJMSReplyTo());
							return;
						} else {
							message.acknowledge();
							/*Message responseMessage = this.server.get_BakerRequestsession().createTextMessage(Messages.NO_STORED_DATA);
							message.acknowledge();
							MessageProducer producer = this.server.get_BakerRequestsession().createProducer(message.getJMSReplyTo());
							responseMessage.setJMSCorrelationID(message.getJMSCorrelationID());
							producer.send(responseMessage);
							producer.close();*/

							JMSUtils.sendReponse(MessageType.TEXTMESSAGE, 
									Messages.NO_STORED_DATA, 
									null, 
									this.server.get_BakerRequestsession(), 
									message.getJMSCorrelationID(), 
									message.getJMSReplyTo());
						}
					} else if (textMessage.getText() != null && textMessage.getText().equals(Messages.SERVER_REMOVE_STORED_BAKED_GINGERBREADS)) {
						if (this.server.getBakersChargeInOven().containsKey(bakerID)) {
							this.server.getBakersChargeInOven().remove(bakerID);
							message.acknowledge();
							return;
						}
					}
				}
			}

		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

}
