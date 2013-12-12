package factory.spacesImpl;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.mozartspaces.capi3.FifoCoordinator;
import org.mozartspaces.core.Capi;
import org.mozartspaces.core.ContainerReference;
import org.mozartspaces.core.DefaultMzsCore;
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
import factory.interfaces.EventListener;
import factory.interfaces.Monitor;

public class SpacesMonitor implements Monitor {

	private EventListener listener = null;
	private MzsCore core;
	private Capi capi;
	
	private ContainerReference ovenContainer;
	private ContainerReference gingerbreadContainer;
	private ContainerReference ingredientsContainer;
	
	public SpacesMonitor() {
		core = DefaultMzsCore.newInstanceWithoutSpace();
		capi = new Capi(core);
		try {
			ovenContainer = capi.lookupContainer("oven", new URI(App.spaceURL), MzsConstants.RequestTimeout.INFINITE, null);
			gingerbreadContainer = capi.lookupContainer("gingerbreads", new URI(App.spaceURL), MzsConstants.RequestTimeout.INFINITE, null);
			ingredientsContainer = capi.lookupContainer("ingredients", new URI(App.spaceURL), MzsConstants.RequestTimeout.INFINITE, null);
		} catch (MzsCoreException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
	
	
	private void setupNotifications() throws MzsCoreException, InterruptedException {
		NotificationManager notify = new NotificationManager(core);
		notify.createNotification(ovenContainer, new NotificationListener() {
			public void entryOperationFinished(Notification notify, Operation arg1, List<? extends Serializable> arg2) {
				System.out.println("OVEN CHANGED");
				if (SpacesMonitor.this.listener != null) {
					SpacesMonitor.this.listener.onOvenChanged(getOvenContent());
				}
			}
		}, Operation.WRITE, Operation.DELETE, Operation.TAKE);
		
		notify.createNotification(gingerbreadContainer, new NotificationListener() {
			public void entryOperationFinished(Notification notify, Operation arg1, List<? extends Serializable> arg2) {
				System.out.println("GINGERBREAD CHANGED");
				if (SpacesMonitor.this.listener != null) {
					SpacesMonitor.this.listener.onGingerBreadStateChange(getGingerBreads());
				}
			}
		}, Operation.WRITE, Operation.DELETE, Operation.TAKE);
		
		notify.createNotification(ingredientsContainer, new NotificationListener() {
			public void entryOperationFinished(Notification notify, Operation arg1, List<? extends Serializable> arg2) {
				System.out.println("INGREDIENTS CHANGED");
				if (SpacesMonitor.this.listener != null) {
					SpacesMonitor.this.listener.onIngredientChanged(getIngredients());
				}
			}
		}, Operation.WRITE, Operation.DELETE, Operation.TAKE);
	}
	
	public void setListener(EventListener listener) {
		this.listener = listener;
		try {
			setupNotifications();
		} catch (MzsCoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public List<GingerBread> getGingerBreads() {
		List<GingerBread> result = null;
		try {
			System.out.println("getGB");
			result = capi.read(gingerbreadContainer, FifoCoordinator.newSelector(MzsConstants.Selecting.COUNT_ALL), MzsConstants.RequestTimeout.INFINITE, null);
		} catch (MzsCoreException e) {
			e.printStackTrace();
		}
		return result;
	}

	public List<Ingredient> getIngredients() {
		List<Ingredient> result = null;
		try {
			result = capi.read(ingredientsContainer, FifoCoordinator.newSelector(MzsConstants.Selecting.COUNT_ALL), MzsConstants.RequestTimeout.INFINITE, null);
		} catch (MzsCoreException e) {
			e.printStackTrace();
		}
		return result;
	}

	public List<GingerBread> getOvenContent() {
		List<GingerBread> result = null;
		try {
			result = capi.read(ovenContainer, FifoCoordinator.newSelector(MzsConstants.Selecting.COUNT_ALL), MzsConstants.RequestTimeout.INFINITE, null);
		} catch (MzsCoreException e) {
			e.printStackTrace();
		}
		return result;
	}

}
