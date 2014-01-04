package factory.spacesImpl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.TreeSet;

import org.mozartspaces.capi3.FifoCoordinator;
import org.mozartspaces.capi3.LindaCoordinator;
import org.mozartspaces.core.Capi;
import org.mozartspaces.core.ContainerReference;
import org.mozartspaces.core.DefaultMzsCore;
import org.mozartspaces.core.Entry;
import org.mozartspaces.core.MzsConstants;
import org.mozartspaces.core.MzsCore;
import org.mozartspaces.core.MzsCoreException;
import org.mozartspaces.core.TransactionReference;

import factory.entities.GingerBread;
import factory.entities.GingerBread.State;
import factory.entities.Order;
import factory.utils.Utils;

public class LogisticsEmployee {
	
	private MzsCore core;
	private Long id = 1L;
	private TreeSet<Order> orderQueue;
	
	public LogisticsEmployee(MzsCore core, Long id) {
		this.core = core;
		this.id = id;
		
		this.orderQueue = new TreeSet<Order>(new Comparator<Order>() {
			public int compare(Order a, Order b) {
				if (a.getState().equals(b.getState())) {
					return (int)(a.getTimestamp() - b.getTimestamp());
				}
				else if (a.getState().equals(Order.State.IN_PROGRESS)) {
					return 1;
				}
				else {
					return -1;
				}
			}
		});

	}
	
	public void run() {
		Capi capi = new Capi(core);
		
		// TODO: Pickup old work
		
		ContainerReference gingerbreadsContainer = null;
		ContainerReference qaPassedContainer = null;
		ContainerReference orderContainer = null;
		try {
			gingerbreadsContainer = capi.lookupContainer("gingerbreads", new URI(Server.spaceURL), MzsConstants.RequestTimeout.INFINITE, null);
			qaPassedContainer = capi.lookupContainer("qaPassed", new URI(Server.spaceURL), MzsConstants.RequestTimeout.INFINITE, null);
			orderContainer = capi.lookupContainer("orders", new URI(Server.spaceURL), MzsConstants.RequestTimeout.INFINITE, null);
		} catch (MzsCoreException e1) {
			e1.printStackTrace();
			System.exit(1);
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
			System.exit(1);
		}

		while (true) {
			try {
				// Wait for next event
				capi.take(qaPassedContainer, FifoCoordinator.newSelector(1), MzsConstants.RequestTimeout.INFINITE, null);
				System.out.println("GOT EVENT");
				
				TransactionReference tx = capi.createTransaction(MzsConstants.RequestTimeout.INFINITE, new URI(Server.spaceURL));
				
				orderQueue.clear();
				
				// Do we have new Orders ?
				Order emptyOrder = new Order();
				emptyOrder.setState(Order.State.OPEN);
				ArrayList<Order> orders = capi.take(orderContainer, LindaCoordinator.newSelector(emptyOrder, MzsConstants.Selecting.COUNT_ALL), MzsConstants.RequestTimeout.INFINITE, tx);
				orderQueue.addAll(orders);
				
				System.out.println("Open orders :" + orders.size());

				// Do we have old (in progress) orders
				Order oldOrder = new Order();
				oldOrder.setState(Order.State.IN_PROGRESS);
				oldOrder.setLogisticsId(id);
				orders = capi.take(orderContainer, LindaCoordinator.newSelector(oldOrder, MzsConstants.Selecting.COUNT_ALL), MzsConstants.RequestTimeout.INFINITE, tx);
				orderQueue.addAll(orders);
				
				System.out.println("In progress orders :" + orders.size());
				
				GingerBread tpl = new GingerBread();
				tpl.setState(State.CONTROLLED);
				
				ArrayList<GingerBread> inStock = capi.take(gingerbreadsContainer, LindaCoordinator.newSelector(tpl, MzsConstants.Selecting.COUNT_ALL), MzsConstants.RequestTimeout.INFINITE, tx);
				
				System.out.println(inStock.size());
				
				LinkedList<GingerBread> normalInStock = new LinkedList<GingerBread>();
				LinkedList<GingerBread> nutInStock = new LinkedList<GingerBread>();
				LinkedList<GingerBread> chocolateInStock = new LinkedList<GingerBread>();
				
				for (GingerBread tmp : inStock) {
					switch(tmp.getFlavor()) {
					case NORMAL:
						normalInStock.add(tmp);
						break;
					case NUT:
						nutInStock.add(tmp);
						break;
					case CHOCOLATE:
						chocolateInStock.add(tmp);
						break;
					}
				}
				
				ArrayList<GingerBread> pack = new ArrayList<GingerBread>();
				
				boolean fallback = true; // Set to true when we have no order to process
				Long orderId = null;
				for (Order tmpOrder : orderQueue) {
					if (normalInStock.size() >= tmpOrder.getNumNormal() && nutInStock.size() >= tmpOrder.getNumNut() && chocolateInStock.size() >= tmpOrder.getNumChocolate()) {
						System.out.println("PROCESSING ORDER");
						for (int i = 0; i < tmpOrder.getNumNormal(); i++) {
							pack.add(normalInStock.pop());
						}
						for (int i = 0; i < tmpOrder.getNumNut(); i++) {
							pack.add(nutInStock.pop());
						}
						for (int i = 0; i < tmpOrder.getNumChocolate(); i++) {
							pack.add(chocolateInStock.pop());
						}	
						fallback = false;

						if (tmpOrder.getDonePackages() == null) {
							tmpOrder.setDonePackages(1);
						}
						else {
							tmpOrder.setDonePackages(tmpOrder.getDonePackages() + 1);
						}
	
						if (tmpOrder.getDonePackages() == tmpOrder.getPackages()) {
							tmpOrder.setState(Order.State.DONE);
						}
						else {
							tmpOrder.setState(Order.State.IN_PROGRESS);
						}
						tmpOrder.setLogisticsId(id);
						orderId = tmpOrder.getId();
						break;
					}
				}
			
				
				if (fallback) {
					// First case 2 of each
					if (normalInStock.size() >= 2 && nutInStock.size() >= 2 && chocolateInStock.size() >= 2) {
						pack.add(normalInStock.pop());
						pack.add(normalInStock.pop());
						pack.add(nutInStock.pop());
						pack.add(nutInStock.pop());
						pack.add(chocolateInStock.pop());
						pack.add(chocolateInStock.pop());
					}
					// 3 normal and 3 nut
					else if (normalInStock.size() >= 3 && nutInStock.size() >= 3) {
						pack.add(normalInStock.pop());
						pack.add(normalInStock.pop());
						pack.add(normalInStock.pop());
						pack.add(nutInStock.pop());
						pack.add(nutInStock.pop());
						pack.add(nutInStock.pop());
					}
					// 3 normal and 3 chocolate
					else if (normalInStock.size() >= 3 && chocolateInStock.size() >= 3) {
						pack.add(normalInStock.pop());
						pack.add(normalInStock.pop());
						pack.add(normalInStock.pop());
						pack.add(chocolateInStock.pop());
						pack.add(chocolateInStock.pop());
						pack.add(chocolateInStock.pop());
					}
					// 3 nut and 3 chocolate
					else if (normalInStock.size() >= 3 && chocolateInStock.size() >= 3) {
						pack.add(nutInStock.pop());
						pack.add(nutInStock.pop());
						pack.add(nutInStock.pop());
						pack.add(chocolateInStock.pop());
						pack.add(chocolateInStock.pop());
						pack.add(chocolateInStock.pop());
					}
					// 6 normal
					else if (normalInStock.size() >= 6) {
						pack.add(normalInStock.pop());
						pack.add(normalInStock.pop());
						pack.add(normalInStock.pop());
						pack.add(normalInStock.pop());
						pack.add(normalInStock.pop());
						pack.add(normalInStock.pop());
					}
					// 6 nut
					else if (nutInStock.size() >= 6) {
						pack.add(nutInStock.pop());
						pack.add(nutInStock.pop());
						pack.add(nutInStock.pop());
						pack.add(nutInStock.pop());
						pack.add(nutInStock.pop());
						pack.add(nutInStock.pop());
					}
					// 6 chocolate
					else if (chocolateInStock.size() >= 6) {
						pack.add(chocolateInStock.pop());
						pack.add(chocolateInStock.pop());
						pack.add(chocolateInStock.pop());
						pack.add(chocolateInStock.pop());
						pack.add(chocolateInStock.pop());
						pack.add(chocolateInStock.pop());
					}
					// Out of stock lets wait
					else {
						System.out.println("NOT IN STOCK WAIT...");
						capi.rollbackTransaction(tx);
						continue;
					}
				}
				
				// Build the package
				Long packageId = Utils.getID();
				for(GingerBread current : pack) {
					current.setLogisticsId(id);
					current.setPackageId(packageId);
					current.setOrderId(orderId);
					current.setState(State.DONE);
					capi.write(new Entry(current), gingerbreadsContainer, MzsConstants.RequestTimeout.INFINITE, tx);
				}
				// Write back what we didn't need
				ArrayList<GingerBread> unused = new ArrayList<GingerBread>();
				unused.addAll(normalInStock);
				unused.addAll(nutInStock);
				unused.addAll(chocolateInStock);
				
				for (GingerBread current : unused) {
					capi.write(new Entry(current), gingerbreadsContainer, MzsConstants.RequestTimeout.INFINITE, tx);
				}
				
				// Write back orders
				for (Order current : orderQueue) {
					capi.write(new Entry(current), orderContainer, MzsConstants.RequestTimeout.INFINITE, tx);
				}
				
				try {
					System.out.println("Done packing ...");
					capi.commitTransaction(tx);
				}
				catch (MzsCoreException e) {
					capi.rollbackTransaction(tx);
				}
				
			} catch (MzsCoreException e) {
				e.printStackTrace();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) {
		System.setProperty("mozartspaces.configurationFile", "mozartspaces-client.xml");
		MzsCore core = DefaultMzsCore.newInstanceWithoutSpace();
		new LogisticsEmployee(core, Utils.getStartupId(args)).run();
	}
}
