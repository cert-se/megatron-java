package se.sitic.megatron.entity;

import se.sitic.megatron.entity.base.BaseContact;

public class Contact extends BaseContact {
    private static final long serialVersionUID = 1L;

    /* [CONSTRUCTOR MARKER BEGIN] */
    public Contact() {
        super();
    }

    /**
     * Constructor for primary key
     */
    public Contact(java.lang.Integer id) {
        super(id);
    }

    /**
     * Constructor for required fields
     */
    public Contact(

            java.lang.Integer id, java.lang.String firstName,
            java.lang.String lastName, java.lang.String comment,
            java.lang.String emailAddress, java.lang.String emailType,
            java.lang.String phoneNumber, java.lang.String role,
            java.lang.String externalReference,
            java.lang.Integer organizationId, boolean enabled,
            java.lang.Long created, java.lang.Long lastModified,
            java.lang.String modifiedBy, boolean autoUpdateEmail) {

        super(id, firstName, lastName, comment, emailAddress, emailType,
                phoneNumber, role, externalReference, organizationId, enabled,
                created, lastModified, modifiedBy, autoUpdateEmail);
    }

    /* [CONSTRUCTOR MARKER END] */

    @Override
    public int compareTo(Object obj) {
        
        return this.getEmailAddress().compareToIgnoreCase(((Contact)obj).getEmailAddress());
    }
}
