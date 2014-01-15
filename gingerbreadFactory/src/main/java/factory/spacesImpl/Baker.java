package factory.spacesImpl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import factory.entities.GingerBread.Flavor;
import factory.entities.GingerBread.State;
import factory.entities.Ingredient;
import factory.utils.Utils;

public class Baker {

	private Capi capi;
	private ExecutorService executor;
	private Long id;
	
	private LinkedList<Ingredient> honey = new LinkedList<Ingredient>();
	private LinkedList<Ingredient> eggs = new LinkedList<Ingredient>();
	private LinkedList<Ingredient> flour = new LinkedList<Ingredient>();

	private ContainerReference ingredientsContainer;
	private ContainerReference gingerbreadsContainer;
	private ContainerReference ovenContainer;
	private ContainerReference chargeContainer;
	
	public Baker(MzsCore core, ExecutorService executor, Long id) {
		this.capi = new Capi(core);
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
			try {
				resultEntries = capi.take(ingredientsContainer, selector, timeout, this.tx);
				sync.countDown();
				
				return;
			} catch (MzsCoreException e) {
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
					capi.rollbackTransaction(tx); // Not enough ingredients
				}
				else {
					flour.addAll(getFlour.getResultEntries());
					honey.addAll(getHoney.getResultEntries());
					eggs.addAll(getEggs.getResultEntries());
					capi.commitTransaction(tx);
					
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
	 * Returns the next flavor to produce (if possible)
	 * @return
	 */
	private Flavor getNextFlavor() {
		ArrayList<Flavor> list = new ArrayList<Flavor>(3);
		list.addAll(Arrays.asList(Flavor.values()));
		Collections.shuffle(list);
		return list.get(0);
	}
	
	/**
	 * Tries to get a special (nut or chocolate) ingredient
	 * 
	 * @param flavor
	 * @return
	 */
	private Ingredient getSpecialIngredient(Flavor flavor) {
		ArrayList<Ingredient> resultEntries = null;
		try {
			switch(flavor) {
			case CHOCOLATE:
				resultEntries = capi.take(ingredientsContainer, LindaCoordinator.newSelector(new Ingredient(null, null, Ingredient.Type.CHOCOLATE), 1), 1000, null);
				break;
			case NUT:
				resultEntries = capi.take(ingredientsContainer, LindaCoordinator.newSelector(new Ingredient(null, null, Ingredient.Type.NUT), 1), 1000, null);
				break;
			default:
				resultEntries = null;
			}
		} catch (MzsCoreException e) {
		}

		return resultEntries == null ? null: resultEntries.get(0);
	}
	

	/**
	 * Produces a new charge
	 * @param chargeId
	 * @return
	 * @throws MzsCoreException
	 * @throws URISyntaxException
	 */
	private ArrayList<GingerBread> produceCharge(Long chargeId) throws MzsCoreException, URISyntaxException {
		int size = getChargeSize();
		
		ArrayList<GingerBread> currentCharge = new ArrayList<GingerBread>(size);
		ArrayList<Entry> entries = new ArrayList<Entry>(size);
		
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
			
			Flavor flavor = getNextFlavor();
			Ingredient special = getSpecialIngredient(flavor);
			if (special != null) {
				switch(flavor) {
				case NUT:
					tmp.setFlavor(flavor);
					tmp.setNutId(special.getId());
					tmp.setNutSupplierId(special.getSupplierId());
					break;
				case CHOCOLATE:
					tmp.setFlavor(flavor);
					tmp.setChocolateId(special.getId());
					tmp.setChocolateSupplierId(special.getSupplierId());
					break;
				default:
					// Cant happen
				}
			}
			else {
				tmp.setFlavor(Flavor.NORMAL);
			}
			
			entries.add(new Entry(tmp));
			currentCharge.add(tmp);
		}
		
		capi.write(entries, gingerbreadsContainer);
		
		return currentCharge;
	}
	
	/**
	 * Bakes a charge in oven
	 * 
	 * @param currentCharge
	 * @return
	 * @throws MzsCoreException
	 * @throws URISyntaxException
	 */
	private ArrayList<GingerBread> bakeCharge(ArrayList<GingerBread> currentCharge) throws MzsCoreException, URISyntaxException {
		boolean baked = false;		
		
		ArrayList<Entry> chargeEntries = new ArrayList<Entry>(currentCharge.size());
		for (GingerBread tmp : currentCharge)  {
			chargeEntries.add(new Entry(tmp, KeyCoordinator.newCoordinationData(tmp.getId().toString())));
		}
		
		do {
			TransactionReference tx = capi.createTransaction(MzsConstants.RequestTimeout.INFINITE, new URI(Server.spaceURL));
			capi.write(chargeEntries, ovenContainer, MzsConstants.RequestTimeout.INFINITE, tx);
			try {
				capi.commitTransaction(tx);
			}
			catch (MzsCoreException e) {
				capi.rollbackTransaction(tx);
				continue;
			}
			if (!Server.BENCHMARK) {
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
	
				}
			}
			baked = true;
		}
		while (baked == false);

		return currentCharge;
	}
	
	/**
	 * Gets backed gingerbreads from oven
	 * 
	 * @param currentCharge
	 * @throws MzsCoreException
	 * @throws URISyntaxException
	 */
	private void getFromOven(ArrayList<GingerBread> currentCharge) throws MzsCoreException, URISyntaxException {		
		TransactionReference tx = capi.createTransaction(MzsConstants.RequestTimeout.INFINITE, new URI(Server.spaceURL));
		try {
			ArrayList<GingerBread> items;
			for (GingerBread tmp : currentCharge)  {
				GingerBread current = null;
				// Remove from oven
				items = capi.take(ovenContainer, KeyCoordinator.newSelector(tmp.getId().toString()), MzsConstants.RequestTimeout.INFINITE, tx);
				current = items.get(0);
				// Remove from list
				capi.take(gingerbreadsContainer, LindaCoordinator.newSelector(current), MzsConstants.RequestTimeout.INFINITE, tx);
				current.setState(State.BAKED);
				capi.write(new Entry(current), gingerbreadsContainer, MzsConstants.RequestTimeout.INFINITE, tx);
			}
			capi.write(chargeContainer, new Entry(currentCharge.get(0).getChargeId()));
			capi.commitTransaction(tx);
			currentCharge.clear();
		}
		catch (MzsCoreException e) {
			capi.rollbackTransaction(tx);
		}
	}
	
	/**
	 * Do the work (pick up ingredients, produce and bake)
	 */
	private void doWork() {
		while (true) {
			while(true) {
				/* Get at least enough for one */
				getNextIngredientSet(MzsConstants.RequestTimeout.INFINITE);
	
				/* Try up to five */
				getNextIngredientSet(0L);
				if (getChargeSize() == 2) {
					getNextIngredientSet(0L);
				}
				if (getChargeSize() == 3) {
					getNextIngredientSet(0L);
				}
				if (getChargeSize() == 4) {
					getNextIngredientSet(0L);
				}
				break;
			}
			try {
				ArrayList<GingerBread> charge = produceCharge(Utils.getID());
				charge = bakeCharge(charge);
				getFromOven(charge);
			} catch (MzsCoreException e) {
				e.printStackTrace();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Entry point picks up crashes and starts the work
	 */
	public void run() {
		// Pick up unfinished tasks if nay
		try {
			ingredientsContainer = capi.lookupContainer("ingredients", new URI(Server.spaceURL), MzsConstants.RequestTimeout.INFINITE, null);
			gingerbreadsContainer = capi.lookupContainer("gingerbreads", new URI(Server.spaceURL), MzsConstants.RequestTimeout.INFINITE, null);
			ovenContainer = capi.lookupContainer("oven", new URI(Server.spaceURL), MzsConstants.RequestTimeout.INFINITE, null);
			chargeContainer = capi.lookupContainer("charges", new URI(Server.spaceURL), MzsConstants.RequestTimeout.INFINITE, null);

			ArrayList<GingerBread> list;

			// Do we have something left in oven?
			GingerBread tpl = new GingerBread();
			tpl.setBakerId(id);
			try {
				list = capi.read(ovenContainer, LindaCoordinator.newSelector(tpl, MzsConstants.Selecting.COUNT_ALL), 1000, null);
			}
			catch (MzsCoreException e) {
				list = new ArrayList<GingerBread>();
			}
			if (!list.isEmpty()) {
				System.out.println("Picking up oven work");
				getFromOven(list);
			}
			else {
				// Do we have something produced that should be baked?
				tpl.setState(State.PRODUCED);
				try {
					list = capi.read(gingerbreadsContainer, LindaCoordinator.newSelector(tpl, MzsConstants.Selecting.COUNT_ALL), 1000, null);
				}
				catch (MzsCoreException e) {
					list = new ArrayList<GingerBread>();
				}
				if (!list.isEmpty()) {
					System.out.println("Picking up produced gingerbreads");
					ArrayList<GingerBread> charge = bakeCharge(list);
					getFromOven(charge);
				}
			}
			
		} catch (MzsCoreException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		// Start working
		doWork();
	}

	public static void main(String[] args) throws MzsCoreException, InterruptedException, URISyntaxException {
		System.setProperty("mozartspaces.configurationFile", "mozartspaces-client.xml");
		MzsCore core = DefaultMzsCore.newInstanceWithoutSpace();
		SpaceUtils.parseFactoryID(args, 1);
		ExecutorService executor = Executors.newCachedThreadPool();
		SpaceUtils.setupBenchmark(core); // Only for benchmark
		new Baker(core, executor, Utils.getStartupId(args)).run();
	}

}
