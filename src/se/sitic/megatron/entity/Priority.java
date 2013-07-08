package se.sitic.megatron.entity;

import se.sitic.megatron.entity.base.BasePriority;



public class Priority extends BasePriority {
	private static final long serialVersionUID = 1L;

/*[CONSTRUCTOR MARKER BEGIN]*/
	public Priority () {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public Priority (java.lang.Integer id) {
		super(id);
	}

	/**
	 * Constructor for required fields
	 */
	public Priority (
		java.lang.Integer id,
		java.lang.String name,
		java.lang.Integer prio) {

		super (
			id,
			name,
			prio);
	}

/*[CONSTRUCTOR MARKER END]*/


}