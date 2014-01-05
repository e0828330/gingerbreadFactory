package factory.spacesImpl;

public class SpaceUtils {
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
		int factoryId = 0;
		try {
			factoryId = Integer.parseInt(args[idx]);
		}
		catch (NumberFormatException e) {
			System.err.println("Please supply a valid factory id!");
			System.exit(1);
		}
		
		Server.spaceURL = "xvsm://localhost:" + factoryId;
	}
}
