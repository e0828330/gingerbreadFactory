package factory.spacesImpl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.mozartspaces.capi3.LindaCoordinator;
import org.mozartspaces.capi3.LindaCoordinator.LindaSelector;
import org.mozartspaces.core.Capi;
import org.mozartspaces.core.ContainerReference;
import org.mozartspaces.core.DefaultMzsCore;
import org.mozartspaces.core.MzsConstants;
import org.mozartspaces.core.MzsCore;
import org.mozartspaces.core.MzsCoreException;
import org.mozartspaces.core.TransactionReference;

import factory.entities.Ingredient;
import factory.factory.App;

public class Baker implements Runnable {

	private MzsCore core;
	private ExecutorService executor;
	
	private ArrayList<Ingredient> honey = new ArrayList<Ingredient>();
	private ArrayList<Ingredient> eggs = new ArrayList<Ingredient>();
	private ArrayList<Ingredient> flour = new ArrayList<Ingredient>();

	public Baker(MzsCore core, ExecutorService executor) {
		this.core = core;
		this.executor = executor;
	}

	/**
	 * This thread gets items (Ingredients) from the space
	 */
	private class ItemGetter implements Runnable {

		private ArrayList<Ingredient> resultEntries;
		private LindaSelector selector;
		private CountDownLatch sync;
		private TransactionReference tx;
		private Long timeout;

		public ItemGetter(LindaSelector selector, TransactionReference tx, Long timeout, CountDownLatch sync) {
			this.selector = selector;
			this.sync = sync;
			this.tx = tx;
			this.timeout = timeout;
		}
		
		public void run() {
			Capi capi = new Capi(core);
			try {
				ContainerReference container = capi.lookupContainer("ingredients", new URI(App.spaceURL), MzsConstants.RequestTimeout.INFINITE, this.tx);
				resultEntries = capi.take(container, selector, timeout, this.tx);
				sync.countDown();
				
				System.out.println(resultEntries.get(0));
				return;
				
			} catch (MzsCoreException e) {
				resultEntries = null;
			} catch (URISyntaxException e) {
				resultEntries = null;
			}
			resultEntries = null;
			sync.countDown();
		}

		public ArrayList<Ingredient> getResultEntries() {
			return resultEntries;
		}

	}

	/**
	 * Gets the next ingredient set (2 eggs, 1 flour, 1 honey) from the space
	 * 
	 * @param timeout
	 */
	private void getNextIngredientSet(Long timeout) {
		Capi capi = new Capi(core);
		try {
			TransactionReference tx = capi.createTransaction(MzsConstants.RequestTimeout.INFINITE, new URI(App.spaceURL));
			try {
				CountDownLatch sync = new CountDownLatch(3);
				
				ItemGetter getFlour = new ItemGetter(LindaCoordinator.newSelector(new Ingredient(null, null, Ingredient.Type.FLOUR), 1), tx, timeout, sync);
				ItemGetter getHoney = new ItemGetter(LindaCoordinator.newSelector(new Ingredient(null, null, Ingredient.Type.HONEY), 1), tx, timeout, sync);
				ItemGetter getEggs = new ItemGetter(LindaCoordinator.newSelector(new Ingredient(null, null, Ingredient.Type.EGG), 2), tx, timeout, sync);

				executor.execute(getFlour);
				executor.execute(getHoney);
				executor.execute(getEggs);
				
				sync.await();
				
				if (getFlour.getResultEntries() == null || getHoney.getResultEntries() == null || getEggs.getResultEntries() == null) {
					System.out.println("NOT ENOUGH ROLLBACK");
					capi.rollbackTransaction(tx); // Not enough ingredients
				}
				else {
					flour.addAll(getFlour.getResultEntries());
					honey.addAll(getHoney.getResultEntries());
					eggs.addAll(getEggs.getResultEntries());
					
				}
				
			} catch (InterruptedException e) {
				capi.rollbackTransaction(tx);
			}
		} catch (MzsCoreException e) {
			e.printStackTrace();
		} catch (URISyntaxException e1) {

		}
	}
	
	/**
	 * Returns the size of the current charge
	 * 
	 * @return
	 */
	private int getChargeSize() {
		return flour.size();
	}
	
	/**
	 * Generates a new gingerbread
	 */
	private void mixGingerbread() {
		// TODO
	}
	
	public void run() {

		while(true) {
			/* Get at least enough for one */
			getNextIngredientSet(MzsConstants.RequestTimeout.INFINITE);

			/* Try up to five */
			getNextIngredientSet(1000L);
			if (getChargeSize() == 2) {
				getNextIngredientSet(1000L);
			}
			if (getChargeSize() == 3) {
				getNextIngredientSet(1000L);
			}
			if (getChargeSize() == 4) {
				getNextIngredientSet(1000L);
			}
			
			System.out.println("SIZE: " + getChargeSize());
			break;
		}
		System.out.println("DONE");
		mixGingerbread();
	}
	
	public static void main(String[] args) throws MzsCoreException, InterruptedException, URISyntaxException {
		System.setProperty("mozartspaces.configurationFile", "mozartspaces-client.xml");
		MzsCore core = DefaultMzsCore.newInstanceWithoutSpace();
		ExecutorService executor = Executors.newCachedThreadPool();
		new Baker(core, executor).run();
	}

}
