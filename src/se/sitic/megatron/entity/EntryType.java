package se.sitic.megatron.entity;

import se.sitic.megatron.entity.base.BaseEntryType;



public class EntryType extends BaseEntryType {
	private static final long serialVersionUID = 1L;

/*[CONSTRUCTOR MARKER BEGIN]*/
	public EntryType () {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public EntryType (java.lang.Integer id) {
		super(id);
	}

	/**
	 * Constructor for required fields
	 */
	public EntryType (
		java.lang.Integer id,
		java.lang.String name) {

		super (
			id,
			name);
	}

/*[CONSTRUCTOR MARKER END]*/


}