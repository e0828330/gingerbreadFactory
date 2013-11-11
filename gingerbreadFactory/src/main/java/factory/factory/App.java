package factory.factory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.mozartspaces.capi3.LindaCoordinator;
import org.mozartspaces.core.Capi;
import org.mozartspaces.core.ContainerReference;
import org.mozartspaces.core.DefaultMzsCore;
import org.mozartspaces.core.Entry;
import org.mozartspaces.core.MzsConstants;
import org.mozartspaces.core.MzsCore;
import org.mozartspaces.core.MzsCoreException;
import org.mozartspaces.core.TransactionReference;

import factory.entities.Ingredient;

public class App {
	
	private static ExecutorService executor;
	
	public static void main(String[] args) throws MzsCoreException, InterruptedException {
		MzsCore core = DefaultMzsCore.newInstance(); // TODO: Server
		Capi capi = new Capi(core);
		ContainerReference container = capi.createContainer("ingredients", null, 
															 MzsConstants.Container.UNBOUNDED, null, new LindaCoordinator(false));
		
		
		executor = Executors.newCachedThreadPool();
		
		executor.execute(new BakerTest(core, executor));
		
		Thread.sleep(2000);
		
		Ingredient honey = new Ingredient(1L, Ingredient.Type.HONEY);
		Ingredient flour = new Ingredient(2L, Ingredient.Type.FLOUR);
		Ingredient egg = new Ingredient(3L, Ingredient.Type.EGG);
		Ingredient egg2 = new Ingredient(4L, Ingredient.Type.EGG);
		Ingredient egg3 = new Ingredient(5L, Ingredient.Type.EGG);
	
		
		TransactionReference tx = capi.createTransaction(1000, null);
		capi.write(container, new Entry(honey), new Entry(flour));	
		capi.commitTransaction(tx);
		Thread.sleep(3500);
		tx = capi.createTransaction(1000, null);
		capi.write(container, new Entry(egg), new Entry(egg2), new Entry(egg3));
		capi.commitTransaction(tx);
		
		
		
		Thread.sleep(15000);

		capi.destroyContainer(container, null);
		core.shutdown(true);
		executor.shutdownNow();
	}
}
