package factory.gui;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import org.apache.pivot.beans.Bindable;
import org.apache.pivot.collections.Map;
import org.apache.pivot.util.Resources;
import org.apache.pivot.wtk.Button;
import org.apache.pivot.wtk.ButtonGroup;
import org.apache.pivot.wtk.ButtonPressListener;
import org.apache.pivot.wtk.PushButton;
import org.apache.pivot.wtk.TableView;
import org.apache.pivot.wtk.TextInput;
import org.apache.pivot.wtk.Window;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import factory.entities.Ingredient;

public class TabbedWindow extends Window implements Bindable{

	private TextInput supplierId;
	private TextInput amount;
	private ButtonGroup type;
	private PushButton submitButton;
	private TableView testTable;
	
	public void initialize(Map<String, Object> namespace, URL location, Resources resources) {
		supplierId = (TextInput) namespace.get("supplierId");
		amount = (TextInput) namespace.get("amount");
		type = (ButtonGroup) namespace.get("type");
		submitButton = (PushButton) namespace.get("submitButton");
		testTable = (TableView) namespace.get("testTable");
		
		
		/* Add some stuff to the table */
		ArrayList<Ingredient> tableData = new ArrayList<Ingredient>();
		tableData.add(new Ingredient(15L, 10L, Ingredient.Type.EGG));
		tableData.add(new Ingredient(112L, 15L, Ingredient.Type.EGG));
		ObjectMapper mapper = new ObjectMapper();
		try {
			System.out.println(mapper.writeValueAsString(tableData));
			testTable.setTableData(mapper.writeValueAsString(tableData));
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		submitButton.getButtonPressListeners().add(new ButtonPressListener() {
			public void buttonPressed(Button btn) {
				System.out.println("CLICK");
				System.out.println("ID:" + supplierId.getText());
				System.out.println("AMOUNT:" + amount.getText());
				System.out.println("TYPE: " + type.getSelection().getButtonData());
			
				ArrayList<Ingredient> tableData = new ArrayList<Ingredient>();
				tableData.add(new Ingredient(20L, 10L, Ingredient.Type.HONEY));
				tableData.add(new Ingredient(112L, 15L, Ingredient.Type.EGG));
				ObjectMapper mapper = new ObjectMapper();
				try {
					System.out.println(mapper.writeValueAsString(tableData));
					testTable.setTableData(mapper.writeValueAsString(tableData));
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

}
