package factory.gui;

import org.apache.pivot.beans.BXMLSerializer;
import org.apache.pivot.collections.Map;
import org.apache.pivot.wtk.Application;
import org.apache.pivot.wtk.DesktopApplicationContext;
import org.apache.pivot.wtk.Display;

public class GuiMain implements Application {

	private TabbedWindow window = null;
	
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
		window = (TabbedWindow) bxmlSerializer.readObject(GuiMain.class, "TabbedWindow.xml");
		window.setWidth(1100);
		window.setHeight(720);
		window.open(dsp);
	}

	public void suspend() throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	public static void main(String[] args) {
	    DesktopApplicationContext.main(GuiMain.class, args);
	}

}
