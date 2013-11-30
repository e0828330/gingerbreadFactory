package factory.spacesImpl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

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
import factory.factory.App;

public class LogisticsEmployee {
	
	private MzsCore core;
	private Long id = 1L; // TODO: Set at startup
	
	public LogisticsEmployee(MzsCore core) {
		this.core = core;
	}
	
	public void run() {
		Capi capi = new Capi(core);
		while (true) {
			try {
				ContainerReference gingerbreadsContainer = capi.lookupContainer("gingerbreads", new URI(App.spaceURL), MzsConstants.RequestTimeout.INFINITE, null);
				ContainerReference qaPassedContainer = capi.lookupContainer("qaPassed", new URI(App.spaceURL), MzsConstants.RequestTimeout.INFINITE, null);
				
				System.out.println("got containers");
				
				TransactionReference tx = capi.createTransaction(MzsConstants.RequestTimeout.INFINITE, new URI(App.spaceURL));
				
				System.out.println("got tx");
				// Get next 6
				ArrayList<Long> ids = capi.take(qaPassedContainer, FifoCoordinator.newSelector(), MzsConstants.RequestTimeout.INFINITE, tx);
				System.out.println(ids);
				for (Long gid : ids) {
					GingerBread current = null;
					GingerBread tpl = new GingerBread();
				//	tpl.setChargeId(gid);
					//tpl.setState(State.CONTROLLED);
					// Get, update state and write back
					System.out.println("QUERY");
					current = (GingerBread) capi.take(gingerbreadsContainer, LindaCoordinator.newSelector(tpl), MzsConstants.RequestTimeout.INFINITE, tx).get(0);
					System.out.println("RES");
					current.setLogisticsId(id);
					current.setState(State.DONE);
					capi.write(new Entry(current), gingerbreadsContainer, MzsConstants.RequestTimeout.INFINITE, tx);
					System.out.println("delivered: " + current.getId());
				}
				
				try {
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
		new LogisticsEmployee(core).run();
	}
}
