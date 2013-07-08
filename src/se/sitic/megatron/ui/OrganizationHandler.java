package se.sitic.megatron.ui;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.db.DbException;
import se.sitic.megatron.db.DbManager;
import se.sitic.megatron.entity.ASNumber;
import se.sitic.megatron.entity.DomainName;
import se.sitic.megatron.entity.IpRange;
import se.sitic.megatron.entity.Organization;
import se.sitic.megatron.entity.Priority;
import se.sitic.megatron.util.AppUtil;
import se.sitic.megatron.util.DateUtil;
import se.sitic.megatron.util.IpAddressUtil;
import se.sitic.megatron.util.SqlUtil;



public class OrganizationHandler {

    private DbManager dbManager = null; 
    private static boolean debug = false;
    
    // Commands List, Add, Edit, Show, Quit, Help, Debug, Mail(-addresses)
    private enum Commands { L, A, E, S, Q, H, D, M }

    private BufferedReader in = null;   
    private TypedProperties props = null;
    private String currentUser = null; 
    private boolean orgChanged = false;
        
    static private final String DEFAULT_CC = "SE";
    static private final String DEFAULT_LC = "sv";
    
    
    public OrganizationHandler(TypedProperties props) {

        in = new BufferedReader(new InputStreamReader(System.in));
        this.props = props;
                
        try {
            dbManager = DbManager.createDbManager(this.props);
        } catch (DbException e) {
            println("Error: Establish a DB session");
            debug("DBException at createDbManager");
            e.printStackTrace();
        }
        
        while (this.currentUser == null) {
            String user = readInput("Enter user id: ");
            if (user != null && user.trim() != "") {
                currentUser = user;
            }            
        }
        
       welcome();
       usage();
    }
    
    public void startUI() {
        
        while(true) {
            this.processCommand(this.readCommand());
        }
    }
        
    private void println(String msg){        
        System.out.println(msg);
    }
    
    private void printf(String msg, Object... args) {        
        System.out.printf(msg, args);        
    }
    
    private String readCommand() {
        
        String command = readInput("\n> ");
        if (command != null && command.length() > 0) {
            command = (String) command.subSequence(0, 1);
        }
        else {
            return readCommand();
        }
        return command;
    }
    

    private String readInput(String promt){

        String input = null;
        
        try{
            System.out.print(promt);            
            input = in.readLine();
        } catch (java.io.IOException ioe) {   
            ioe.printStackTrace();
        }
        return input;
        
    } 
    
    private String readMultiLineInput(String msg) {
        
        String fullLine = "";
        
        String input = "";
        println(msg);
        while (input.equals(".") == false) {
            fullLine = fullLine + input + "\n";
            input = readInput(">");
        }
        
        return fullLine;
        
    }
    
    private void welcome(){
        println("\nWelcome " + currentUser + " to Megatron Organization Manager!");
        println("");
    }
    
    private void usage(){
        
        println("Valid commands are:  \n\tL - List organizations \n\tA - Add organization \n\tE - Edit organization \n\tS - Show organization \n\tM - Edit contact mail addresses\n\tQ - Quit \n\tH - Help");
        
    }
    
    private void help() {
        println("Don't panic! :P");        
        usage();        
        println("\nList organizations takes a string and list organizations with names containing the string,");
        println("an empty string will result in all organizations being listed.");
        println("Add organization adds a new organization to the system.");
        println("Edit organization takes org-id, which can be seen in the list output.");
        println("Show organization takes org-id as input.");
        println("Mail address, edit organization e-mail addresses only, takes org-id as input.");
        println("Quit to exit this program.");
        println("Help, shows this help message.");
        println("");
        
    }

    private void toggleDebug() {
        
        debug = !debug; 
        println("Debug is now " + Boolean.toString(debug));
        
    }
    
    private void debug(String msg) {
        
        if (debug) {
            println("DEBUG: " + msg);
        }
        
    }
    
    private void debug(Exception e) {
        
        if (debug) {
            e.printStackTrace();
        }
        
    }
    
    private void processCommand(String command){
        
        try {
            switch(Commands.valueOf(command.toUpperCase())) {

            case L: listOrganizations(); return;
            case A: addOrganisation(); return;  
            case E: editOrganization(null); return; 
            case S: showOrganization(null); return;
            case Q: quit(); return;                  
            case H: help(); return;
            case M: editOrganizatonEmailAddress(); return;
            case D: toggleDebug(); return;

            }
        }
        catch (java.lang.IllegalArgumentException e){
            println("Unknown command: " + command);
            usage();
        }
    }
    
    private boolean confirm(String message) {
        
        String input = readInput("\n" + message + " y/n (N): ").toUpperCase();
        boolean result = false;
        
        if (input.equals("Y") || input.equals("N") || input.trim().equals("")) {
            result = input.equals("Y");
        }
        else {
            println("Error: Bad input.");            
        }
            
        return result;
        
    }
    
    private void listOrganizations() {

        String orgSearchName = this.readInput("Organization name to seach for (<enter>=all) : ");

        try {
            List<Organization>  organizations = dbManager.searchOrganizations(orgSearchName, 0, 1000);

            if (organizations.size() > 0) {                
                println("\nOrganizations:");
                println("+------+----------------------------------------------------------------------------+");
                println("|   Id | Name                                                                       |");                
                println("+------+----------------------------------------------------------------------------+");
                for (Iterator<Organization> iterator = organizations.iterator(); iterator.hasNext(); ) {
                    Organization org = iterator.next();
                    printf("| %4d | %-74s |\n", org.getId(), org.getName());               
                }      
                println("+------+----------------------------------------------------------------------------+");
            }
            else {
                println("No organization with name containing '" + orgSearchName + "' was found.");
            }

        } catch (DbException e) {
            println("Error: could not search for organizations");
            debug("DbException at listOrganizations");
            e.printStackTrace();
        }
    }
    
    private void initNewOrg(Organization org) {

        // Setting default values
        org.setAutoUpdateEmail(true);
        org.setAutoUpdateMatchFields(true);
        org.setEnabled(true);  
        org.setCountryCode(DEFAULT_CC);
        org.setLanguageCode(DEFAULT_LC);
        org.setModifiedBy(this.currentUser);
        
    }
    
    private void addOrganisation() {
        
       Organization org = new Organization();
       initNewOrg(org);
       editOrganization(org); 
    
    }
    
    private void editOrganization(Organization org) {
            
        boolean isNewOrg = true;   
        
        
        try {
            
            if (org == null) {
                org = fetchOrganization();
                isNewOrg = false;
            }            

            if (org != null) {
                if (isNewOrg) {
                    debug("This org is new");
                    editOrgProperties(org, isNewOrg);                                             
                    saveNewOrganization(org);
                    // Edit / add the rest                        
                    editContactAndSearchInfo(org);   
                    saveOrganization(org);                               
                    println("\nNew organization added:\n");
                }
                else {
                    debug("This org is not new");
                    editContactAndSearchInfo(org);

                    if (confirm ("Edit organization properties?")) {                    
                        editOrgProperties(org, isNewOrg);                  
                    }                
                    saveOrganization(org);
                    println("\nDone with edit Organization:\n");
                }
                // Show the current organization values after edit
                showOrganization(org);
            }                         
        } catch (NumberFormatException e) {
            println("Error: bad number format");            
            debug(e);
        } catch (DbException e) {
            println("Error: Database problem in editOrganization, " + e.getMessage());            
            debug(e);
        } catch (MegatronException e) {
        	println("Error: editOrganization, " + e.getMessage());
        	debug(e);
        }
        
    }

    private Organization fetchOrganization() {
        
        Organization org = null;     

        String orgId = this.readInput("Organization to edit (enter id or blank to quit): ");

        if (orgId.trim().equals("")) {
            println("No organization id entered, exiting edit.");            
        }
        else {
            try {                                        
                org = dbManager.getOrganization(Integer.valueOf(orgId));
                if (org == null || org.getName() == null) {
                    println("Error: could not find Organisation with id = "+ orgId);                    
                }
            }
            catch (DbException e) {
                println("Error: could not find Organisation with id = "+ orgId);
                e.printStackTrace();                
            }
            catch (NumberFormatException e) {
                println("Not a valid org id: "+ orgId);                                                        
            }
            catch (org.hibernate.ObjectNotFoundException onfe) {
                println("Error: could not find Organisation with id = "+ orgId + "\n");
                org = fetchOrganization();
            }
        }                   
        return org;
    }

    private void editOrgProperties(Organization org, boolean isNewOrg) 
    throws DbException {
        
        // Name
        editName(org);       
        
        // Registration number
        editRegNumber(org);
        
        if (isNewOrg == false) {
            // Country code        
            editCountryCode(org);

            // Language code                   
            editLanguageCode(org);

            // Description
            editDescription(org);

            // Enable / Disable            
            editEnabled(org);                                                    
        }
        
        // Comment
        editComment(org);
                    
        // Priority                  
        editPriority(org);                                        
    }

    
    
    private void editContactAndSearchInfo(Organization org) throws DbException, MegatronException {
        
        // Email addresses        
        editEmailAddresses(org);                                        
                    
        // Domain names
        editDomainNames(org);
        
        // AS numbers       
        editASNumbers(org);   

        // IP-ranges
        editIpRanges(org);
        
        // Comment
        addComment(org);
    }

    private void editOrganizatonEmailAddress() {
    
        Organization org = null;

        org = fetchOrganization();
        if (org != null) {
            try {
                editEmailAddresses(org);
            } 
            catch (DbException e) {
                println("Error: Database problem");
                debug("Database excption in editOrganizatonEmailAddress");
                debug(e);
            }            
            catch (MegatronException e) {
            	println("Error: " + e.getMessage());
            	debug("Megatron excption in editOrganizatonEmailAddress");
                debug(e);
            }
            	
            addComment(org);                    
            if (this.orgChanged) {
                saveOrganization(org);
            }
        }
    }
                
    
    private void editName(Organization org) {
        if (org.getName() == null || confirm("Current name is: " + org.getName() + ". Edit?")) {
            org.setName(readInput("Enter new organization name: "));
            this.orgChanged = true;
        }
    }

    private void editComment(Organization org) {
        if (confirm("Current comment is: " + org.getComment() + " Edit?")) {
            String userNameTimestamp = getUserNameWithTimestamp();
            org.setComment(userNameTimestamp + readMultiLineInput("Enter new comment (end with single .): "));   
            this.orgChanged = true;
        }
    }

    private void addComment(Organization org) {           
        
        String userAndDate = getUserNameWithTimestamp();
        String newComment = readInput("Add new comment (empty string will skip this):");
        if (newComment.trim().equals("") == false) {
            if (org.getComment() != null && org.getComment().equals("null") != true){
                org.setComment(org.getComment() + "\n" + userAndDate + newComment);
            }
            else {
                org.setComment(userAndDate + newComment);
            }
            this.orgChanged = true;
        }                           
    }
    
    private String getUserNameWithTimestamp(){
        
        return "[" + this.currentUser + " " 
        + DateUtil.formatDateTime(DateUtil.DATE_TIME_FORMAT_WITH_T_CHAR, new Date()) + "] ";        
    }
    
    private void editDescription(Organization org) {
        if (confirm("Current description is: " + org.getDescription() + ". Edit?")) {
            org.setDescription(readInput("Enter new description: "));    
            this.orgChanged = true;
        }
    }

    private void editLanguageCode(Organization org) {
        if (org.getLanguageCode() == null || confirm("Current language code is: " + org.getLanguageCode() + ". Edit?")) {
            org.setLanguageCode(readInput("Enter new language code: "));   
            this.orgChanged = true;
        }
    }
    private void editCountryCode(Organization org) {
        if (org.getCountryCode() == null || confirm("Current country code is: " + org.getCountryCode() + ". Edit?")) {
            org.setCountryCode(readInput("Enter new country code: ")); 
            this.orgChanged = true;
        }
    }

    private void editRegNumber(Organization org) {
        if (confirm("Current registration number is: " + org.getRegistrationNo() + ". Edit?")) {
            org.setRegistrationNo(readInput("Enter new registration number: ")); 
            this.orgChanged = true;
        }
    }

    private void editASNumbers(Organization org) throws DbException {
        
        boolean error = false;
        boolean change = true;
        String asn = getASNumbersInTextFormat(org);
        asn = asn.length() == 0 ? "<empty>" : asn; 
        change =  confirm("Current AS-numbers: " + asn + ". Edit?");
        
        if (change) {

            String newAsNumbers = this.readInput("Enter new list (comma separated): ");            
            String[] numbers = newAsNumbers.split(",");

            Set<ASNumber> newASNumbers = new java.util.TreeSet<ASNumber>();
            Set<ASNumber> toKeepASNumbers = new java.util.TreeSet<ASNumber>();

            for (String number : numbers) {                  
                if (number.trim().length() > 0) {
                    // AS-number not empty                  
                    ASNumber tmpASN = (ASNumber)dbManager.genericLoadObject("ASNumber", "Number", Long.valueOf(number.trim()));
                    if (tmpASN == null) {
                        tmpASN = new ASNumber();
                        tmpASN.setNumber(Long.valueOf(number.trim()));
                        tmpASN.setOrganizationId(org.getId());
                        newASNumbers.add(tmpASN);
                    }
                    else {
                        // Check if object already belongs to another org.
                        if (tmpASN.getOrganizationId().intValue() != org.getId().intValue()) {
                            println("ERROR: Conflicting AS-number " + number + " with org id " + tmpASN.getOrganizationId());
                            error = true;
                        }
                        else {
                            toKeepASNumbers.add(tmpASN);
                        }
                    }
                }
            }
            if (error == false) {                                                    
                if (org.getASNumbers() == null) {
                    // add new list
                    org.setASNumbers(toKeepASNumbers);
                }
                else {
                    deleteMissingObjects(org.getASNumbers(), toKeepASNumbers);
                    // Just keep the objects in the keep Set
                    org.setASNumbers(toKeepASNumbers);
                }
                // Add the new objects
                org.getASNumbers().addAll(newASNumbers);                                                       
                org.setAutoUpdateMatchFields(false);
                dbManager.updateOrganization(org, this.currentUser);
                this.orgChanged = false;
            }
        }
    }

    private String getASNumbersInTextFormat(Organization org) {
        
        String asn = "";
        if (org.getASNumbers() != null){
            for (ASNumber asNumber : org.getASNumbers()) {                
                asn = asn + asNumber.getNumber().toString() + ",";
            }
            if (asn.endsWith(",")) {
                asn = asn.substring(0, asn.length() - 1);
            }
        }
        return asn;
    }
    
    private void editDomainNames(Organization org) throws DbException {
        
        boolean error = false;
        boolean change = true;
        String names = getDomainNamesInTextFormat(org);    
        names = names.equals("") ? "<empty>" : names;
        change = confirm ("Current domain names: " + names + ". Edit?");          

        if (change) {
            String namesString = this.readInput("Enter new list (comma separated): ");            

            String[] dNames = namesString.split(",");
            
            Set<DomainName> newDomainNames = new java.util.TreeSet<DomainName>();
            Set<DomainName> toKeepDomainNames = new java.util.TreeSet<DomainName>();
            for (String name : dNames) {                   
                if (name.length() > 0){
                    DomainName tmpDomainName = (DomainName)dbManager.genericLoadObject("DomainName", "Name", name);
                    if (tmpDomainName == null) {                    
                        tmpDomainName = new DomainName();
                        tmpDomainName.setName(name);
                        tmpDomainName.setOrganizationId(org.getId());
                        newDomainNames.add(tmpDomainName);
                    }
                    else {
                        // Check if object already belongs to another org.
                        debug("Owning org id: " + tmpDomainName.getOrganizationId() + ", editing org id: " + org.getId());                        
                        if (tmpDomainName.getOrganizationId().intValue() != org.getId().intValue()) {                        
                            println("ERROR: Conflicting domain name " + name + " with org id " + tmpDomainName.getOrganizationId());
                            error = true;
                        }
                        else {                        
                            toKeepDomainNames.add(tmpDomainName);
                        }
                    }
                }
            }
            if (error == false) {              
                if (org.getDomainNames() == null && toKeepDomainNames.size() > 0) {
                    org.setDomainNames(toKeepDomainNames);
                }
                else  {                    
                    deleteMissingObjects(org.getDomainNames(), toKeepDomainNames);
                    // Just keep the objects in the keep Set
                     org.setDomainNames(toKeepDomainNames);
                }
                
                // Add the new objects
                if (newDomainNames.size() > 0) {
                    debug("Addning new domain names.");
                    org.getDomainNames().addAll(newDomainNames);
                }
                
                org.setAutoUpdateMatchFields(false);
                dbManager.updateOrganization(org, this.currentUser);
                this.orgChanged = false;
            }
            else {
                editDomainNames(org);
            }
        }
    }

    private String getDomainNamesInTextFormat(Organization org) {
        String names = "";
        if (org.getDomainNames() != null) {
            for (DomainName domainName : org.getDomainNames()) {
                names = names + domainName.getName() + ",";
            }
            if (names.endsWith(",")) {
                names = names.substring(0, names.length() - 1);
            }
        }
        return names;
    }
    

    private void editIpRanges(Organization org) throws DbException {
        String ranges = "";
        boolean error = false;
        boolean change = true;
        ranges = getIpRangesInTextFormat(org);
        ranges = ranges.equals("") ? "<empty>" : ranges;
        change = confirm("Current IP-ranges: " + ranges + ". Edit?");

        if (change) {
            ranges = this.readInput("Enter new list (comma separated): ");            

            String[] enteredRanges = ranges.split(",");

            Set<IpRange> newIpRanges = new java.util.TreeSet<IpRange>();
            Set<IpRange> toKeepIpRanges = new java.util.TreeSet<IpRange>();
            
            String[] attrNames = {"StartAddress", "EndAddress"};                   
            for (String range : enteredRanges) {                 
                Long startAddress;
                Long endAddress;
                if (range.trim().equals("") == false) {
                    try {
                        startAddress = IpAddressUtil.convertIpAddress(range.split("-")[0]);
                        endAddress = IpAddressUtil.convertIpAddress(range.split("-")[1]);
                        Object[] addressValues = {startAddress, endAddress};


                        IpRange tmpRange = (IpRange)dbManager.genericLoadObject("IpRange", attrNames, addressValues);
                        if (tmpRange == null) {       
                            tmpRange = new IpRange();
                            tmpRange.setOrganizationId(org.getId());
                            tmpRange.setStartAddress(startAddress);
                            tmpRange.setEndAddress(endAddress);

                            if (dbManager.isRangeOverlapping(startAddress, endAddress) == false) {                                
                                newIpRanges.add(tmpRange);
                            }
                            else {
                                println("ERROR: IP-range " + range + " overlaps with an existing range.");
                                error = true;
                            }                        
                        }
                        else {
                            // Check if object already belongs to another org.
                            if (tmpRange.getOrganizationId().intValue() != org.getId().intValue()) {
                                println("ERROR: IP-range " + range + " belongs to another org, id = " + tmpRange.getId());
                                error = true;
                            }
                            else {                                         
                                toKeepIpRanges.add(tmpRange);
                            }
                        }
                    } catch (UnknownHostException e) {
                        println("Error: Invalid IP-address in range " + range + ".");
                        e.printStackTrace();
                    }   
                }
            }
            if (error == false) {                  
                if (org.getIpRanges() == null) {
                    org.setIpRanges(toKeepIpRanges);                    
                }
                else {
                    deleteMissingObjects(org.getIpRanges(), toKeepIpRanges);               
                    // Just keep the objects in the keep Set
                    org.setIpRanges(toKeepIpRanges);
                }
                
                // Add the new objects
                org.getIpRanges().addAll(newIpRanges);            
               
                org.setAutoUpdateMatchFields(false);
                dbManager.updateOrganization(org, this.currentUser);
                this.orgChanged = false;
            }
        }
    }

    private String getIpRangesInTextFormat(Organization org) {
        
        String ranges = "";
        
        if (org.getIpRanges() != null) {
            for (IpRange range : org.getIpRanges()) {            
                ranges = ranges + IpAddressUtil.convertIpAddress(range.getStartAddress(), false) + "-" + IpAddressUtil.convertIpAddress(range.getEndAddress(), false) + ",";
            }
            if (ranges.endsWith(",")) {
                ranges = ranges.substring(0, ranges.length() - 1);
            }
        }
        return ranges;
    }
    
    
    private void editEmailAddresses(Organization org) 
    throws DbException, MegatronException {
        
        if (org.getEmailAddresses() == null || confirm("Current email contacts = " + org.getEmailAddresses() + ". Edit?")) {
            //if (org.getEmailAddresses() == null)  println("\n");
        	String emailAddresses = this.readInput("Enter new email contacts (comma separated): ");
        	
        	if (AppUtil.isEmailAddressesValid(emailAddresses) || (emailAddresses.trim().length() == 0)){
        		org.setEmailAddresses(emailAddresses);
        		org.setAutoUpdateEmail(false);
                dbManager.updateOrganization(org, this.currentUser);
                this.orgChanged = false;
        	}
        	else { 
        		throw new MegatronException("Invalid email address in :" + emailAddresses);
        	}                        
        }
    }    

    
    private void editEnabled(Organization org) 
    throws DbException {
                
        if (confirm("The Organization is currently : " + (org.isEnabled() ? "ENABLED": "DISABLED") + ". Edit?")) {
            if (confirm("Do you want to " + (org.isEnabled() ? "DISABLE ?": "ENABLE ?"))) {
                org.setEnabled(!org.isEnabled());
                dbManager.updateOrganization(org, this.currentUser);
                this.orgChanged = false;
            }
        }
    }
    
    private void editPriority(Organization org) {
        
        boolean change = true;
        
        try {            
            if (org.getPriority() != null) {
                change = confirm("Current priority = " +  org.getPriority().getId() + " - " + org.getPriority().getName() + " Edit?"); 
            }
            if (change) {
                List<Priority> prios = dbManager.getAllPriorities();
                Priority newPrio = null;
                while (newPrio == null) {                    
                    println("\nExisting priorities:");                    
                    println("+------+----------------------------------------------------------------------------+");
                    println("| Prio | Name                                                                       |");                
                    println("+------+----------------------------------------------------------------------------+");
                    for (Priority prio: prios) {
                        System.out.printf("| %4d | %-74s |\n", prio.getPrio(), prio.getName());
                        
                    }
                    println("+------+----------------------------------------------------------------------------+");

                    String prioNo = readInput("\nEnter prio-number of one of the above priorities: ");
                    newPrio = (Priority)dbManager.genericLoadObject("Priority", "Prio", prioNo);

                    if (newPrio != null) {
                        org.setPriority(newPrio);
                        this.orgChanged = true;
                    }
                    else {
                        println("Sorry, no priority with # " + prioNo + " was found.");
                    }
                }                
            }

        } catch (DbException e) {
            println("Exception in editPriority");
            e.printStackTrace();
        }
        
    }
    
    private boolean saveOrganization(Organization org) {
        
        boolean saved = false;
        
        if (this.orgChanged) {
        
            try {
                dbManager.updateOrganization(org, this.currentUser);
            } catch (DbException e) {
                println("DbException when updating organization");
                e.printStackTrace();                
            }
            saved = true;    
            this.orgChanged = false;
            
        }    
            
        return saved;
        
    }
       
    private boolean saveNewOrganization(Organization org) {
        
        boolean saved = false;
        
            try {
                dbManager.addOrganization(org);
            } catch (DbException e) {
                println("DbException when saving organization");
                e.printStackTrace();                
            }           
            saved = true;  
            this.orgChanged = false;
        return saved;
        
    }
        
    private void showOrganization(Organization org)    
    {
        
        if (org == null) {
            String orgId = this.readInput("Organization to edit (enter id): ");
            
            try {                                        
                org = dbManager.getOrganization(Integer.valueOf(orgId));
            }
            catch (DbException e) {
                println("Error: could not find Organisation with id = "+ orgId);
                debug(e);
                return;
            }
            catch (NumberFormatException e) {
                println("Not a valid org id: "+ orgId);
                return;
            } 
            catch (Exception e) {
                println("Unknown Error");
                debug(e); 
                return;
            }            
        }        
        printf("%-16s: %d\n", "Id", org.getId());
        printf("%-16s: %s\n", "Name", org.getName());
        printf("%-16s: %s\n", "Org/Reg no", org.getRegistrationNo());
        printf("%-16s: %s\n", "Status", (org.isEnabled() ? "ENABLED" : "DISABLED"));        
        printf("%-16s: %s\n", "Priority", org.getPriority().getName());
        printf("%-16s: %s\n", "Country code", org.getCountryCode());
        printf("%-16s: %s\n", "Language code", org.getLanguageCode());
        printf("%-16s: %s\n", "Email addresses", org.getEmailAddresses());
        printf("%-16s: %s\n", "Description", org.getDescription());
        printf("%-16s: %s\n", "Comment", org.getComment());
        printf("%-16s: %s\n", "Created", toStringDate(org.getCreated()));
        printf("%-16s: %s\n", "Last modified", toStringDate(org.getLastModified()));
        printf("%-16s: %s\n", "AS-numbers", getASNumbersInTextFormat(org));
        printf("%-16s: %s\n", "Domain names", getDomainNamesInTextFormat(org));
        printf("%-16s: %s\n", "Ip-ranges", getIpRangesInTextFormat(org));
    }    
    
    private void quit() {
        
        println("Catch you later."); 
        System.exit(0);    
    }
            
    private void deleteMissingObjects(Set<?> list, Set<?> toKeep) 
    throws DbException {
        
        // Checks if objects in list exists in toKeep, if not they are deleted from DB.        
        if (list != null && toKeep != null) {
            for (Object obj : list) {     
                boolean found = false;
                for (Object keepObj : toKeep) {
                    if (keepObj.equals(obj)) {
                        found = true;
                        break;                        
                    }
                }
                if (found == false) {                    
                    debug("Deleting the object from db " + obj.toString());
                    this.dbManager.genericDelete(obj);
                }
            }
        }
        return;
    }
           
    @SuppressWarnings("unused")
    private void handleException(Exception e, String msg) 
    throws MegatronException {
        
        throw new MegatronException (msg);
    }

    
    private String toStringDate(java.lang.Long timeStamp) {
    
        return DateUtil.formatDateTime(DateUtil.DATE_TIME_FORMAT_WITH_T_CHAR, SqlUtil.convertTimestamp(timeStamp));
    }
}
