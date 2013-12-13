package factory.jmsImpl.monitor;

import factory.interfaces.Monitor;

public class MonitorStart {

	public static void main(String[] args) {
		Monitor monitor = new JMSMonitor();
		System.out.println(monitor.getIngredients());
		System.out.println(monitor.getGingerBreads());
		System.out.println(monitor.getOvenContent());
		while(true) {

			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
