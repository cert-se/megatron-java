package se.sitic.megatron.entity;

import se.sitic.megatron.entity.base.BaseJob;



public class Job extends BaseJob {
	private static final long serialVersionUID = 1L;

/*[CONSTRUCTOR MARKER BEGIN]*/
	public Job () {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public Job (java.lang.Long id) {
		super(id);
	}

	/**
	 * Constructor for required fields
	 */
	public Job (
		java.lang.Long id,
		java.lang.String name,
		java.lang.String filename,
		java.lang.String fileHash,
		java.lang.Long fileSize,
		java.lang.Long started) {

		super (
			id,
			name,
			filename,
			fileHash,
			fileSize,
			started);
	}

/*[CONSTRUCTOR MARKER END]*/


}