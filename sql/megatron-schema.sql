# Database schema for Megatron (MySQL)
#
# More info:
#   * Storing an IP address in a database table: http://arjen-lentz.livejournal.com/44290.html
#   * MySQL FULLTEXT Searching: http://www.onlamp.com/pub/a/onlamp/2003/06/26/fulltext.html
#   * VARCHAR-fields are space-padded: http://dev.mysql.com/doc/refman/5.1/en/static-format.html
#   * VARCHAR vs. TEXT: http://lists.mysql.com/mysql/1623
#   * "?" not allowed in comment. Using "[questionmark]" instead.
#


# Log record
DROP TABLE IF EXISTS log_entry;
CREATE TABLE log_entry (
    id                      INT UNSIGNED NOT NULL AUTO_INCREMENT,   
    created                 INT UNSIGNED NOT NULL,                  # Timestamp when written to db
    log_timestamp           INT UNSIGNED NOT NULL,                  # Timestamp in UTC from log line

    ip_address              INT UNSIGNED,                           # Primary ip-address, e.g. rogue host.
    hostname                VARCHAR(255),                           # Primary host        
    port                    SMALLINT UNSIGNED,                      # Port for primary host  
    asn                     INT UNSIGNED,                           # AS-number for primary host
    country_code            CHAR(2),                                # Country for primary host, e.g. "SE". Always in upper-case.

    ip_address2             INT UNSIGNED,                           # Secondary ip-address, e.g. victim.
    hostname2               VARCHAR(255),
    port2                   SMALLINT UNSIGNED,
    asn2                    INT UNSIGNED,
    country_code2           CHAR(2),

    ip_range_start          INT UNSIGNED,                           # Start ip-address in range (inclusive)
    ip_range_end            INT UNSIGNED,                           # End ip-address in range (inclusive)
    
    url                     TEXT, 
    
    original_log_entry_id   INT UNSIGNED,                           # Key in original_log_entry
    
    org_id                  MEDIUMINT UNSIGNED,                     # Matching organisation for primary id if found 
    org_id2                 MEDIUMINT UNSIGNED,                     # Matching organisation for secondary id if found

    job_id                  INT UNSIGNED NOT NULL,                  # Job that have processed this log entry
  
    PRIMARY KEY (id),
    INDEX (ip_address),
    INDEX (ip_address2),
    INDEX (ip_range_start, ip_range_end),
    INDEX (asn),
    INDEX (asn2),
    INDEX (org_id),
    INDEX (org_id2),
    INDEX (job_id)
) ENGINE = MYISAM;


# Original log line
DROP TABLE IF EXISTS original_log_entry;
CREATE TABLE original_log_entry (
    id            INT UNSIGNED NOT NULL AUTO_INCREMENT,
    created       INT UNSIGNED NOT NULL,    # Same as log_entry.created (can be used for table partitioning)
    entry         TEXT NOT NULL,            # Original log line

    PRIMARY KEY (id)
    # FULLTEXT(entry)
) ENGINE = MYISAM;


# Addtional freetext fields to a log record. 
DROP TABLE IF EXISTS free_text;
CREATE TABLE free_text (
    log_entry_id  INT UNSIGNED NOT NULL,        # Key in log_entry
    text_index    SMALLINT UNSIGNED NOT NULL,   # Index for text, T0, T1, ..., Tn
    text          TEXT NOT NULL,                # Actual free text

    INDEX (log_entry_id)
) ENGINE = MYISAM;


# Addtional parsed, untyped items to a log record. 
DROP TABLE IF EXISTS additional_item;
CREATE TABLE additional_item (
    log_entry_id  INT UNSIGNED NOT NULL,    # Key in log_entry
    name          VARCHAR(128) NOT NULL,    # Key
    value         TEXT NOT NULL,            # Value

    INDEX (log_entry_id)
) ENGINE = MYISAM;


# Type for a log entry, e.g. "Phishing", "DDOS" etc.
DROP TABLE IF EXISTS entry_type;
CREATE TABLE entry_type (
    id    MEDIUMINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name  VARCHAR(128) NOT NULL UNIQUE,
  
    PRIMARY KEY (id)
) ENGINE = MYISAM;


# Job type, e.g. "shadowserver-ddos". Name correspondes to properties- and template file.
DROP TABLE IF EXISTS job_type;
CREATE TABLE job_type (
    id                    MEDIUMINT UNSIGNED NOT NULL AUTO_INCREMENT,
    entry_type_id         MEDIUMINT UNSIGNED NOT NULL,                # Key in entry_type
    name                  VARCHAR(128) NOT NULL UNIQUE,               # Name, e.g. "shadowserver-ddos"
    enabled               BOOLEAN NOT NULL,                           # Is this job_type active[questionmark]
    source_description    TEXT,                                       # Multi-line, e.g. URL to source plus description. 
    comment               TEXT,                                       # Multi-line comment

    PRIMARY KEY (id)
) ENGINE = MYISAM;


# Job that process a file. One job may have several mail-jobs.
DROP TABLE IF EXISTS job;
CREATE TABLE job (
    id            INT UNSIGNED NOT NULL AUTO_INCREMENT,
    job_type_id   MEDIUMINT UNSIGNED NOT NULL,
    name          VARCHAR(128) NOT NULL UNIQUE, # Job instance name, e.g. "shadowserver-ddos_2009-04-16". 
    filename      VARCHAR(128) NOT NULL,        # Filename without path
    file_hash     VARCHAR(40) NOT NULL,         # MD5 or SHA1 value in hex (md5sum format)
    file_size     INT UNSIGNED NOT NULL,        # File size in bytes
    started       INT UNSIGNED NOT NULL,        # Time when job started.
    finished      INT UNSIGNED,                 # Time when job terminated. Null if pending.
    error_msg     TEXT,                         # Null if job successful
    comment       TEXT,                         # Multi-line comment
    processed_lines     INT UNSIGNED,           # No. of processed lines (after possible split or merge)

    PRIMARY KEY (id),
    INDEX (name),
    INDEX (file_hash)
) ENGINE = MYISAM;


# Mail job.
DROP TABLE IF EXISTS mail_job;
CREATE TABLE mail_job (
    id            INT UNSIGNED NOT NULL AUTO_INCREMENT,
    job_id        INT UNSIGNED NOT NULL,                # Key in job 
    use_primary_org BOOLEAN NOT NULL,                   # Mail sent to Primary or secondary org
    started       INT UNSIGNED NOT NULL,                # Time when mail-job started.
    finished      INT UNSIGNED,                         # Time when mail-job terminated. Null if pending.
    error_msg     TEXT,                                 # Null if mail-job successful
    comment       TEXT,                                 # Multi-line comment
    
    PRIMARY KEY (id),
    INDEX (job_id)
) ENGINE = MYISAM;


# Mapping table: mail_job <--> log_entry
DROP TABLE IF EXISTS mail_job_log_entry_mapping;
CREATE TABLE mail_job_log_entry_mapping (
    id              INT UNSIGNED NOT NULL AUTO_INCREMENT,
    mail_job_id     INT UNSIGNED NOT NULL,          # Key in mail_job 
    log_entry_id    INT UNSIGNED NOT NULL,          # Key in log_entry    
    
    PRIMARY KEY (id)
) ENGINE = MYISAM;


# Contact db: organization.
DROP TABLE IF EXISTS organization;
CREATE TABLE organization (
    id                MEDIUMINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name              VARCHAR(128) NOT NULL UNIQUE,
    registration_no   VARCHAR(11),                                # Registration number should be "NOT NULL UNIQUE"
                                                                  # but it will not work since we do not have it for all orgs.
    enabled           BOOLEAN NOT NULL,                           # Is this organisation active[questionmark]
    prio_id           MEDIUMINT UNSIGNED NOT NULL,                # Key in prio
    country_code      CHAR(2),                                    # Country code, e.g. "SE". Always in upper-case.
    language_code     CHAR(2),                                    # Language code, e.g. "sv". Always in lower-case. 
    email_addresses   TEXT,                                       # Comma separated list of email addresses
    description       TEXT,                                       # Multi-line description
    comment           TEXT,                                       # Multi-line comment
    auto_update_email BOOLEAN NOT NULL,                           # Auto update of email contact adresses allowed
    auto_update_match_fields BOOLEAN NOT NULL,                    # Autot update of AS-no, domain name or IP-ranges allowed
    created           INT UNSIGNED NOT NULL,                      # Timestamp when first written to db
    last_modified     INT UNSIGNED NOT NULL,                      # Timestamp when last written to db
    modified_by       VARCHAR(64) NOT NULL,                       # Who modified this record[questionmark] 

    PRIMARY KEY (id)
) ENGINE = MYISAM;


# Contact db: contact.
DROP TABLE IF EXISTS contact;
CREATE TABLE contact (
    id                MEDIUMINT UNSIGNED NOT NULL AUTO_INCREMENT,
    org_id            MEDIUMINT UNSIGNED NOT NULL,                # Organization that the contact belong to
    first_name        VARCHAR(64),                               # Contact first name
    last_name         VARCHAR(64),                               # Contact last name
    role              VARCHAR(64),                               # Contact work role/responsability i.e. sysadmin, manager.
    phone_number      VARCHAR(32),                               # Contact phone number
    email_address     VARCHAR(128) NOT NULL,                      # Contact email address
    email_type        CHAR(4) NOT NULL,                           # Email address type, i.e. To:, CC:, BCC.
    enabled           BOOLEAN NOT NULL,                           # Is the contact active and valid[questionmark]
    comment           TEXT,                                       # Multi-line comment
    created           INT UNSIGNED NOT NULL,                      # Timestamp when first written to db
    last_modified     INT UNSIGNED NOT NULL,                      # Timestamp when last written to db
    modified_by       VARCHAR(64) NOT NULL,                       # Who modified this record[questionmark]	
    external_reference VARCHAR(255),
    auto_update_email BOOLEAN NOT NULL,
    PRIMARY KEY (id)
) ENGINE=MyISAM;








# Contact db: prio for organisation.
DROP TABLE IF EXISTS prio;
CREATE TABLE prio (
    id    MEDIUMINT UNSIGNED NOT NULL AUTO_INCREMENT,     
    name  VARCHAR(128) NOT NULL UNIQUE,                   # Name, e.g. "Myndighet", "Kommun" etc. 
    prio  SMALLINT UNSIGNED NOT NULL UNIQUE,              # Numeric priority, 0-100.

    PRIMARY KEY (id),
    INDEX (prio)
) ENGINE = MYISAM;


# Contact db: AS-number for organisation.
DROP TABLE IF EXISTS asn;
CREATE TABLE asn (
    id          MEDIUMINT UNSIGNED NOT NULL AUTO_INCREMENT,
    org_id      MEDIUMINT UNSIGNED NOT NULL,                  # Key in organisation
    asn         INT UNSIGNED NOT NULL UNIQUE,                 # An AS-number may be 2-or 4 bytes long  

    PRIMARY KEY (id),
    INDEX (asn)
) ENGINE = MYISAM;


# Contact db: domain-names for organisation.
DROP TABLE IF EXISTS domain_name;
CREATE TABLE domain_name (
    id            MEDIUMINT UNSIGNED NOT NULL AUTO_INCREMENT,
    org_id        MEDIUMINT UNSIGNED NOT NULL,                  # Key in organisation
    domain_name   VARCHAR(255) NOT NULL UNIQUE,                 # Always in lower-case    

    PRIMARY KEY (id),
    INDEX (domain_name)
) ENGINE = MYISAM;


# Contact db: ip-ranges for organisation.
DROP TABLE IF EXISTS ip_range;
CREATE TABLE ip_range (
    id              MEDIUMINT UNSIGNED NOT NULL AUTO_INCREMENT,
    org_id          MEDIUMINT UNSIGNED NOT NULL,                # Key in organisation
    start_address   INT UNSIGNED NOT NULL UNIQUE,               # Start ip-address in range (inclusive)
    end_address     INT UNSIGNED NOT NULL UNIQUE,               # End ip-address in range (inclusive)
    net_name        VARCHAR(64),                                # Net name of the ip range owner

    PRIMARY KEY (id),
    INDEX (start_address, end_address)
) ENGINE = MYISAM;


# Lookup table for ASN: IP-address to ASN. Data imported from a BGP prefix table dump.
DROP TABLE IF EXISTS asn_lookup;
CREATE TABLE asn_lookup (
    start_address   INT UNSIGNED NOT NULL,
    end_address     INT UNSIGNED NOT NULL,
    asn             INT UNSIGNED NOT NULL,

    PRIMARY KEY (start_address, end_address),
    INDEX (start_address),
    INDEX (end_address)
) ENGINE = MYISAM;


# Additional indices that will increase performance.
# SQL queries for quarantine will be must faster.   

CREATE INDEX mj_started ON mail_job (started);
CREATE INDEX mj_finished ON mail_job (finished);
CREATE INDEX mjlem_job_id ON mail_job_log_entry_mapping (mail_job_id);
CREATE INDEX mjlem_log_id ON mail_job_log_entry_mapping (log_entry_id);
