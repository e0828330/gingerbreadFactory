package factory.gui;


import factory.interfaces.Monitor;
import factory.jmsImpl.monitor.JMSMonitor;
import factory.spacesImpl.SpacesMonitor;

public class DataThread implements Runnable {

	private DataListener listener;
	
	public DataThread(MainWindow window) {
		listener = new DataListener(window);
	}
	
	public void run() {
		Monitor dataMonitor = null;
		if (GuiMain.mode.equals(GuiMain.Mode.SPACES)) {
			dataMonitor = new SpacesMonitor();
		}
		else {
			dataMonitor = new JMSMonitor();
		}
		listener.onGingerBreadStateChange(dataMonitor.getGingerBreads());
		listener.onOvenChanged(dataMonitor.getOvenContent());
		listener.onIngredientChanged(dataMonitor.getIngredients());
		dataMonitor.setListener(listener);
	}

}
