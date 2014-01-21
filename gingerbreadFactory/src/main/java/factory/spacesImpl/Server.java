package factory.spacesImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.mozartspaces.capi3.FifoCoordinator;
import org.mozartspaces.capi3.KeyCoordinator;
import org.mozartspaces.capi3.LindaCoordinator;
import org.mozartspaces.core.Capi;
import org.mozartspaces.core.ContainerReference;
import org.mozartspaces.core.DefaultMzsCore;
import org.mozartspaces.core.Entry;
import org.mozartspaces.core.MzsConstants;
import org.mozartspaces.core.MzsCore;
import org.mozartspaces.core.MzsCoreException;
import org.mozartspaces.core.config.TcpSocketConfiguration;

import factory.entities.GingerBread;
import factory.entities.Ingredient;

public class Server {
		
	public static String spaceURL = "xvsm://localhost:9876";
	public static final boolean BENCHMARK = false;
	
	public static void fillForBenchmark(Capi capi, ContainerReference ingredientsContainer, Ingredient.Type type, int amount) throws MzsCoreException {
		ArrayList<Entry> entries = new ArrayList<Entry>();
		for (int i = 0; i < amount; i++) {
			entries.add(new Entry(new Ingredient(1234L, 1L, type)));
		}
		
		capi.write(entries, ingredientsContainer);
		
	}
	
	public static void main(String[] args) throws MzsCoreException, InterruptedException, URISyntaxException, IOException {
		MzsCore core = DefaultMzsCore.newInstance();
		
		Server.spaceURL = core.getConfig().getSpaceUri().toString();
		TcpSocketConfiguration tcpConfig = (TcpSocketConfiguration) core.getConfig().getTransportConfigurations().get("xvsm");
		
		Capi capi = new Capi(core);
		ContainerReference ingredientsContainer = capi.createContainer("ingredients", new URI(spaceURL), MzsConstants.Container.UNBOUNDED, null, new LindaCoordinator(false), new FifoCoordinator());
		capi.createContainer("charges", new URI(spaceURL), MzsConstants.Container.UNBOUNDED, null, new FifoCoordinator());
		capi.createContainer("oven", new URI(spaceURL), 10, null, new KeyCoordinator(), new LindaCoordinator(false), new FifoCoordinator());
		ContainerReference gingerbreadContainer = capi.createContainer("gingerbreads", new URI(spaceURL), MzsConstants.Container.UNBOUNDED, null, new LindaCoordinator(false), new FifoCoordinator());
		capi.createContainer("qaPassed", new URI(spaceURL), MzsConstants.Container.UNBOUNDED, null, new FifoCoordinator());
		capi.createContainer("orders", new URI(spaceURL), MzsConstants.Container.UNBOUNDED, null, new LindaCoordinator(false), new FifoCoordinator());
		
		
		System.out.println("Server running");
		System.out.println("=====================================");
		System.out.println("Factory ID: " + tcpConfig.getReceiverPort());
		

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		/* Benchmark stuff */
		if (Server.BENCHMARK) {
			ContainerReference start = capi.createContainer("benchmarkStart", new URI(spaceURL), MzsConstants.Container.UNBOUNDED, null, new FifoCoordinator());
			ContainerReference stop = capi.createContainer("benchmarkStop", new URI(spaceURL), MzsConstants.Container.UNBOUNDED, null, new FifoCoordinator());
			
			fillForBenchmark(capi, ingredientsContainer, Ingredient.Type.FLOUR, 1500);
			
			fillForBenchmark(capi, ingredientsContainer, Ingredient.Type.HONEY, 1500);

			fillForBenchmark(capi, ingredientsContainer, Ingredient.Type.CHOCOLATE, 500);
			
			fillForBenchmark(capi, ingredientsContainer, Ingredient.Type.NUT, 500);
			
			fillForBenchmark(capi, ingredientsContainer, Ingredient.Type.EGG, 3000);

			System.out.println("Waiting ...");
			Thread.sleep(20000);
			
			System.out.println("Sending start signal ...");
			capi.write(start, new Entry(new String("START")));
			Thread.sleep(60000);
			System.out.println("Sending stop signal!");
			capi.write(stop, new Entry(new String("STOP")));
			
			GingerBread tpl = new GingerBread();
			tpl.setState(GingerBread.State.DONE);
			ArrayList<GingerBread> results = capi.read(gingerbreadContainer, LindaCoordinator.newSelector(tpl, MzsConstants.Selecting.COUNT_MAX), MzsConstants.RequestTimeout.INFINITE, null);
			System.out.println("Produced " + (results.size() / 6) + " packages!");
			results = capi.read(gingerbreadContainer, FifoCoordinator.newSelector(MzsConstants.Selecting.COUNT_MAX), MzsConstants.RequestTimeout.INFINITE, null);
			System.out.println("Produced " + results.size() + " gingerbreads!");
			System.exit(0);
		}
		System.out.println("Type quit or exit to quit.");
		
		String input;
		while ((input = br.readLine()) != null) {
			 if (input.equals("quit") || input.equals("exit")) {
				break;
			}
		}
		
		System.out.println("Shutting down.");
		core.shutdown(true);
	}
	
}
