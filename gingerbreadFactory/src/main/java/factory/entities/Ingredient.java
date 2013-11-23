package factory.entities;

import java.io.Serializable;

public class Ingredient implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Long id;
	private Long supplierId;
	
	public enum Type {
		HONEY,
		FLOUR,
		EGG
	};
	
	private Type type;

	
	public Long getId() {
		return id;
	}

	public Long getSupplierId() {
		return supplierId;
	}

	public Ingredient(Long supplierId, Long id, Type type) {
		this.id = id;
		this.type = type;
		this.supplierId = supplierId;
	}
	
	 @Override
     public String toString() {
		 return "[ | " + this.getSupplierId() + " | " + this.getId() + " - " + this.type + " ]";
	 }

}
