package factory.gui;

import org.apache.pivot.beans.BXMLSerializer;
import org.apache.pivot.collections.Map;
import org.apache.pivot.wtk.Application;
import org.apache.pivot.wtk.DesktopApplicationContext;
import org.apache.pivot.wtk.Display;

public class GuiMain implements Application {

	private MainWindow window = null;
	
	public enum Mode {
		SPACES,
		JMS
	}
	
	public static Mode mode;
	
	public void resume() throws Exception {
		// TODO Auto-generated method stub
		
	}

	public boolean shutdown(boolean arg0) throws Exception {
		window.close();
		return false;
	}

	public void startup(Display dsp, Map<String, String> props)
			throws Exception {
		BXMLSerializer bxmlSerializer = new BXMLSerializer();
		window = (MainWindow) bxmlSerializer.readObject(GuiMain.class, "/Window.xml");
		window.setWidth(1000);
		window.setHeight(720);
		window.open(dsp);
	}

	public void suspend() throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	public static void main(String[] args) {
		if (args.length != 0 && args[0].equals("jms")) {
			mode = Mode.JMS;
		}
		else {
			mode = Mode.SPACES;
		}
	    DesktopApplicationContext.main(GuiMain.class, args);
	}

}
