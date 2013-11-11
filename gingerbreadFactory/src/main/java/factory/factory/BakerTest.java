package factory.factory;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import org.mozartspaces.capi3.LindaCoordinator;
import org.mozartspaces.capi3.LindaCoordinator.LindaSelector;
import org.mozartspaces.core.Capi;
import org.mozartspaces.core.ContainerReference;
import org.mozartspaces.core.MzsConstants;
import org.mozartspaces.core.MzsCore;
import org.mozartspaces.core.MzsCoreException;
import org.mozartspaces.core.TransactionReference;

import factory.entities.Ingredient;

public class BakerTest implements Runnable {

	private MzsCore core;
	private ExecutorService executor;

	public BakerTest(MzsCore core, ExecutorService executor) {
		this.core = core;
		this.executor = executor;
	}

	private class ItemGetter implements Runnable {

		private ArrayList<Ingredient> resultEntries;
		private LindaSelector selector;
		private CountDownLatch sync;
		
		public ItemGetter(LindaSelector selector, CountDownLatch sync) {
			this.selector = selector;
			this.sync = sync;
		}
		
		public void run() {
			Capi capi = new Capi(core);
			try {
				ContainerReference container = capi.lookupContainer("ingredients");
				resultEntries = capi.take(container, selector, MzsConstants.RequestTimeout.INFINITE, null);
				sync.countDown();
			} catch (MzsCoreException e) {
				e.printStackTrace();
			}

		}

		public ArrayList<Ingredient> getResultEntries() {
			return resultEntries;
		}

	}

	public void run() {
		Capi capi = new Capi(core);
		try {
			TransactionReference tx = capi.createTransaction(MzsConstants.RequestTimeout.INFINITE, null);

			System.out.println("=== START -- QUERY =====");

			CountDownLatch sync = new CountDownLatch(3);
			
			ItemGetter getFlour = new ItemGetter(LindaCoordinator.newSelector(new Ingredient(null, Ingredient.Type.FLOUR), 1), sync);
			ItemGetter getHoney = new ItemGetter(LindaCoordinator.newSelector(new Ingredient(null, Ingredient.Type.HONEY), 1), sync);
			ItemGetter getEggs = new ItemGetter(LindaCoordinator.newSelector(new Ingredient(null, Ingredient.Type.EGG), 2), sync);
			
			executor.execute(getFlour);
			executor.execute(getHoney);
			executor.execute(getEggs);
			
			sync.await();
			
			for (Ingredient res : getFlour.getResultEntries()) {
				System.out.println(res);
			}
			
			for (Ingredient res : getHoney.getResultEntries()) {
				System.out.println(res);
			}
			
			for (Ingredient res : getEggs.getResultEntries()) {
				System.out.println(res);
			}

			capi.commitTransaction(tx);

			System.out.println("=== END -- QUERY =====");

		} catch (MzsCoreException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
