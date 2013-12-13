package factory.spacesImpl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.mozartspaces.capi3.KeyCoordinator;
import org.mozartspaces.capi3.LindaCoordinator;
import org.mozartspaces.capi3.LindaCoordinator.LindaSelector;
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
import factory.entities.Ingredient;
import factory.utils.Utils;

public class Baker {

	private MzsCore core;
	private ExecutorService executor;
	private Long id;
	
	private LinkedList<Ingredient> honey = new LinkedList<Ingredient>();
	private LinkedList<Ingredient> eggs = new LinkedList<Ingredient>();
	private LinkedList<Ingredient> flour = new LinkedList<Ingredient>();

	public Baker(MzsCore core, ExecutorService executor, Long id) {
		this.core = core;
		this.executor = executor;
		this.id = id;
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
				ContainerReference container = capi.lookupContainer("ingredients", new URI(Server.spaceURL), MzsConstants.RequestTimeout.INFINITE, this.tx);
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
			TransactionReference tx = capi.createTransaction(MzsConstants.RequestTimeout.INFINITE, new URI(Server.spaceURL));
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
	

	private void processCharge(Long chargeId) throws MzsCoreException, URISyntaxException {
		int size = getChargeSize();
		Capi capi = new Capi(core);
		ContainerReference gingerbreadsContainer = capi.lookupContainer("gingerbreads", new URI(Server.spaceURL), MzsConstants.RequestTimeout.INFINITE, null);
		
		ArrayList<GingerBread> currentCharge = new ArrayList<GingerBread>(size);
		
		for (int i = 0; i < size; i++) {
			GingerBread tmp = new GingerBread();
			tmp.setId(Utils.getID());
			tmp.setBakerId(id);
			tmp.setChargeId(chargeId);
			tmp.setFlourSupplierId(flour.peek().getSupplierId());
			tmp.setHoneySupplierId(honey.peek().getSupplierId());
			tmp.setFlourId(flour.poll().getId());
			tmp.setHoneyId(honey.poll().getId());
			tmp.setFirstEggSupplierId(eggs.peek().getSupplierId());
			tmp.setFirstEggId(eggs.poll().getId());
			tmp.setSecondEggSupplierId(eggs.peek().getSupplierId());
			tmp.setSecondEggId(eggs.poll().getId());
			tmp.setState(GingerBread.State.PRODUCED);
			capi.write(gingerbreadsContainer, new Entry(tmp));
			currentCharge.add(tmp);
		}
		
		boolean baked = false;	
		
		ContainerReference ovenContainer = capi.lookupContainer("oven", new URI(Server.spaceURL), MzsConstants.RequestTimeout.INFINITE, null);
		do {
			TransactionReference tx = capi.createTransaction(MzsConstants.RequestTimeout.INFINITE, new URI(Server.spaceURL));
			for (GingerBread tmp : currentCharge)  {
				capi.write(new Entry(tmp, KeyCoordinator.newCoordinationData(tmp.getId().toString())), ovenContainer, MzsConstants.RequestTimeout.INFINITE, tx);
			}
			try {
				capi.commitTransaction(tx);
			}
			catch (MzsCoreException e) {
				capi.rollbackTransaction(tx);
				continue;
			}
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {

			}
			baked = true;
		}
		while (baked == false);
		
		TransactionReference tx = capi.createTransaction(MzsConstants.RequestTimeout.INFINITE, new URI(Server.spaceURL));
		try {
			ArrayList<GingerBread> items;
			for (GingerBread tmp : currentCharge)  {
				GingerBread current = null;
				// Remove from oven
				items = capi.take(ovenContainer, KeyCoordinator.newSelector(tmp.getId().toString()), MzsConstants.RequestTimeout.INFINITE, tx);
				current = items.get(0);
				// Remove from list
				capi.take(gingerbreadsContainer, LindaCoordinator.newSelector(current), MzsConstants.RequestTimeout.INFINITE, tx);;
				current.setState(State.BAKED);
				capi.write(new Entry(current), gingerbreadsContainer, MzsConstants.RequestTimeout.INFINITE, tx);
			}
			capi.commitTransaction(tx);
			currentCharge.clear();
		}
		catch (MzsCoreException e) {
			capi.rollbackTransaction(tx);
		}

		ContainerReference chargeContainer = capi.lookupContainer("charges", new URI(Server.spaceURL), MzsConstants.RequestTimeout.INFINITE, null);
		capi.write(chargeContainer, new Entry(chargeId));
		System.out.println("ADDING " + chargeId + " to charges container");
	}
	
	public void run() {

		// TODO: Pick up unfinished tasks
		
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
		try {
			processCharge(Utils.getID());
			run(); // Restart loop
		} catch (MzsCoreException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws MzsCoreException, InterruptedException, URISyntaxException {
		System.setProperty("mozartspaces.configurationFile", "mozartspaces-client.xml");
		MzsCore core = DefaultMzsCore.newInstanceWithoutSpace();
		ExecutorService executor = Executors.newCachedThreadPool();
		new Baker(core, executor, Utils.getStartupId(args)).run();
	}

}
