package se.sitic.megatron.entity;

import se.sitic.megatron.entity.base.BaseDomainName;



public class DomainName extends BaseDomainName {
	private static final long serialVersionUID = 1L;

/*[CONSTRUCTOR MARKER BEGIN]*/
	public DomainName () {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public DomainName (java.lang.Integer id) {
		super(id);
	}

	/**
	 * Constructor for required fields
	 */
	public DomainName (
		java.lang.Integer id,
		java.lang.Integer organizationId,
		java.lang.String name) {

		super (
			id,
			organizationId,
			name);
	}

/*[CONSTRUCTOR MARKER END]*/	
	
	public DomainName(String name) {
		super();
		this.setName(name);
	}
	
	public int compareTo(Object obj) {
		
		return this.getName().compareToIgnoreCase(((DomainName)obj).getName());
		
	}
	
	// Added to trim the domain name
	public void setName (java.lang.String name) {
        super.setName(name.trim());
    }
	
}