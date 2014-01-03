package factory.spacesImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

import org.mozartspaces.capi3.FifoCoordinator;
import org.mozartspaces.capi3.KeyCoordinator;
import org.mozartspaces.capi3.LindaCoordinator;
import org.mozartspaces.capi3.QueryCoordinator;
import org.mozartspaces.core.Capi;
import org.mozartspaces.core.DefaultMzsCore;
import org.mozartspaces.core.MzsConstants;
import org.mozartspaces.core.MzsCore;
import org.mozartspaces.core.MzsCoreException;

public class Server {
		
	public static final String spaceURL = "xvsm://localhost:9876";
	
	public static void main(String[] args) throws MzsCoreException, InterruptedException, URISyntaxException, IOException {
		MzsCore core = DefaultMzsCore.newInstance();
		Capi capi = new Capi(core);
		capi.createContainer("ingredients", new URI(spaceURL), MzsConstants.Container.UNBOUNDED, null, new LindaCoordinator(false), new FifoCoordinator());
		capi.createContainer("charges", new URI(spaceURL), MzsConstants.Container.UNBOUNDED, null, new FifoCoordinator());
		capi.createContainer("oven", new URI(spaceURL), 10, null, new KeyCoordinator(), new LindaCoordinator(false), new FifoCoordinator());
		capi.createContainer("gingerbreads", new URI(spaceURL), MzsConstants.Container.UNBOUNDED, null, new LindaCoordinator(false), new FifoCoordinator());
		capi.createContainer("qaPassed", new URI(spaceURL), MzsConstants.Container.UNBOUNDED, null, new FifoCoordinator());
		capi.createContainer("orders", new URI(spaceURL), MzsConstants.Container.UNBOUNDED, null, new QueryCoordinator());
		
		
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
	}
	
}
