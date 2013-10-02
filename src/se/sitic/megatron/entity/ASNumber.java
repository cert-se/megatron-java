package se.sitic.megatron.entity;

import se.sitic.megatron.entity.base.BaseASNumber;



public class ASNumber extends BaseASNumber {
	private static final long serialVersionUID = 1L;

/*[CONSTRUCTOR MARKER BEGIN]*/
	public ASNumber () {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public ASNumber (java.lang.Integer id) {
		super(id);
	}

	/**
	 * Constructor for required fields
	 */
	public ASNumber (
		java.lang.Integer id,
		java.lang.Integer organizationId,
		java.lang.Long number) {

		super (
			id,
			organizationId,
			number);
	}
/*[CONSTRUCTOR MARKER END]*/
	
	public ASNumber (java.lang.Long asn) {
		super();
		this.setNumber(asn);
	}
	
	@Override
    public int compareTo(Object obj) {
		
		int result = 0;
		if (this.getNumber() == ((ASNumber)obj).getNumber()) {		
			result = 0;
		}
		else if (this.getNumber() < ((ASNumber)obj).getNumber()) {
			result = -1;
		}
		else {
			return 1;
		}
		return result;
	}


}