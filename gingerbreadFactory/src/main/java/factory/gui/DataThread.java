package factory.gui;


import factory.interfaces.Monitor;
import factory.spacesImpl.SpacesMonitor;

public class DataThread implements Runnable {

	private MainWindow window;
	private DataListener listener;
	
	public DataThread(MainWindow window) {
		this.window = window;
		listener = new DataListener(window);
	}
	
	public void run() {
		Monitor dataMonitor = new SpacesMonitor();
		listener.onGingerBreadStateChange(dataMonitor.getGingerBreads());
		listener.onOvenChanged(dataMonitor.getOvenContent());
		listener.onIngredientChanged(dataMonitor.getIngredients());
		dataMonitor.setListener(listener);
	}

}
