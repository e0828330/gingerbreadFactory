package factory.spacesImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

import org.mozartspaces.capi3.FifoCoordinator;
import org.mozartspaces.core.Capi;
import org.mozartspaces.core.ContainerReference;
import org.mozartspaces.core.DefaultMzsCore;
import org.mozartspaces.core.MzsConstants;
import org.mozartspaces.core.MzsCore;
import org.mozartspaces.core.MzsCoreException;

public class LoadBalancer implements Runnable {
	
	public static String loadBalancerURL = "xvsm://localhost:9876";
	
	private MzsCore core;
	
	public LoadBalancer(MzsCore core) {
		this.core = core;
	}

	public static void main(String[] args) throws MzsCoreException, URISyntaxException, IOException {
		System.setProperty("mozartspaces.configurationFile", "mozartspaces-loadbalancer.xml");
		MzsCore core = DefaultMzsCore.newInstance();
		Capi capi = new Capi(core);
		capi.createContainer("ordersLB", new URI(loadBalancerURL), MzsConstants.Container.UNBOUNDED, null, new FifoCoordinator());

		
		LoadBalancer lb = new LoadBalancer(core);
		new Thread(lb).start();
		
		System.out.println("Loadbalancer running");
		System.out.println("=====================================");
		
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

	public void run() {
		Capi capi = new Capi(core);
		try {
			ContainerReference container = capi.lookupContainer("ingredients", new URI(loadBalancerURL), MzsConstants.RequestTimeout.INFINITE, null);
			// TODO: Implement logic
			
		} catch (MzsCoreException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

}
