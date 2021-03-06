package factory.gui;

import org.apache.pivot.beans.BXMLSerializer;
import org.apache.pivot.collections.Map;
import org.apache.pivot.wtk.Application;
import org.apache.pivot.wtk.DesktopApplicationContext;
import org.apache.pivot.wtk.Display;

import factory.spacesImpl.SpaceUtils;
import factory.utils.JMSUtils;

public class GuiMain implements Application {

	private MainWindow window = null;
	
	public enum Mode {
		SPACES,
		JMS
	}
	
	public static Mode mode;
	
	public void resume() throws Exception {
		
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
		
	}
	
	public static void main(String[] args) {
		if (args.length != 0 && args[0].equals("jms")) {
			mode = Mode.JMS;
			JMSUtils.parseFactoryID(args, args.length - 1);
		}
		else {
			mode = Mode.SPACES;
			SpaceUtils.parseFactoryID(args, args.length - 1);
		}
	    DesktopApplicationContext.main(GuiMain.class, args);
	}

}
