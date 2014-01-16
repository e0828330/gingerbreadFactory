package factory.jmsImpl.server;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.qpid.transport.util.Logger;

import factory.entities.GingerBread;
import factory.entities.LogisticsEntity;
import factory.entities.Order;
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
			if (message.getStringProperty("LOGISTICS_ID") != null && message.getStringProperty("REQTYPE") != null 
					&& message.getStringProperty("REQTYPE").equals("GET_PACKAGE")) {
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
				
				//get gingerbreads for order
				ArrayList<GingerBread> list = this.server.getPackage(nr_normal, nr_chocolate, nr_nut);
				if (list.size() == 0) {
					this.sendNoDataMessage(message);
				}
				else {
					this.logger.info("Package ok... send now to logistics.", (Object[]) null);
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
			else if (message.getStringProperty("LOGISTICS_ID") != null && message.getStringProperty("REQTYPE") != null 
					&& message.getStringProperty("REQTYPE").equals("GET_ORDERS")) {
				Long logisticsID = Long.valueOf(message.getStringProperty("LOGISTICS_ID"));
				// if not locked
				
				if (this.server.getPackageOrderesBlocked().get() == false) {
					this.server.getPackageOrderesBlocked().set(true);
					LinkedList<Order> tmp = new LinkedList<Order>(this.server.getOpenOrders());
					this.logger.info("Get request to send all orders to logistics with id = " + logisticsID + " and size = " + tmp.size(), (Object[]) null);
					Hashtable<String, String> properties = new Hashtable<String, String>();
					properties.put("TYPE", "LinkedList<Order>");
					
					JMSUtils.sendReponse(MessageType.OBJECTMESSAGE, 
							tmp, 
							properties, 
							this.server.get_PackagingQueueSession(), 
							message.getJMSCorrelationID(),
							message.getJMSReplyTo());
				}
				else {
					LogisticsEntity lEntity = new LogisticsEntity();
					lEntity.setDestination(message.getJMSReplyTo());
					lEntity.setCorrelationID(message.getJMSCorrelationID());
					lEntity.setID(logisticsID);
					this.logger.info("Enqueue logistic with id = " + logisticsID, (Object[]) null);
					this.server.getLogisticsWaitingList().add(lEntity);
					
				}
			}
			else if (message.getStringProperty("TYPE") != null && message.getStringProperty("TYPE").equals("ArrayList<Order>")){
				if (message instanceof ObjectMessage) {
					ObjectMessage objMessage = (ObjectMessage) message;
					ArrayList<Order> orderList = (ArrayList<Order>) objMessage.getObject();
					ConcurrentLinkedQueue<Order> tmp_open_orders = new ConcurrentLinkedQueue<Order>();
					
					for (Order o : orderList) {
						this.server.getOrder_list().put(o.getId(), o);
						if (o.getState() != Order.State.DONE) {
							tmp_open_orders.add(o);
						}
					}
					this.server.setOpenOrders(tmp_open_orders);
					this.logger.info("Overwrite order list with current orders...", (Object[]) null);
					
					// next in queue
					if (this.server.getLogisticsWaitingList().size() == 0) {
						this.logger.info("Unlock package-order-blocked value.", (Object[]) null);
						this.server.setPackageOrderesBlocked(false);
					}
					else {
						this.logger.info("Pop next logistic from waiting list...", (Object[]) null);
						LinkedList<Order> tmpList = new LinkedList<Order>(this.server.getOpenOrders());
						LogisticsEntity entity = this.server.getLogisticsWaitingList().remove();
						this.logger.info("Get request to send all orders to logistics with id = " + entity.getID() + " and size = " + tmpList.size(), (Object[]) null);
						Hashtable<String, String> properties = new Hashtable<String, String>();
						properties.put("TYPE", "LinkedList<Order>");
						JMSUtils.sendReponse(MessageType.OBJECTMESSAGE, 
								tmpList, 
								properties, 
								this.server.get_PackagingQueueSession(), 
								entity.getCorrelationID(),
								entity.getDestination());
					}
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
