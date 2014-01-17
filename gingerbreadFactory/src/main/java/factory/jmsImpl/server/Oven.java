package factory.jmsImpl.server;

import java.util.ArrayList;

import org.apache.qpid.transport.util.Logger;

import factory.entities.ChargeReplyObject;
import factory.entities.GingerBread;
import factory.entities.GingerBread.State;
import factory.utils.JMSUtils;

public class Oven implements Runnable {

	private JMSServerInstance server;
	
	private ArrayList<ChargeReplyObject> charges;
	
	private Logger logger = Logger.get(getClass());
	
	private int WAITING_TIME = 10000;
	
	public Oven(JMSServerInstance server, ArrayList<ChargeReplyObject> charges) {
		this.server = server;
		this.charges = charges;
	}
	
	public void run() {
		this.logger.info("OVEN STARTED", (Object[]) null);

		for (ChargeReplyObject charge : this.charges) {
			this.logger.info("Baking charge " + charge.toString(), (Object[]) null);
			for (GingerBread gingerBread : charge.getCharge()) {
				gingerBread.setState(State.BAKED);
				this.server.getGingerBreads().put(gingerBread.getId(), gingerBread);
			}
		}
		if (!JMSUtils.BENCHMARK) {
			try {
				Thread.sleep(WAITING_TIME);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		this.server.stopOven(this.charges);
	}

}
