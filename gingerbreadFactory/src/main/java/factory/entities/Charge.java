package factory.entities;

import java.io.Serializable;

public class Charge implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Long id;
	
	public Charge(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}
	
}
