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

import factory.entities.Ingredient;
import factory.entities.Ingredient.Type;
import factory.interfaces.Supplier;
import factory.utils.Utils;

public class SupplierImpl implements Supplier {

	private Long id;
	private int amount;
	private Type type;
	
	public void run() {
		System.setProperty("mozartspaces.configurationFile", "mozartspaces-client.xml");
		MzsCore core = DefaultMzsCore.newInstanceWithoutSpace();
		Capi capi = new Capi(core);
		try {
			ContainerReference container = capi.lookupContainer("ingredients", new URI(Server.spaceURL), MzsConstants.RequestTimeout.INFINITE, null);
			for (int i = 0; i < amount; i++) {
				// Using a transaction here makes no sense but fixes notify while baker is running
				TransactionReference tx = capi.createTransaction(MzsConstants.RequestTimeout.INFINITE, new URI(Server.spaceURL));
				Ingredient item =  new Ingredient(id, Utils.getID(), type);
				capi.write(new Entry(item), container, MzsConstants.RequestTimeout.INFINITE, tx);
				capi.commitTransaction(tx);
				//System.out.println("Unloaded:" + item);
				if (!Server.BENCHMARK) {
					Thread.sleep(Utils.getRandomWaitTime());
				}
			}
		} catch (MzsCoreException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		core.shutdown(false);
	}

	public void setId(Long id) {
		this.id = id;
		
	}

	public void placeOrder(int amount, Type type) {
		this.amount = amount;
		this.type = type;
	}

}
