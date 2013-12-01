package factory.gui;

import java.net.URL;

import org.apache.pivot.beans.Bindable;
import org.apache.pivot.collections.Map;
import org.apache.pivot.util.Resources;
import org.apache.pivot.wtk.Button;
import org.apache.pivot.wtk.ButtonGroup;
import org.apache.pivot.wtk.ButtonPressListener;
import org.apache.pivot.wtk.PushButton;
import org.apache.pivot.wtk.TextInput;
import org.apache.pivot.wtk.Window;

public class TabbedWindow extends Window implements Bindable{

	private TextInput supplierId;
	private TextInput amount;
	private ButtonGroup type;
	private PushButton submitButton;
	
	public void initialize(Map<String, Object> namespace, URL location, Resources resources) {
		supplierId = (TextInput) namespace.get("supplierId");
		amount = (TextInput) namespace.get("amount");
		type = (ButtonGroup) namespace.get("type");
		submitButton = (PushButton) namespace.get("submitButton");
		
		submitButton.getButtonPressListeners().add(new ButtonPressListener() {
			public void buttonPressed(Button btn) {
				System.out.println("CLICK");
				System.out.println("ID:" + supplierId.getText());
				System.out.println("AMOUNT:" + amount.getText());
				System.out.println("TYPE: " + type.getSelection().getButtonData());
			}
		});
	}

}
