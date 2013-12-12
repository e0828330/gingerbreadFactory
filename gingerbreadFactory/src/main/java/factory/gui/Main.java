package factory.gui;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.mozartspaces.capi3.FifoCoordinator;
import org.mozartspaces.core.Capi;
import org.mozartspaces.core.ContainerReference;
import org.mozartspaces.core.DefaultMzsCore;
import org.mozartspaces.core.Entry;
import org.mozartspaces.core.MzsConstants;
import org.mozartspaces.core.MzsCore;
import org.mozartspaces.core.MzsCoreException;
import org.mozartspaces.notifications.Notification;
import org.mozartspaces.notifications.NotificationListener;
import org.mozartspaces.notifications.NotificationManager;
import org.mozartspaces.notifications.Operation;

import factory.entities.GingerBread;
import factory.entities.Ingredient;
import factory.factory.App;
import factory.interfaces.Supplier;
import factory.spacesImpl.SupplierImpl;

public class Main {

	public static void main(String[] args) throws InterruptedException {
		// TODO: Implement GUI, dummy for now
		for (int i = 0; i < 5; i++) {
			Supplier supplier = new SupplierImpl();
			supplier.setId(1L);
			supplier.placeOrder(5, Ingredient.Type.FLOUR);
			new Thread(supplier).start();
	
			Supplier supplier2 = new SupplierImpl();
			supplier2.setId(2L);
			supplier2.placeOrder(4, Ingredient.Type.EGG);
			new Thread(supplier2).start();
	
			Supplier supplier3 = new SupplierImpl();
			supplier3.setId(3L);
			supplier3.placeOrder(4, Ingredient.Type.HONEY);
			new Thread(supplier3).start();
		}

		System.setProperty("mozartspaces.configurationFile", "mozartspaces-client.xml");
		MzsCore core = DefaultMzsCore.newInstanceWithoutSpace();
		final Capi capi = new Capi(core);

		try {
			final ContainerReference ovenContainer = capi.lookupContainer("oven", new URI(App.spaceURL), MzsConstants.RequestTimeout.INFINITE, null);
			NotificationManager notify = new NotificationManager(core);
			notify.createNotification(ovenContainer, new NotificationListener() {
				public void entryOperationFinished(Notification notify, Operation arg1, List<? extends Serializable> arg2) {
					System.out.println("OVEN CHANGED");
					try {
						List<GingerBread> ovenContent = capi.read(ovenContainer, FifoCoordinator.newSelector(MzsConstants.Selecting.COUNT_ALL), MzsConstants.RequestTimeout.INFINITE, null);
						for (GingerBread bread : ovenContent) {
							System.out.println("In oven: [" + bread + "]");
						}
						System.out.println("----");
					} catch (MzsCoreException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}, Operation.WRITE, Operation.DELETE, Operation.TAKE);

			final ContainerReference gingerBreadContainer = capi.lookupContainer("gingerbreads", new URI(App.spaceURL), MzsConstants.RequestTimeout.INFINITE, null);
			NotificationManager notify2 = new NotificationManager(core);
			notify2.createNotification(gingerBreadContainer, new NotificationListener() {
				public void entryOperationFinished(Notification notify, Operation arg1, List<? extends Serializable> arg2) {
					System.out.println("GINGERBREAD CHANGED");
					try {
						List<GingerBread> gingerBreadContent = capi.read(gingerBreadContainer, FifoCoordinator.newSelector(MzsConstants.Selecting.COUNT_ALL), MzsConstants.RequestTimeout.INFINITE, null);
						for (GingerBread bread : gingerBreadContent) {
							System.out.println("In gingerBread: [" + bread + "]");
						}
						System.out.println("----");
					} catch (MzsCoreException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}, Operation.WRITE, Operation.DELETE, Operation.TAKE);

		} catch (MzsCoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
