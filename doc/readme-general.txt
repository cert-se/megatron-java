Introduction
============
Megatron is a tool implemented by CERT-SE which collects and analyses bad IPs,
for example from Shadowserver. Apart from abuse mail handling, Megatron can be
used to collect statistics, convert log files, and do log file analyses during
incident handling.


Major features:

* Flexible parsing: Many input sources are supported without any coding.
  Regular expressions are used to extract tokens which are bound to variables.
  A variable corresponds to a field in the database and can be used in
  for example filters and templates.

* Organization matching: Megatron tries to match every input record to an
  organization using its IP-blocks, ASNs, and domain names. Each organization
  have a priority which makes it possible to filter out important records.
 
* Database: Input records are stored in a relational database (MySQL is
  default), making data mining possible.

* Filtering: A wide variety of filters can be used to filter input records.

* Data decoration: Data may be added to the record before it is saved to the
  database. Types of lookups: IP --> ASN, IP --> country code, IP --> hostname,
  hostname --> IP, URL --> hostname, hostname --> country code, and 
  IP --> geolocation. 

* Performance: Large data volumes can be handled. All lookups except DNS queries
  are local, for instance ASN lookups uses database queries by importing a BGP
  routing table to the database.


Important but minor features:

* Untyped data: Tokens in an input record that do not match a predefined 
  database field may be saved to a name-value field or a freetext field.

* Notification email: If a job contains high priority records, a notification
  email may be sent to the handler on duty.

* Templates: Abuse emails and export files can be customized using templates.

* Export: Data may be exported to a customized format. Megatron can also convert
  data from one format to another, for example from syslog to pwhois, without 
  saving any data to the database. Filtering and data decoration may be used to
  refine the content.
 
* Time zone: Uses time zone in the timestamp if given, or the time zone may be
  specified per input source.

* Time formats: May be specified as a mask.

* Feed directory: Files in the so called slurp directory are matched to a 
  configuration using the pattern of the filename. Thus, no configuration needs
  to be specified.

* Diff-filter: If an input source contains both new and old records, a diff can
  be used to filter out only new items.

* RSS: Job status and statistics are exported to RSS feeds.

* Organization administration: Organizations may be imported or updated in
  batch, or managed in the interactive user interface. Email addresses may be
  added or deleted in batch.

* Warnings: Hashvalue for files is checked to avoid duplicates. If timestamps
  in an input file is older than a specified number of seconds, a warning is issued.

* IP quarantine: A machine is quarantined for a certain time to avoid sending
  repeated abuse mails about the same IP.


Megatron is a command line tool and database searches are made using SQL. Next
version will include a web GUI, in which the user can make searches and
manage organizations.

Megatron is released as open source using the Apache license. CERT-SE does not provide
any kind of support, and Megatron is provided "as is" without warranty of any kind.
However, we are a bunch of nerds at CERT-SE and we would love to hear from you to 
discuss technical stuff. Send questions, criticism, flatter, feature requests, etc.
to <cert@cert.se>. 


Concepts
========
The following concepts and terminology are good to know in order to understand
Megatron:

* Batch oriented: Megatron is batch oriented, i.e. a file is processed and the
  result is stored in the database. Then a mail job can be executed to send
  abuse mails. Sometimes a "job" (file processing and storing) is called 
  "log job" to differ it from a "mail job" (sending mail). 

* Job Type: The configuration for a specific input source is called job type. 
  A job type specifies e.g. how to parse the input file, filters, templates,
  and timestamp format. One job type may be applicable on several input
  sources if the file format is the same. This is usual the case with RBL
  files. The directory "conf/job-type" contains job types. The switch
  --job-type specifies which job type to use.
  
* Hierarchical Properties: Properties from "megatron-globals.properties" are
  inherited by job type properties. Thus, properties in a job type overrides
  the global properties. 

* Job Name: Every job have an unique name. Format: <job type>_<timestamp>.
  Example: "shadowserver-drone_2009-12-11_120827".

* Slurp: Files in the so called slurp directory are matched to a configuration 
  using the pattern of the filename. Thus, no configuration needs to be 
  specified. A common usage of Megatron is to download files from different
  sources to the slurp directory and then run Megatron with the --slurp switch.

* Download Scripts: In production, scripts that downloads input files to the
  slurp directory is used to avoid downloading by hand.

* Matching: Megatron tries to match every input record to an organization using
  its IP-blocks, ASNs, and domain names. The order of matching is the 
  following: IP-block, ASN, and domin name. Thus, IP-blocks are macthed first.

* Secondary Organization: For a log entry, information about a secondary 
  machine may be added. It may be for example a C&C server or DDoS victim.
  Use the switch --use-org2 to send abuse mail to the secondary organization.     

* Unique IP-blocks, ASNs, and domains: No overlapping IP blocks are allowed.
  ASN and domain names must be unique as well. One implication of this
  design choice is that no IP-blocks can be assigned to ISPs, becuase ISPs
  assigns IP-blocks to its customers.  
  
* UTC: All timestamps in Megatron are in UTC. Timestamps from input files are
  converted to UTC.
