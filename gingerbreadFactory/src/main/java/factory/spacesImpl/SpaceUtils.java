package factory.spacesImpl;

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
}
