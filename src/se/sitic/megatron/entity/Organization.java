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
            boolean autoUpdateMatchFields) {

        super (
                id,
                name,
                enabled,
                created,
                lastModified,
                modifiedBy,            
                autoUpdateMatchFields);
    }

    /*[CONSTRUCTOR MARKER END]*/


    // Overloaded method from the base class that adds organization id to the Contact.
    @Override
    public void addToContacts (se.sitic.megatron.entity.Contact contact) {
        if (null == getContacts()) setContacts(new java.util.TreeSet<se.sitic.megatron.entity.Contact>());

        contact.setOrganizationId(this.getId());        
        super.addToContacts(contact);        
    }    
    // Overloaded method from the base class that adds organization id to the IP-range.
    @Override
    public void addToIpRanges (se.sitic.megatron.entity.IpRange ipRange) {
        if (null == getIpRanges()) setIpRanges(new java.util.TreeSet<se.sitic.megatron.entity.IpRange>());

        ipRange.setOrganizationId(this.getId());        
        super.addToIpRanges(ipRange);        
    }
    // Overloaded method from the base class that adds organization id to the DomainName.
    @Override
    public void addToDomainNames (se.sitic.megatron.entity.DomainName domainName) {
        if (null == getDomainNames()) setDomainNames(new java.util.TreeSet<se.sitic.megatron.entity.DomainName>());

        domainName.setOrganizationId(this.getId());
        super.addToDomainNames(domainName);        
    }
    // Overloaded method from the base class that adds organization id to the ASNumber.
    @Override
    public void addToASNumbers (se.sitic.megatron.entity.ASNumber asNumber) {
        if (null == getASNumbers()) setASNumbers(new java.util.TreeSet<se.sitic.megatron.entity.ASNumber>());

        asNumber.setOrganizationId(this.getId());
        super.addToASNumbers(asNumber);
    }

    // This method returns all email addresses for the organization that matches the given email type and 
    // the enabledOnly flag. If the email type is null or empty, all addresses matching the enabledOnly
    // flag are returned.

    public String getEmailAddresses(String emailType, boolean enabledOnly){

        java.util.Set<se.sitic.megatron.entity.Contact> allContacts = getContacts();
        java.util.Set<String> addresses = new java.util.TreeSet<String>(); 

        for (se.sitic.megatron.entity.Contact contact : allContacts) {
            if (contact.isEnabled() == true || enabledOnly == false) {
                if (emailType == null) {
                    addresses.add(contact.getEmailAddress());
                }
                else if (contact.getEmailType().toLowerCase().equals(emailType.toLowerCase())) {                    
                    addresses.add(contact.getEmailAddress());    
                }                
            }
        }
        if (addresses.isEmpty()) {
            return "";
        }
        else {
            String emails = java.util.Arrays.toString(addresses.toArray());        
            return emails.substring(1, emails.length()-1);
        }
    }
}

