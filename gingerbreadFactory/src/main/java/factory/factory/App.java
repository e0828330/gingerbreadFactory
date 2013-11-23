package factory.factory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

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
import factory.utils.Utils;

public class App {
		
	public static final String spaceURL = "xvsm://localhost:9876";
	
	public static void main(String[] args) throws MzsCoreException, InterruptedException, URISyntaxException, IOException {
		MzsCore core = DefaultMzsCore.newInstance();
		Capi capi = new Capi(core);
		ContainerReference container = capi.createContainer("ingredients", new URI(spaceURL), 
															 MzsConstants.Container.UNBOUNDED, null, new LindaCoordinator(false));
		
		
		System.out.println("Server running");
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		System.out.println("Type quit or exit to quit.");
		
		String input;
		while ((input = br.readLine()) != null) {
			 if (input.equals("quit") || input.equals("exit")) {
				break;
			}
		}
		
		System.out.println("Shutting down.");
		core.shutdown(true);
		
		
		/*
		Ingredient honey = new Ingredient(Utils.getID(), Ingredient.Type.HONEY);
		Ingredient flour = new Ingredient(Utils.getID(), Ingredient.Type.FLOUR);
		Ingredient egg = new Ingredient(Utils.getID(), Ingredient.Type.EGG);
		Ingredient egg2 = new Ingredient(Utils.getID(), Ingredient.Type.EGG);
		Ingredient egg3 = new Ingredient(Utils.getID(), Ingredient.Type.EGG);
	
		
		TransactionReference tx = capi.createTransaction(1000, new URI(spaceURL));
		capi.write(container, new Entry(honey), new Entry(flour));	
		capi.write(container, new Entry(honey), new Entry(flour));
		capi.commitTransaction(tx);
		Thread.sleep(3500);
		tx = capi.createTransaction(1000, new URI(spaceURL));
		capi.write(container, new Entry(egg), new Entry(egg2), new Entry(egg3));
		capi.write(container, new Entry(egg), new Entry(egg2), new Entry(egg3));
		capi.commitTransaction(tx);*/
	}
	
}
