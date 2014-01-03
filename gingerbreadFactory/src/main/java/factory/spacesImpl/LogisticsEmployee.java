package factory.spacesImpl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedList;

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
import factory.utils.Utils;

public class LogisticsEmployee {
	
	private MzsCore core;
	private Long id = 1L;
	
	public LogisticsEmployee(MzsCore core, Long id) {
		this.core = core;
		this.id = id;
	}
	
	public void run() {
		Capi capi = new Capi(core);
		
		ContainerReference gingerbreadsContainer = null;
		ContainerReference qaPassedContainer = null;
		try {
			gingerbreadsContainer = capi.lookupContainer("gingerbreads", new URI(Server.spaceURL), MzsConstants.RequestTimeout.INFINITE, null);
			qaPassedContainer = capi.lookupContainer("qaPassed", new URI(Server.spaceURL), MzsConstants.RequestTimeout.INFINITE, null);
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
				
				// Build the package
				Long packageId = Utils.getID();
				for(GingerBread current : pack) {
					current.setLogisticsId(id);
					current.setPackageId(packageId);
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
