package factory.gui;

import java.net.URL;
import java.util.HashMap;

import org.apache.pivot.beans.Bindable;
import org.apache.pivot.collections.List;
import org.apache.pivot.collections.Map;
import org.apache.pivot.util.Resources;
import org.apache.pivot.wtk.Button;
import org.apache.pivot.wtk.ButtonGroup;
import org.apache.pivot.wtk.ButtonPressListener;
import org.apache.pivot.wtk.PushButton;
import org.apache.pivot.wtk.TableView;
import org.apache.pivot.wtk.TextInput;
import org.apache.pivot.wtk.Window;

public class TabbedWindow extends Window implements Bindable{

	private TextInput supplierId;
	private TextInput amount;
	private ButtonGroup type;
	private PushButton submitButton;
	private TableView testTable;
	
	@SuppressWarnings("unchecked")
	public void initialize(Map<String, Object> namespace, URL location, Resources resources) {
		supplierId = (TextInput) namespace.get("supplierId");
		amount = (TextInput) namespace.get("amount");
		type = (ButtonGroup) namespace.get("type");
		submitButton = (PushButton) namespace.get("submitButton");
		testTable = (TableView) namespace.get("testTable");
		
		
		/* Add some stuff to the table */
		final List<Object> test = (List<Object>) testTable.getTableData();
		test.add(new HashMap<String, String>());
		test.add(new HashMap<String, String>());
		test.add(new HashMap<String, String>());
		((HashMap<String, String>) test.get(0)).put("id", "1234");
		((HashMap<String, String>) test.get(0)).put("baker", "Hallo");
		((HashMap<String, String>) test.get(1)).put("id", "9852");
		((HashMap<String, String>) test.get(1)).put("baker", "Hallo test");
		((HashMap<String, String>) test.get(2)).put("id", "1235");
		((HashMap<String, String>) test.get(2)).put("baker", "Hallo");
		
		submitButton.getButtonPressListeners().add(new ButtonPressListener() {
			public void buttonPressed(Button btn) {
				System.out.println("CLICK");
				System.out.println("ID:" + supplierId.getText());
				System.out.println("AMOUNT:" + amount.getText());
				System.out.println("TYPE: " + type.getSelection().getButtonData());
				test.add(new HashMap<String, String>());
				((HashMap<String, String>) test.get(test.getLength() - 1)).put("id", amount.getText());
				((HashMap<String, String>) test.get(test.getLength() - 1)).put("baker", "Dynamic Test");
			}
		});
	}

}
