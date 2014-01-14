package factory.spacesImpl;

import java.net.URI;
import java.net.URISyntaxException;

import org.mozartspaces.capi3.FifoCoordinator;
import org.mozartspaces.core.Capi;
import org.mozartspaces.core.ContainerReference;
import org.mozartspaces.core.MzsConstants;
import org.mozartspaces.core.MzsCore;
import org.mozartspaces.core.MzsCoreException;

public class SpaceUtils {
	
	private static int factoryId;
	
	/**
	 * Parses the factory id from the cmd line arguments and sets the space url to point to it
	 * 
	 * @param args
	 * @param idx
	 */
	public static void parseFactoryID(String[] args, int idx) {
		if (args.length < idx + 1) {
			System.err.println("Please supply a factory id!");
			System.exit(1);
		}
		factoryId = 0;
		try {
			factoryId = Integer.parseInt(args[idx]);
		}
		catch (NumberFormatException e) {
			System.err.println("Please supply a valid factory id!");
			System.exit(1);
		}
		
		Server.spaceURL = "xvsm://localhost:" + factoryId;
	}

	public static int getFactoryId() {
		return factoryId;
	}

	public static void setFactoryId(int factoryId) {
		SpaceUtils.factoryId = factoryId;
	}

	private static class BenchmarkStopThread implements Runnable {

		private MzsCore core;
		
		public BenchmarkStopThread(MzsCore core) {
			this.core = core;
		}
		
		public void run() {
			Capi capi = new Capi(core);
			try {
				ContainerReference start = capi.lookupContainer("benchmarkStop", new URI(Server.spaceURL), MzsConstants.RequestTimeout.INFINITE, null);
				capi.read(start, FifoCoordinator.newSelector(), MzsConstants.RequestTimeout.INFINITE, null);
				System.out.println("Benchmark ended!");
				System.exit(0);
			} catch (MzsCoreException e) {
				e.printStackTrace();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Used to setup benchmark for workers, kill on stop and wait for start
	 * @param core
	 */
	public static void setupBenchmark(MzsCore core) {
		if (!Server.BENCHMARK) {
			return;
		}
		Capi capi = new Capi(core);
		try {
			new Thread(new BenchmarkStopThread(core)).start();
			System.out.println("Waiting for start signal ...");
			ContainerReference start = capi.lookupContainer("benchmarkStart", new URI(Server.spaceURL), MzsConstants.RequestTimeout.INFINITE, null);
			capi.read(start, FifoCoordinator.newSelector(), MzsConstants.RequestTimeout.INFINITE, null); 
		} catch (MzsCoreException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
}
