package se.sitic.megatron.entity;

import se.sitic.megatron.entity.base.BaseOriginalLogEntry;



public class OriginalLogEntry extends BaseOriginalLogEntry {
	private static final long serialVersionUID = 1L;

/*[CONSTRUCTOR MARKER BEGIN]*/
	public OriginalLogEntry () {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public OriginalLogEntry (java.lang.Long id) {
		super(id);
	}

	/**
	 * Constructor for required fields
	 */
	public OriginalLogEntry (
		java.lang.Long id,
		java.lang.Long created,
		java.lang.String entry) {

		super (
			id,
			created,
			entry);
	}

/*[CONSTRUCTOR MARKER END]*/


}