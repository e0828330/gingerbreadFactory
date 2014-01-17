package factory.jmsImpl.loadBalancer;

import java.util.ArrayList;

public class JMSLoadBalancer {

	public static void main(String[] args) {
		ArrayList<Integer> factories = new ArrayList<Integer>();
		for (String arg : args) {
			try {
				Integer id = Integer.parseInt(arg);
				factories.add(id);
				
			}
			catch(NumberFormatException e) {
				System.err.println("Invalid factory ID: " + arg);
				System.exit(1);
			}
		}
		
		Thread t = new Thread(new LoadBalancerInstance(factories));
		t.start();
	}
	
}
