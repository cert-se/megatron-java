### Megatron - A System for Abuse Handling and Incident Handling
Megatron is a tool implemented by CERT-SE which collects and analyses log files with bad 
machines, e.g. from Shadowserver. Apart from abuse mail handling, Megatron can be used to 
collect statistics, convert log files, and do log file analysis during incident handling.

Major features:
* Flexible parsing: many input sources are supported without any coding. Regular expressions are 
  used to extract tokens which are bound to variables. A variable corresponds to a field in the 
  database and can be used in for example filters and templates.
* Organization matching: Megatron tries to match every input record to an organization using its 
  IP-blocks, ASNs, and domain names. Each organization have a priority which makes it possible to 
  filter out important records.
* Database: input records are stored in a relational database (MySQL is default), making data mining possible.
* Filtering: a wide variety of filters can be used to filter input records.
* Data decoration: data may be added to the record before it is saved to the database. Types of lookups: 
  IP to ASN, IP to country code, IP to hostname, hostname to IP, URL to hostname, hostname to 
  country code, and IP to geolocation.
* Performance: large data volumes can be handled. All lookups except DNS queries are local, for instance 
  ASN lookups uses database queries by importing a BGP routing table to the database.

For all features, see [readme-general.txt](https://github.com/cert-se/megatron-java/blob/master/doc/readme-general.txt)


### Example of Usage
Convert a file with IP addresses to a pipe-separated file with IP, AS, country code, and hostname:
```      
$ ./megatron-dev.sh --job-type ip-flowing --export --no-db test-data/multiple-ips-per-line3.log
```

As above but output file is tab-separated and contains geolocation data:
```
$ ./megatron-dev.sh --job-type ip-flowing-verbose --export --no-db test-data/multiple-ips-per-line3.log
```

Process file and save result in the database:
```
$ ./megatron-dev.sh --job-type shadowserver-drone test-data/2009-06-08-drone-report-se.log
```

Preview of mail to be sent:
```
$ ./megatron.sh --job shadowserver-drone_2013-06-22_160142 --id 4242 --mail-dry-run
```

Send emails for the job:
```
$ ./megatron.sh --job shadowserver-drone_2009-06-22_160142 --id 4242 --mail
```

Megatron is designed to automate day-to-day abuse handling. Running Megatron from the
command line is only necessary in special cases.  


### Installation
Megatron is cross-platform as it is implemented in Java and Python. It has been tested on Windows 7 and Ubuntu 12.04. 
Megatron requires a MySQL database. Do not worry, the installation is easy and described in detail in this document:
[readme-install.txt](https://github.com/cert-se/megatron-java/blob/master/doc/readme-install.txt)


### Documentation
To get a grasp of Megatron, we recommend skimming the following:
* Overview documentation: [readme-general.txt](https://github.com/cert-se/megatron-java/blob/master/doc/readme-general.txt)
* Database schema: [megatron-schema.sql](https://github.com/cert-se/megatron-java/blob/master/sql/megatron-schema.sql)
* Configuration files: [conf/dev/megatron-globals.properties](https://github.com/cert-se/megatron-java/blob/master/conf/dev/megatron-globals.properties) 
and [conf/job-type/*](https://github.com/cert-se/megatron-java/tree/master/conf/job-type)
* The source code.

TODO presentation slides and Wiki


### License
Megatron is distributed under the terms of the Apache Software Foundation license version 2.0, included in the 
file LICENSE in the root of the project.


### Support and Warranty
CERT-SE does not offer any support, and Megatron is provided "as is" without warranty of any kind.

However, we are a bunch of nerds at CERT-SE and we would love to hear from you to discuss technical stuff. 
Send questions, criticism, flatter, feature requests, etc. to <cert@cert.se>. Our PGP keys can be found on 
CERT-SE's [contact page](https://www.cert.se/om-cert-se).
