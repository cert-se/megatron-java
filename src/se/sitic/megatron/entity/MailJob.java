package se.sitic.megatron.entity;

import se.sitic.megatron.entity.base.BaseMailJob;



public class MailJob extends BaseMailJob {
	private static final long serialVersionUID = 1L;

/*[CONSTRUCTOR MARKER BEGIN]*/
	public MailJob () {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public MailJob (java.lang.Long id) {
		super(id);
	}

	/**
	 * Constructor for required fields
	 */
	public MailJob (
		java.lang.Long id,
		boolean usePrimaryOrg,
		java.lang.Long started) {

		super (
			id,
			usePrimaryOrg,
			started);
	}

/*[CONSTRUCTOR MARKER END]*/

}