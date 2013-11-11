package factory.entities;

import java.io.Serializable;

public class Ingredient implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Long id;
	
	public enum Type {
		HONEY,
		FLOUR,
		EGG
	};
	
	private Type type;

	
	public Ingredient(Long id, Type type) {
		this.id = id;
		this.type = type;
	}
	
	 @Override
     public String toString() {
		 return "[ " + this.id + " - " + this.type + " ]";
	 }

}
