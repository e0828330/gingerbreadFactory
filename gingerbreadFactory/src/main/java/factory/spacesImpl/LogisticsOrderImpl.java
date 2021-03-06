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

import factory.entities.Order;
import factory.interfaces.LogisticsOrder;

public class LogisticsOrderImpl implements LogisticsOrder {

	private Order order;
	
	public void run() {
		System.setProperty("mozartspaces.configurationFile", "mozartspaces-client.xml");
		MzsCore core = DefaultMzsCore.newInstanceWithoutSpace();
		Capi capi = new Capi(core);
		try {
			ContainerReference container = capi.lookupContainer("ordersLB", new URI(LoadBalancer.loadBalancerURL), MzsConstants.RequestTimeout.INFINITE, null);
			capi.write(new Entry(order), container, MzsConstants.RequestTimeout.INFINITE, null);
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
