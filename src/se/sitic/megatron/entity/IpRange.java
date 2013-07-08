package se.sitic.megatron.entity;

import se.sitic.megatron.entity.base.BaseIpRange;



public class IpRange extends BaseIpRange {
	private static final long serialVersionUID = 1L;

/*[CONSTRUCTOR MARKER BEGIN]*/
	public IpRange () {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public IpRange (java.lang.Integer id) {
		super(id);
	}

	/**
	 * Constructor for required fields
	 */
	public IpRange (
		java.lang.Integer id,
		java.lang.Integer organizationId,
		java.lang.Long startAddress,
		java.lang.Long endAddress) {

		super (
			id,
			organizationId,
			startAddress,
			endAddress);
	}

/*[CONSTRUCTOR MARKER END]*/	
	
	public int compareTo(Object obj) {

		int result = 0;
		if (this.getStartAddress() == ((IpRange)obj).getStartAddress()) {		
			result = 0;
		}
		else if (this.getStartAddress() < ((IpRange)obj).getStartAddress()) {
			result = -1;
		}
		else {
			return 1;
		}
		return result;
	}




}