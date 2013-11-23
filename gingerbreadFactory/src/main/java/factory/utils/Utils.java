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
}
