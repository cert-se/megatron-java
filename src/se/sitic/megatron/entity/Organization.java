package se.sitic.megatron.entity;

import se.sitic.megatron.entity.base.BaseOrganization;



public class Organization extends BaseOrganization {
	private static final long serialVersionUID = 1L;

/*[CONSTRUCTOR MARKER BEGIN]*/
	public Organization () {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public Organization (java.lang.Integer id) {
		super(id);
	}

	/**
	 * Constructor for required fields
	 */
	public Organization (
		java.lang.Integer id,
		java.lang.String name,
		boolean enabled,
		java.lang.Long created,
		java.lang.Long lastModified,
		java.lang.String modifiedBy,
		boolean autoUpdateEmail,
		boolean autoUpdateMatchFields) {

		super (
			id,
			name,
			enabled,
			created,
			lastModified,
			modifiedBy,
			autoUpdateEmail,
			autoUpdateMatchFields);
	}

/*[CONSTRUCTOR MARKER END]*/

	
	// Changed to trim each email address in list
	public void setEmailAddresses (java.lang.String emailAddresses) {
	    	    	    
	    if (emailAddresses != null) {
	        String[] mails = emailAddresses.split(",");
	        String tmpMails = "";

	        for (int i=0; i<mails.length; i++) {	       
	            tmpMails = tmpMails + mails[i].trim() + ",";
	        }	    
	        if (tmpMails.endsWith(",")) {
	            tmpMails = tmpMails.substring(0, tmpMails.length() - 1);
	        }
	        super.setEmailAddresses(tmpMails);
	    }
    }
	
	// Overloaded method from the base class that adds organization id to the IP-range.
	public void addToIpRanges (se.sitic.megatron.entity.IpRange ipRange) {
		if (null == getIpRanges()) setIpRanges(new java.util.TreeSet<se.sitic.megatron.entity.IpRange>());
				
		ipRange.setOrganizationId(this.getId());		
		super.addToIpRanges(ipRange);		
	}
	// Overloaded method from the base class that adds organization id to the DomainName.
	public void addToDomainNames (se.sitic.megatron.entity.DomainName domainName) {
		if (null == getDomainNames()) setDomainNames(new java.util.TreeSet<se.sitic.megatron.entity.DomainName>());
		
		domainName.setOrganizationId(this.getId());
		super.addToDomainNames(domainName);		
	}
	// Overloaded method from the base class that adds organization id to the ASNumber.
	public void addToASNumbers (se.sitic.megatron.entity.ASNumber asNumber) {
		if (null == getASNumbers()) setASNumbers(new java.util.TreeSet<se.sitic.megatron.entity.ASNumber>());

		asNumber.setOrganizationId(this.getId());
		super.addToASNumbers(asNumber);
	}

}