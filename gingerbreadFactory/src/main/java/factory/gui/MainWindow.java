package factory.gui;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.List;

import org.apache.pivot.beans.Bindable;
import org.apache.pivot.collections.Map;
import org.apache.pivot.util.Resources;
import org.apache.pivot.wtk.Alert;
import org.apache.pivot.wtk.ApplicationContext;
import org.apache.pivot.wtk.Button;
import org.apache.pivot.wtk.ButtonGroup;
import org.apache.pivot.wtk.ButtonPressListener;
import org.apache.pivot.wtk.MessageType;
import org.apache.pivot.wtk.PushButton;
import org.apache.pivot.wtk.TableView;
import org.apache.pivot.wtk.TextInput;
import org.apache.pivot.wtk.Window;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import factory.entities.Ingredient;
import factory.entities.Order;
import factory.entities.Order.State;
import factory.interfaces.LogisticsOrder;
import factory.interfaces.Supplier;
import factory.jmsImpl.logistics.JMSLogisticsOrderImpl;
import factory.jmsImpl.supplier.JMSSupplierInstance;
import factory.spacesImpl.LogisticsOrderImpl;
import factory.spacesImpl.SpaceUtils;
import factory.spacesImpl.SupplierImpl;
import factory.utils.JMSUtils;
import factory.utils.Utils;

public class MainWindow extends Window implements Bindable{

	private TextInput supplierId;
	private TextInput amount;
	private ButtonGroup type;
	private PushButton submitButton;
	
	private TextInput numPackages;
	private TextInput numNormal;
	private TextInput numNut;
	private TextInput numChocolade;
	private PushButton submitOrderButton;
	
	private Map<String, Object> namespace;
	
	private ObjectMapper mapper;
	
	public synchronized void updateTable(String tableId, List<?> data) throws JsonGenerationException, JsonMappingException, IOException {
		final String _tableId = tableId;
		final List<?> _data = data;
		ApplicationContext.queueCallback(new Runnable() {
			public void run() {
				TableView table = (TableView) namespace.get(_tableId);
				try {
					table.setTableData(mapper.writeValueAsString(_data));
				} catch (JsonGenerationException e) {
					e.printStackTrace();
				} catch (JsonMappingException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public void initialize(Map<String, Object> namespace, URL location, Resources resources) {
		
		this.namespace = namespace;
		mapper = new ObjectMapper();
		supplierId = (TextInput) namespace.get("supplierId");
		amount = (TextInput) namespace.get("amount");
		type = (ButtonGroup) namespace.get("type");
		submitButton = (PushButton) namespace.get("submitButton");
		
		numPackages = (TextInput) namespace.get("numPackages");
		numNormal = (TextInput) namespace.get("numNormal");
		numNut = (TextInput) namespace.get("numNut");
		numChocolade = (TextInput) namespace.get("numChocolade");
		submitOrderButton = (PushButton) namespace.get("submitOrderButton");
		
		DataThread dataThread = new DataThread(this);
		new Thread(dataThread).start();
		
		submitButton.getButtonPressListeners().add(new ButtonPressListener() {
			public void buttonPressed(Button btn) {
				System.out.println("CLICK");
				System.out.println("ID:" + supplierId.getText());
				System.out.println("AMOUNT:" + amount.getText());
				System.out.println("TYPE: " + type.getSelection().getButtonData());
				
				Supplier supplier = null;
				if (GuiMain.mode.equals(GuiMain.Mode.SPACES)) {
					supplier = new SupplierImpl();
				}
				else {
					supplier = new JMSSupplierInstance(JMSUtils.getFactoryID());
				}
				supplier.setId(Long.parseLong(supplierId.getText()));
				Ingredient.Type selectedType = null;
				if (type.getSelection().getButtonData().toString().equals("Honig")) {
					selectedType = Ingredient.Type.HONEY;
				}
				else if (type.getSelection().getButtonData().toString().equals("Eier")) {
					selectedType = Ingredient.Type.EGG;
				}
				else if (type.getSelection().getButtonData().toString().equals("Nüsse")) {
					selectedType = Ingredient.Type.NUT;
				}
				else if (type.getSelection().getButtonData().toString().equals("Schokolade")) {
					selectedType = Ingredient.Type.CHOCOLATE;
				}
				else {
					selectedType = Ingredient.Type.FLOUR;
				}
				
				supplier.placeOrder(Integer.parseInt(amount.getText()), selectedType);
				new Thread(supplier).start();
				Alert.alert("Auftrag abgeschickt", MainWindow.this);
			}
		});
		
		submitOrderButton.getButtonPressListeners().add(new ButtonPressListener() {
			public void buttonPressed(Button btn) {
				System.out.println("CLICK");
				
				System.out.println("Packages: " + numPackages.getText());
				System.out.println("Normal: " + numNormal.getText());
				System.out.println("Nut: " + numNut.getText());
				System.out.println("Chocolate: " + numChocolade.getText());

				int nPackages = 0;
				int nNormal = 0;
				int nNut = 0;
				int nChocolate = 0;
				
				try {
					nPackages = Integer.parseInt(numPackages.getText());
					nNormal = Integer.parseInt(numNormal.getText());
					nNut = Integer.parseInt(numNut.getText());
					nChocolate = Integer.parseInt(numChocolade.getText());
				}
				catch (NumberFormatException e) {
					
				}

				if (nPackages <= 0) {
					Alert.alert(MessageType.ERROR, "Bitte gültige Packungsanzahl angeben!", MainWindow.this);
					return;
				}
				
				if ((nNormal + nNut + nChocolate) != 6) {
					Alert.alert(MessageType.ERROR, "Eine Packung muss 6 Lebkuchen enthalten!", MainWindow.this);
					return;
				}
				
				Order order = new Order();
				order.setId(factory.utils.Utils.getID());
				order.setPackages(nPackages);
				order.setNumNormal(nNormal);
				order.setNumNut(nNut);
				order.setNumChocolate(nChocolate);
				order.setTimestamp(new Date().getTime());
				order.setState(State.OPEN);

				LogisticsOrder logisticsOrder = null;
				if (GuiMain.mode.equals(GuiMain.Mode.SPACES)) {
					logisticsOrder = new LogisticsOrderImpl();
					order.setFactoryId(SpaceUtils.getFactoryId());
				}
				else {
					logisticsOrder = new JMSLogisticsOrderImpl(JMSUtils.getFactoryID());
					order.setFactoryId(JMSUtils.getFactoryID());
				}
				
				logisticsOrder.placeOrder(order);
				new Thread(logisticsOrder).start();
				Alert.alert("Auftrag abgeschickt", MainWindow.this);
			}
		});
	}

}
