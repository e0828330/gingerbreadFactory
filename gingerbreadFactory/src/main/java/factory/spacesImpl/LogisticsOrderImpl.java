package factory.spacesImpl;

import java.net.URI;
import java.net.URISyntaxException;

import org.mozartspaces.core.Capi;
import org.mozartspaces.core.ContainerReference;
import org.mozartspaces.core.DefaultMzsCore;
import org.mozartspaces.core.Entry;
import org.mozartspaces.core.MzsConstants;
import org.mozartspaces.core.MzsCore;
import org.mozartspaces.core.MzsCoreException;
import org.mozartspaces.core.TransactionReference;

import factory.entities.Order;
import factory.interfaces.LogisticsOrder;

public class LogisticsOrderImpl implements LogisticsOrder {

	private Order order;
	
	public void run() {
		System.setProperty("mozartspaces.configurationFile", "mozartspaces-client.xml");
		MzsCore core = DefaultMzsCore.newInstanceWithoutSpace();
		Capi capi = new Capi(core);
		try {
			ContainerReference container = capi.lookupContainer("orders", new URI(Server.spaceURL), MzsConstants.RequestTimeout.INFINITE, null);
			TransactionReference tx = capi.createTransaction(MzsConstants.RequestTimeout.INFINITE, new URI(Server.spaceURL));
			capi.write(new Entry(order), container, MzsConstants.RequestTimeout.INFINITE, tx);
			capi.commitTransaction(tx);
		} catch (MzsCoreException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		core.shutdown(false);
		
	}

	public void placeOrder(Order order) {
		this.order = order;
	}

}
