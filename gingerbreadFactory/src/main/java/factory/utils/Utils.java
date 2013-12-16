package factory.utils;

import java.net.UnknownHostException;
import java.util.UUID;

public class Utils {
	/**
	 * Generates a global unique ID
	 * It gets XORed with the IP address to make sure to not have
	 * collisions between hosts.
	 * 
	 * @return
	 */
	public static Long getID() {
		try {
			int hash1 = java.net.Inet4Address.getLocalHost().hashCode();
			int hash2 = UUID.randomUUID().hashCode();

			return new Long(Math.abs((hash1) ^ (hash2)));
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		return 0L;
		
	}
	
	/**
	 * Returns a number between 1000 and 2000
	 * can be used to wait "1-2 seconds"
	 * @return
	 */
	public static int getRandomWaitTime() {
		return (int) (1000 + Math.random() * 1000);
	}
	
	/**
	 * Returns Long id passed during startup
	 * 
	 * @param args
	 * @return
	 */
	public static Long getStartupId(String[] args) {
		Long id = Utils.getID();
		if (args.length > 0) {
			id = Long.parseLong(args[0]);
		}
		return id;
	}
	
	/**
	 * Parses the defect rate from cmdline param
	 * @param args
	 * @return
	 */
	public static Float getStartupDefectRate(String[] args) {
		Float defectRate = 0.2f;
		if (args.length > 1) {
			defectRate = Float.parseFloat(args[1]);
		}
		return defectRate;
	}
}
