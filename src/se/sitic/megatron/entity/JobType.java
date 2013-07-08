package se.sitic.megatron.entity;

import se.sitic.megatron.entity.base.BaseJobType;



public class JobType extends BaseJobType {
	private static final long serialVersionUID = 1L;

/*[CONSTRUCTOR MARKER BEGIN]*/
	public JobType () {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public JobType (java.lang.Integer id) {
		super(id);
	}

	/**
	 * Constructor for required fields
	 */
	public JobType (
		java.lang.Integer id,
		java.lang.String name,
		boolean enabled) {

		super (
			id,
			name,
			enabled);
	}

/*[CONSTRUCTOR MARKER END]*/


}