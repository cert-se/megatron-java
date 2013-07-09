Quick Start
===========
To get a grasp of Megatron, we recommend skimming the following:  
  - Overview documentation: doc/readme-general
  - Database schema: sql/megatron-schema.sql
  - Configuration files: conf/dev/megatron-globals.properties and conf/job-type  
  - The source code. 

To install Megatron, follow the instructions below.


Directory Structure
===================
The distribution (megatron-pub-X.X.X.tar.gz) contains the following:
directories:
  - megatron-java: This is the main project. Binaries, source code, 
    documentation, and configuration files are found here. 
  - megatron-python: Helper scripts written in Python, which for example 
    send unsent jobs in batch.


Database
========
Megatron requires a database to execute. Only MySQL 5.1 have been tested, but 
any JDBC compliant database should work. 

Installation of MySQL:
  - Windows: http://dev.mysql.com/downloads/
  - Ubuntu: sudo apt-get install mysql-server 

The following steps will create a Megatron database:
  - mysql -u root -p
  - create database megatron;
  - use megatron;
  - create user 'megatron'@'localhost' identified by 'megatron';
  - grant all on megatron.* to megatron@localhost;
  - source /home/foo-user/megatron/sql/megatron-schema.sql

Do not forget to change path to "megatron-schema.sql". Note that MySQL for 
Windows are picky with backslashes in filenames; use slashes instead.

For obvious reasons, change the password in the production environment. The 
password is specified in the following configuration files:
  - megatron-globals.properties
  - hibernate.cfg.xml

Test the database using a JDBC client tool. We warmly recommend DbVisualizer 
<http://www.minq.se/products/dbvis/>. Use the following configuration:
  - Database URL: jdbc:mysql://localhost:3306/megatron
  - JDBC driver is included: lib/mysql-connector.jar
  - Driver classname: com.mysql.jdbc.Driver


Quick Installation (Windows)
============================
* Create the database (see above).

* Ensure that Java is installed (Java 1.7 or later is required by Megatron): 
    > java -version
  
* To install Java, download JDK from Suns website:
  http://java.sun.com/javase/downloads/

* Unpack the distribution (megatron-pub-X.X.X.tar.gz) into the installation 
  directory, e.g. "C:\Users\foo-user\megatron". If "C:\Program Files" in Vista
  is used, then Megatron must be run as Administrator.

* Download additional MaxMind databases (GeoIP.dat is included) to 
  "conf/geoip-db" and unzip them:
    http://geolite.maxmind.com/download/geoip/database/GeoLiteCity.dat.gz 
    http://geolite.maxmind.com/download/geoip/database/asnum/GeoIPASNum.dat.gz

* Test the following commands (see "Installation IDE (Eclipse)" for details)
  from a command prompt: 
    - Usage:
      > megatron.bat
    - Populate db: 
      > megatron.bat --import-contacts
    - Run a Shadowserver file:
      > megatron.bat --job-type shadowserver-drone test-data/2009-06-08-drone-report-se.log
    - Convert a file with IP addresses to a tab separated file (best viewed with a spreadsheet):
      > megatron.bat --job-type ip-flowing --export --no-db test-data/multiple-ips-per-line3.log
    - Convert a syslog file to a tab separated file:
      > megatron.bat --job-type syslog-ip-plus-host --export --no-db test-data/syslog-ip-plus-host.log

* Log messages are written both to file and to stdout, for testing purposes. 
  Edit "conf/dev/log4j.properties" to turn off stdout logging.
  
* Modify CERT-SE specific stuff in the configuration file, "megatron-globals.properties".
  Search for "TODO Change on install", "CERT-SE", and "SE".

* To be able to bind an input row to an organization, add organizations and 
  machine information (IP ranges, domain names, ASN, etc.) to the database.
  Use --import-contacts and "conf/dev/systemdata.txt".


Quick Installation (Unix)
=========================
* Create the database (see above).

* Ensure that Java is installed (Java 1.7 or later is required by Megatron): 
    $ java -version
  
* To install Java, execute the following in Ubuntu:
    $ sudo apt-get install openjdk-6-jdk
  
  Binaries for other Unices may be downloaded from Sun's web site:    
    http://java.sun.com/javase/downloads/

* Unpack the distribution (megatron-pub-X.X.X.tar.gz) into the installation  
  directory, e.g. "~/megatron".

* Set execute permission:
    $ chmod u+x megatron-dev.sh

* Download additional MaxMind databases (GeoIP.dat is included):
    $ cd conf/geoip-db
    $ wget http://geolite.maxmind.com/download/geoip/database/GeoLiteCity.dat.gz && gunzip GeoLiteCity.dat.gz 
    $ wget http://geolite.maxmind.com/download/geoip/database/asnum/GeoIPASNum.dat.gz && gunzip GeoIPASNum.dat.gz

* Test the following commands (see "Installation IDE (Eclipse)" for details): 
    - Usage:
      $ ./megatron-dev.sh
    - Populate db: 
      $ ./megatron-dev.sh --import-contacts
    - Run a Shadowserver file:
      $ ./megatron-dev.sh --job-type shadowserver-drone test-data/2009-06-08-drone-report-se.log
    - Convert a file with IP addresses to a tab separated file (best viewed with a spreadsheet):
      $ ./megatron-dev.sh --job-type ip-flowing --export --no-db test-data/multiple-ips-per-line3.log
    - Convert a syslog file to a tab separated file:
      $ ./megatron-dev.sh --job-type syslog-ip-plus-host --export --no-db test-data/syslog-ip-plus-host.log

* Log messages are written both to file and to stdout, for testing purposes. 
  Edit "conf/dev/log4j.properties" to turn off stdout logging.
  
* Modify CERT-SE specific stuff in the configuration file, "megatron-globals.properties". 
  Search for "TODO Change on install", "CERT-SE", and "SE".

* To be able to bind an input row to an organization, add organizations and 
  machine information (IP ranges, domain names, ASN, etc.) to the database.
  Use --import-contacts and "conf/dev/systemdata.txt".


Deployment (Unix)
=================
* Follow the steps in "Quick Installation (Unix)", but instead of having all
  files and directories in a single application directory split them into
  Unix standard directories, for example:
    - megatron.sh --> /usr/local/megatron/bin/megatron.sh
    - lib/ --> /usr/local/megatron/lib/
    - dist/sitic-megatron.jar --> /usr/local/megatron/lib/sitic-megatron.jar
    - conf/dev/ --> /etc/megatron/ [except for systemdata.txt]
    - conf/dev/systemdata.txt --> /var/megatron/import/systemdata.txt
    - conf/hibernate-mapping/ --> /etc/megatron/hibernate-mapping/
    - conf/job-type/ --> /etc/megatron/job-type/
    - conf/template/ --> /etc/megatron/template/
    - conf/geoip-db/ --> /etc/megatron/geoip-db/

* The following directories will be created:
    - /var/megatron/flash-xml/
    - /var/megatron/diff-processor-old-files/
    - /var/megatron/log/
    - /var/megatron/rss/
    - /var/megatron/export/
    - /var/megatron/slurp/
    
* Directory names are specified in "megatron-globals.properties" and 
  "megatron.sh".

* Create a user, e.g. u_megatron, that owns all the Megatron files and executes
  "megatron.sh".

* Modify CERT-SE specific stuff in the following configuration files:
    - megatron-globals.properties 
      Search for "TODO Change on install", "CERT-SE", and "SE".
    - job-type/*.properties
      Search for "SE" (filter.countryCodeFilter.includeCountryCodes) and 
      "CERT-SE" (mail.subjectTemplate).
    - template/mail/*
      All mail templates are CERT-SE specific.  

* We recommend having crontabbed download scripts that will download input 
  files to the slurp directory and then call Megatron with the --slurp switch.


Installation IDE (Eclipse)
==========================
* Create the database (see above).

* Create project from GitHub: megatron-java
    - File - Import - Projects from GIT
    - Location of GIT repository: URI
    - URL: https://github.com/cert-se/megatron-java.git
    - Select "master"
    - Select "Import as General Project"
  A Java-project should now have been created as specified in ".project".  

* Ensure that the following project properties are assigned:
    - Add src and src-test as source folders (Java Build Path - Source Tab).
    - Change "Default output folder" to "megatron-java/classes-eclipse" 
      (Java Build Path - Source Tab).
    - Add all .jar-files in lib to project (Java Build Path - Libraries Tab).
    - Add JUnit 4 (Java Build Path - Libraries Tab - Add Library... - JUnit).
    - Set "Compiler compliance level" to 1.7: (Java Compiler). Both JDK 1.5
      and 1.6 is no longer supported.

* Download additional MaxMind databases (GeoIP.dat is included) to 
  "conf/geoip-db" and unzip them:
    http://geolite.maxmind.com/download/geoip/database/GeoLiteCity.dat.gz 
    http://geolite.maxmind.com/download/geoip/database/asnum/GeoIPASNum.dat.gz

* Rebuild project (Project - Clean...)

* Test Megatron by running it without any CLI argumnets, which will show usage. 
  Use the launch file "megatron-usage.launch" (Run - Debug Configurations... - 
  Java Application - megatron-usage).

* Populate the database by running Megatron with the --import-contacts switch.
  This will import testdata which are specified in "conf/dev/systemdata.txt".
  The launch file "launch/megatron-import-contacts.launch" may be used.

* Modify CERT-SE specific stuff in the configuration file, "megatron-globals.properties". 
  Search for "TODO Change on install", "CERT-SE", and "SE".

* Now it's time to run something more interesting. Use the following switches 
  to run a small Shadowserver-file:
    --job-type shadowserver-drone test-data/2009-06-08-drone-report-se.log
  The launch file "megatron-shadowserver.launch" contains the switches.
  Browse around in the database to see how the job have been saved. Look in
  the following tables: log_entry, original_log_entry, job, and 
  additional_item. 

* Another example is to convert a syslog file without saving it to db:
    --job-type syslog-ip-plus-host --export --no-db test-data/syslog-ip-plus-host.log
  Launch file: megatron-syslog-export-no-db 
  The result file will be located in "tmp/export/".
   
* Play around with the testdata and configurations. A configuration for a 
  specific input format is called job type, and files are located in the
  "conf/job-type/" directory.

* Other IDE: It should be no problem to use another IDE than Eclipse; the 
  project have standard structure with for example an Ant build script.
  Use the same directory structure. Make sure that the working directory is 
  set to the same directory "build.xml" is located. The main class is 
  "Megatron", which is found in the default package. The files ".classpath" 
  and ".project" are Eclipse specific and may be deleted. 

* Happy hacking! 


How to update
=============
Use the following steps to upgrade an existing installation in production:

* Read "release-notes.txt" and follow instructions in the section "Deployment 
  changes" and "Config changes".
  
* Diff new "megatron-globals.properties" with existing one. Add new properites
  and remove deprecated ones.

* Diff all files in "conf/job-type/" and "conf/template/".

* Copy new or modified binaries:
    - dist/sitic-megatron.jar (always updated)
    - lib/*.jar (third-party class libraries) 


License
=======
Megatron is distributed under the terms of the Apache Software Foundation
license version 2.0, included in the file LICENSE in the root of the project.


Support and Warranty
====================
CERT-SE does not offer any support, and Megatron is provided "as is" without 
warranty of any kind.

However, we are a bunch of nerds at CERT-SE and we would love to hear from you 
to discuss technical stuff. Send questions, criticism, flatter, feature 
requests, etc. to <cert@cert.se>.


Files not Included
==================
CERT-SE have not included the following files:
  - Configuration files used in production (located in conf/prod).
  - Full BGP dump file. To show the format a small testfile is included: 
    "test-data/bgp-table-small.txt". This mean that lookups from IP to ASN
    does not work out of the box.
  - Miscellaneous downloads scripts and admin scripts.


Concepts and Terminology
========================
See "doc/readme-general.txt".
