package factory.spacesImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;

import org.mozartspaces.capi3.FifoCoordinator;
import org.mozartspaces.capi3.LindaCoordinator;
import org.mozartspaces.core.Capi;
import org.mozartspaces.core.ContainerReference;
import org.mozartspaces.core.DefaultMzsCore;
import org.mozartspaces.core.Entry;
import org.mozartspaces.core.MzsConstants;
import org.mozartspaces.core.MzsCore;
import org.mozartspaces.core.MzsCoreException;

import factory.entities.Order;

public class LoadBalancer implements Runnable {
	
	public static String loadBalancerURL = "xvsm://localhost:9876";
	
	private MzsCore core;
	private HashMap<Integer, ContainerReference> factories;
	
	public LoadBalancer(MzsCore core, HashMap<Integer, ContainerReference> factories) {
		this.core = core;
		this.factories = factories;
	}

	public static void main(String[] args) throws MzsCoreException, URISyntaxException, IOException {
		System.setProperty("mozartspaces.configurationFile", "mozartspaces-loadbalancer.xml");
		
		MzsCore core = DefaultMzsCore.newInstance();
		Capi capi = new Capi(core);
		capi.createContainer("ordersLB", new URI(loadBalancerURL), MzsConstants.Container.UNBOUNDED, null, new FifoCoordinator());

		HashMap<Integer, ContainerReference> factories = new HashMap<Integer, ContainerReference>();
		
		for (String arg : args) {
			try {
				Integer id = Integer.parseInt(arg);
				factories.put(id, capi.lookupContainer("orders", new URI("xvsm://localhost:" + id), MzsConstants.RequestTimeout.INFINITE, null));
			}
			catch(NumberFormatException e) {
				System.err.println("Invalid factory ID: " + arg);
				System.exit(1);
			}
		}
		
		LoadBalancer lb = new LoadBalancer(core, factories);
		new Thread(lb).start();
		
		System.out.println("Loadbalancer running");
		System.out.println("=====================================");
		
		for (Integer factory : factories.keySet()) {
			System.out.println("Managing factory " + factory + " running at " + factories.get(factory));
		}
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		System.out.println("Type quit or exit to quit.");
		
		String input;
		while ((input = br.readLine()) != null) {
			 if (input.equals("quit") || input.equals("exit")) {
				break;
			}
		}
		
		System.out.println("Shutting down.");
		core.shutdown(true);

	}

	public void run() {
		Capi capi = new Capi(core);
		
		try {
			ContainerReference container = capi.lookupContainer("ordersLB", new URI(loadBalancerURL), MzsConstants.RequestTimeout.INFINITE, null);
			ArrayList<Order> orders = capi.take(container, FifoCoordinator.newSelector(1), MzsConstants.RequestTimeout.INFINITE, null);
			Order currentOrder = orders.get(0);
			
			System.out.println("Got order from factory: " + currentOrder.getFactoryId());
			
			double avgOpenPackages = 0.;
			
			int minFactoryId = 0;
			int minPackageCount = Integer.MAX_VALUE;
			
			HashMap<Integer, Integer> openPackages = new HashMap<Integer, Integer>();
			
			for (Integer factory : factories.keySet()) {
				int numOpen = 0;
				/* First get all open packages */
				Order tpl = new Order();
				tpl.setState(Order.State.OPEN);
				ArrayList<Order> tmpList = capi.read(factories.get(factory), LindaCoordinator.newSelector(tpl, MzsConstants.Selecting.COUNT_ALL), MzsConstants.RequestTimeout.INFINITE, null);
				for (Order tmp : tmpList) {
					numOpen += tmp.getPackages();
				}
				
				/* Do the same for the in progess orders */
				tpl.setState(Order.State.IN_PROGRESS);
				tmpList = capi.read(factories.get(factory), LindaCoordinator.newSelector(tpl, MzsConstants.Selecting.COUNT_ALL), MzsConstants.RequestTimeout.INFINITE, null);
				for (Order tmp : tmpList) {
					numOpen += (tmp.getPackages() - tmp.getDonePackages());
				}
				
				/* Store result and prepare for avg calculation */
				openPackages.put(factory, numOpen);
				avgOpenPackages += (double) numOpen;
				
				/* Store the factory that has the minimum load */
				if (numOpen < minPackageCount) {
					minPackageCount = numOpen;
					minFactoryId = factory;
				}
			}
			
			avgOpenPackages = avgOpenPackages / (double) factories.size();

			System.out.println("Averange open packages per factory: " + avgOpenPackages);
			
			/* If we are 25% above average move otherwise stay in the same */
			if (openPackages.get(currentOrder.getFactoryId()) + currentOrder.getPackages() > avgOpenPackages * 1.25) {
				ContainerReference targetFactory = factories.get(currentOrder.getFactoryId());
				currentOrder.setState(Order.State.MOVED);
				currentOrder.setFactoryId(minFactoryId);
				capi.write(new Entry(currentOrder), targetFactory, MzsConstants.RequestTimeout.INFINITE, null);
				currentOrder.setState(Order.State.OPEN);
				capi.write(new Entry(currentOrder),  factories.get(minFactoryId), MzsConstants.RequestTimeout.INFINITE, null);
			}
			else {
				ContainerReference targetFactory = factories.get(currentOrder.getFactoryId());
				capi.write(new Entry(currentOrder), targetFactory, MzsConstants.RequestTimeout.INFINITE, null);
			}
			
		} catch (MzsCoreException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}


	
}
