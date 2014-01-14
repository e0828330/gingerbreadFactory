package factory.spacesImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

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

public class Server {
		
	public static String spaceURL = "xvsm://localhost:9876";
	
	public static void main(String[] args) throws MzsCoreException, InterruptedException, URISyntaxException, IOException {
		MzsCore core = DefaultMzsCore.newInstance();
		
		Server.spaceURL = core.getConfig().getSpaceUri().toString();
		TcpSocketConfiguration tcpConfig = (TcpSocketConfiguration) core.getConfig().getTransportConfigurations().get("xvsm");
		
		Capi capi = new Capi(core);
		capi.createContainer("ingredients", new URI(spaceURL), MzsConstants.Container.UNBOUNDED, null, new LindaCoordinator(false), new FifoCoordinator());
		capi.createContainer("charges", new URI(spaceURL), MzsConstants.Container.UNBOUNDED, null, new FifoCoordinator());
		capi.createContainer("oven", new URI(spaceURL), 10, null, new KeyCoordinator(), new LindaCoordinator(false), new FifoCoordinator());
		capi.createContainer("gingerbreads", new URI(spaceURL), MzsConstants.Container.UNBOUNDED, null, new LindaCoordinator(false), new FifoCoordinator());
		capi.createContainer("qaPassed", new URI(spaceURL), MzsConstants.Container.UNBOUNDED, null, new FifoCoordinator());
		capi.createContainer("orders", new URI(spaceURL), MzsConstants.Container.UNBOUNDED, null, new LindaCoordinator(false), new FifoCoordinator());
		
		
		System.out.println("Server running");
		System.out.println("=====================================");
		System.out.println("Factory ID: " + tcpConfig.getReceiverPort());
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		br.readLine();

		/* Benchmark stuff */
		ContainerReference start = capi.createContainer("benchmarkStart", new URI(spaceURL), MzsConstants.Container.UNBOUNDED, null, new FifoCoordinator());
		ContainerReference stop = capi.createContainer("benchmarkStop", new URI(spaceURL), MzsConstants.Container.UNBOUNDED, null, new FifoCoordinator());
		
		System.out.println("Sending start signal ...");
		capi.write(start, new Entry(new String("START")));
		Thread.sleep(20000);
		System.out.println("Sending stop signal!");
		capi.write(stop, new Entry(new String("STOP")));

		// TODO: Count gingerbreads packages
		
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
