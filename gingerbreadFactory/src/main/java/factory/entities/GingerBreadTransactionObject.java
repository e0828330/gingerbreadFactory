package factory.entities;

import java.io.Serializable;

public class GingerBreadTransactionObject implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Ingredient egg1;
	private Ingredient egg2;
	private Ingredient flour;
	private Ingredient honey;
	private Ingredient chocolate;
	private Ingredient nut;	

	public GingerBreadTransactionObject(Ingredient egg1, Ingredient egg2, Ingredient flour, Ingredient honey) {
		this.egg1 = egg1;
		this.egg2 = egg2;
		this.flour = flour;
		this.honey = honey;
	}
	
	public GingerBreadTransactionObject(Ingredient egg1, Ingredient egg2, Ingredient flour, Ingredient honey,
			Ingredient nut, Ingredient chocolate) {
		this.egg1 = egg1;
		this.egg2 = egg2;
		this.flour = flour;
		this.honey = honey;
		this.nut = nut;
		this.chocolate = chocolate;
	}
	
	public Ingredient getEgg1() {
		return egg1;
	}
	public void setEgg1(Ingredient egg1) {
		this.egg1 = egg1;
	}
	public Ingredient getEgg2() {
		return egg2;
	}
	public void setEgg2(Ingredient egg2) {
		this.egg2 = egg2;
	}
	public Ingredient getFlour() {
		return flour;
	}
	public void setFlour(Ingredient flour) {
		this.flour = flour;
	}
	public Ingredient getHoney() {
		return honey;
	}
	public void setHoney(Ingredient honey) {
		this.honey = honey;
	}

	public Ingredient getNut() {
		return nut;
	}

	public void setNut(Ingredient nut) {
		this.nut = nut;
	}

	public Ingredient getChocolate() {
		return chocolate;
	}

	public void setChocolateId(Ingredient chocolate) {
		this.chocolate = chocolate;
	}
	
}
