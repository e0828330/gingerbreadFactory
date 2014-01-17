package factory.entities;

import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;

public class JMSFactoryConnection {

	private int factoryID;
	private QueueConnection connection;
	private QueueSession session;
	private Queue queue;
	private QueueReceiver receiver;
	private QueueSender sender;
	public QueueConnection getConnection() {
		return connection;
	}
	public void setConnection(QueueConnection connection) {
		this.connection = connection;
	}
	public QueueSession getSession() {
		return session;
	}
	public void setSession(QueueSession session) {
		this.session = session;
	}
	public Queue getQueue() {
		return queue;
	}
	public void setQueue(Queue queue) {
		this.queue = queue;
	}
	public QueueReceiver getReceiver() {
		return receiver;
	}
	public void setReceiver(QueueReceiver receiver) {
		this.receiver = receiver;
	}
	public QueueSender getSender() {
		return sender;
	}
	public void setSender(QueueSender sender) {
		this.sender = sender;
	}
	public int getId() {
		return factoryID;
	}
	public void setId(int id) {
		this.factoryID = id;
	}
	
}
