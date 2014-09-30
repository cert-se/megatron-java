package se.sitic.megatron.ui;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.mail.Message;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.db.DbException;
import se.sitic.megatron.db.DbManager;
import se.sitic.megatron.entity.ASNumber;
import se.sitic.megatron.entity.Contact;
import se.sitic.megatron.entity.DomainName;
import se.sitic.megatron.entity.IpRange;
import se.sitic.megatron.entity.LogEntry;
import se.sitic.megatron.entity.Organization;
import se.sitic.megatron.entity.Priority;
import se.sitic.megatron.util.DateUtil;
import se.sitic.megatron.util.IpAddressUtil;
import se.sitic.megatron.util.SqlUtil;

public class OrganizationHandler {

    private DbManager dbManager = null;
    private static boolean debug = false;
    private Logger log = null;

    // Commands: Add org, Edit org , Find org , View org , Quit, Help, Debug,
    // Edit contact, Extract contacts, Search log entries
    private enum Commands {
        A, E, F, V, Q, H, D, C, X, L
    }

    // Search types: Name, ASN, E-mail, Domain, IP, Range, Cancel, OrgId
    private enum SearchTypes {
        N, A, E, D, I, R, C, O, V
    }

    // Organization edit types: ASN, Domain, Range, Contact, Properities"
    private enum EditTypes {
        A, D, R, C, P
    }

    private PrintWriter screenWriter = null;
    private boolean newContact = false;
    private boolean newOrg = false;
    private BufferedReader in = null;
    private TypedProperties props = null;
    private String currentUser = null;
    private boolean orgChanged = false;
    private boolean contactChanged = false;

    // Values from global properties
    private String exportTimstampFormat = null;
    private String[] validRoles = null;
    private String defaultCountryCode = "";
    private String defaultLanguageCode = "";
    private String outputFilePath = "";

    static private final boolean MANDATORY = true;
    static private final boolean NOT_MANDATORY = false;
    static private final String DEFAULT_CC = "SE";
    static private final String DEFAULT_LC = "sv";
    static private final String[] EMAIL_TYPES = {
            Message.RecipientType.TO.toString(),
            Message.RecipientType.CC.toString(),
            Message.RecipientType.BCC.toString() };
    static private final String[] DEFAULT_ROLES = { "Abuse", "Technical",
            "Administrative", "Manager" };
    static private final String TEXT_EMPTY = "<EMPTY>";
    static private final String TEXT_NULL = "<NULL>";
    static private final String DEFAULT_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";
    static private final String DEFAULT_OUTPUT_FILE_PATH = "/tmp";
    static private final String CONTACT_EXPORT_FILE_NAME = "contact_export.txt";
    static private final String LOG_ENTRY_EXPORT_FILE_NAME = "log_entry_export.txt";
    

    public OrganizationHandler(TypedProperties props) {

        in = new BufferedReader(new InputStreamReader(System.in));
        this.props = props;

        this.defaultCountryCode = this.props.getString(AppProperties.UI_DEFAULT_CC_KEY,
                DEFAULT_CC);
        this.defaultLanguageCode = this.props.getString(AppProperties.UI_DEFAULT_LC_KEY,
                DEFAULT_LC);
        this.outputFilePath = this.props.getString(AppProperties.UI_OUTPUT_FILE_PATH_KEY,
                DEFAULT_OUTPUT_FILE_PATH);
        this.validRoles = this.props.getStringListFromCommaSeparatedValue(
                AppProperties.UI_VALID_ROLES_KEY, DEFAULT_ROLES, true);
        this.exportTimstampFormat = this.props.getString(AppProperties.UI_TIMESTAMP_FORMAT_KEY,                
                DEFAULT_TIMESTAMP_FORMAT);
        this.log = Logger.getLogger(this.getClass());
        this.screenWriter = new PrintWriter(System.out, true);

        try {
            dbManager = DbManager.createDbManager(this.props);
        } catch (DbException e) {
            handleException(
                    "Database problem at createDbManager - could not establish a DB session",
                    e);
        }

        while (this.currentUser == null) {
            String user = System.getenv("SUDO_USER");
            if (user == null) {
                user = System.getenv("USER");
            }
            if (user == null) {
                user = readInput("\nEnter user id: ");
            }
            if (user != null && user.trim() != "") {
                currentUser = user;
            }
        }
        welcome();
        help();
    }

    public void startUI() {

        while (true) {
            this.processCommand(this.readCommand());
        }
    }

    private PrintWriter getScreenWriter() {
        return this.screenWriter;
    }

    private void println(String msg) {
        getScreenWriter().println(msg);
    }

    private void print(String msg) {
        getScreenWriter().print(msg);
        getScreenWriter().flush();
    }

    private void printf(String msg, Object... args) {
        getScreenWriter().printf(msg, args);
    }

    private String[] readCommand() {

        String[] command = new String[2];
        String tmpCommand = readInput("\n> ");

        if (tmpCommand != null && tmpCommand.length() == 1) {
            command[0] = (String) tmpCommand.subSequence(0, 1);
            command[1] = null;
        } else if (tmpCommand != null && tmpCommand.length() > 2) {
            command[0] = (String) tmpCommand.subSequence(0, 1);
            command[1] = tmpCommand.substring(2);
        } else {
            return readCommand();
        }
        return command;
    }

    private String readInput(String promt) {

        String input = null;

        try {
            print(promt);
            input = in.readLine();
        } catch (java.io.IOException ioe) {
            ioe.printStackTrace();
        }
        return input;
    }

    @SuppressWarnings("unused")
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

    private void welcome() {

        println("\nWelcome " + currentUser + " to:\n");
        println(" ███    ███ ███████  ██████   █████  ████████ ██████   ██████  ███    ██");
        println(" ████  ████ ██      ██       ██   ██    ██    ██   ██ ██    ██ ████   ██");
        println(" ██ ████ ██ █████   ██   ███ ███████    ██    ██████  ██    ██ ██ ██  ██");
        println(" ██  ██  ██ ██      ██    ██ ██   ██    ██    ██   ██ ██    ██ ██  ██ ██");
        println(" ██      ██ ███████  ██████  ██   ██    ██    ██   ██  ██████  ██   ████");
        println("");
        println(" ███████████████████████████████████████████████████████████████████████");
        println(" ██      ╔═╗╦═╗╔═╗╔═╗╔╗╔╦╔═╗╔═╗╔╦╗╦╔═╗╔╗╔  ╔╦╗╔═╗╔╗╔╔═╗╔═╗╔═╗╦═╗      ██");
        println(" ██      ║ ║╠╦╝║ ╦╠═╣║║║║╔═╝╠═╣ ║ ║║ ║║║║  ║║║╠═╣║║║╠═╣║ ╦║╣ ╠╦╝      ██");
        println(" ██      ╚═╝╩╚═╚═╝╩ ╩╝╚╝╩╚═╝╩ ╩ ╩ ╩╚═╝╝╚╝  ╩ ╩╩ ╩╝╚╝╩ ╩╚═╝╚═╝╩╚═      ██");
        print(" ███████████████████████████████████████████████████████████████████████");
    }

    private void printExportContactsBanner() {

        println("\n\n");
        println(" ┌─┐ ┐ ┬┌─┐┌─┐┬─┐┌┬┐  ┌─┐┌─┐┌┐┌┌┬┐┌─┐┌─┐┌┬┐┌─┐");
        println(" ├┤ ┌┴┬┘├─┘│ │├┬┘ │   │  │ ││││ │ ├─┤│   │ └─┐");
        println(" └─┘┴ └ ┴  └─┘┴└─ ┴   └─┘└─┘┘└┘ ┴ ┴ ┴└─┘ ┴ └─┘");
    }

    private void printMainMenuBanner() {

        println("\n\n");
        println(" ┌┬┐┌─┐┬┌┐┌  ┌┬┐┌─┐┌┐┌┬ ┬");
        println(" │││├─┤││││  │││├┤ ││││ │");
        println(" ┴ ┴┴ ┴┴┘└┘  ┴ ┴└─┘┘└┘└─┘");
    }

    private void printContactManBanner() {

        println("\n\n");
        println(" ┌─┐┌─┐┌┐┌┌┬┐┌─┐┌─┐┌┬┐  ┌┬┐┌─┐┌┐┌┌─┐┌─┐┌─┐┬─┐");
        println(" │  │ ││││ │ ├─┤│   │   │││├─┤│││├─┤│ ┬├┤ ├┬┘");
        println(" └─┘└─┘┘└┘ ┴ ┴ ┴└─┘ ┴   ┴ ┴┴ ┴┘└┘┴ ┴└─┘└─┘┴└─\n");
    }

    private void printFindOrgBanner() {

        println("\n\n");
        println(" ┌─┐┬┌┐┌┌┬┐  ┌─┐┬─┐┌─┐┌─┐┌┐┌┬┌─┐┌─┐┌┬┐┬┌─┐┌┐┌");
        println(" ├┤ ││││ ││  │ │├┬┘│ ┬├─┤││││┌─┘├─┤ │ ││ ││││");
        println(" └  ┴┘└┘─┴┘  └─┘┴└─└─┘┴ ┴┘└┘┴└─┘┴ ┴ ┴ ┴└─┘┘└┘");
    }

    private void printAddOrgBanner() {

        println("\n\n");
        println(" ┌─┐┌┬┐┌┬┐  ┌─┐┬─┐┌─┐┌─┐┌┐┌┬┌─┐┌─┐┌┬┐┬┌─┐┌┐┌");
        println(" ├─┤ ││ ││  │ │├┬┘│ ┬├─┤││││┌─┘├─┤ │ ││ ││││");
        println(" ┴ ┴─┴┘─┴┘  └─┘┴└─└─┘┴ ┴┘└┘┴└─┘┴ ┴ ┴ ┴└─┘┘└┘");
    }

    private void printEditOrgBanner() {

        println("\n\n");
        println(" ┌─┐┌┬┐┬┌┬┐  ┌─┐┬─┐┌─┐┌─┐┌┐┌┬┌─┐┌─┐┌┬┐┬┌─┐┌┐┌");
        println(" ├┤  │││ │   │ │├┬┘│ ┬├─┤││││┌─┘├─┤ │ ││ ││││");
        println(" └─┘─┴┘┴ ┴   └─┘┴└─└─┘┴ ┴┘└┘┴└─┘┴ ┴ ┴ ┴└─┘┘└┘");
    }

    private void printSearchLogEntriesBanner() {

        println("\n\n");
        println(" ┌─┐┌─┐┌─┐┬─┐┌─┐┬ ┬  ┬  ┌─┐┌─┐  ┌─┐┌┐┌┌┬┐┬─┐┬┌─┐┌─┐");
        println(" └─┐├┤ ├─┤├┬┘│  ├─┤  │  │ ││ ┬  ├┤ │││ │ ├┬┘│├┤ └─┐");
        println(" └─┘└─┘┴ ┴┴└─└─┘┴ ┴  ┴─┘└─┘└─┘  └─┘┘└┘ ┴ ┴└─┴└─┘└─┘");
    }

    private void printContactsBanner() {
        println(" ┌─┐┌─┐┌┐┌┌┬┐┌─┐┌─┐┌┬┐┌─┐");
        println(" │  │ ││││ │ ├─┤│   │ └─┐");
        println(" └─┘└─┘┘└┘ ┴ ┴ ┴└─┘ ┴ └─┘");
        println(" ────────────────────────");
    }

    private void printOrganizationBanner() {
        println(" ┌─┐┬─┐┌─┐┌─┐┌┐┌┬┌─┐┌─┐┌┬┐┬┌─┐┌┐┌");
        println(" │ │├┬┘│ ┬├─┤││││┌─┘├─┤ │ ││ ││││");
        println(" └─┘┴└─└─┘┴ ┴┘└┘┴└─┘┴ ┴ ┴ ┴└─┘┘└┘");
        println(" ────────────────────────────────");
    }

    private void usage() {
        printMainMenuBanner();
        println("\n Valid commands are: \n\n\tF - Find organization \n\tA - Add organization \n\tE - Edit organization \n\tV - View organization \n\tC - Edit organization contacts \n\tL - Search Log entries \n\tX - Export contacts \n\n\tD - Debug \n\tH - Help\n\tQ - Quit \n\n");
    }

    private void help() {

        usage();

        println("Find organization, search by name, mail address, ASN, domain name or IP-range.");
        println("Add organization, add a new organization to the system.");
        println("Edit organization, takes org-id as input.");
        println("View organization, takes org-id as input.");
        println("Contacts, edit organization contact information, takes org-id as input.");
        println("Search for log entries.");
        println("Export selected organization contacts.");
        println("");
        println("Debug mode, toggle on/off.");
        println("Help, shows this help message.");
        println("Quit to exit.\n");
    }

    private void printErrorMessage(String message) {
        println("\nError: " + message + ".\n");
    }

    private void printInfoMessage(String message) {
        println(message + ".\n");
    }

    private void toggleDebug() {

        debug = !debug;
        println("Debug is now " + (debug ? "on." : "off."));
    }

    private void debug(String msg) {

        if (debug) {
            println("DEBUG: " + msg);
        }
    }

    private void debug(Exception e) {

        println(e.getMessage());
        e.printStackTrace();
    }

    private void handleException(String message, Exception e) {

        printErrorMessage(message + ": " + e.getMessage());
        log.error(message, e);

        if (debug) {
            debug(e);
        }
    }

    private void processCommand(String[] command) {

        try {
            Organization org = null;
            if (command[1] != null) {
                org = fetchOrganization(command[1]);
            }

            switch (Commands.valueOf(command[0].toUpperCase())) {

            case A:
                addOrganisation();
                return;
            case E:
                editOrganization(org);
                return;
            case V:
                viewOrganization(org, true);
                return;
            case Q:
                quit();
                return;
            case H:
                help();
                return;
            case C:
                editOrganizationContacts(org);
                return;
            case F:
                findOrganization();
                return;
            case L:
                searchLogEntries();
                return;
            case X:
                exportContacts();
                return;
            case D:
                toggleDebug();
                return;
            }
        } catch (java.lang.IllegalArgumentException e) {
            handleException("unknown command: " + command[0], e);
            help();
        }
    }

    private boolean confirm(String message) {

        String input = readInput("\n" + message + " y/N: ").toUpperCase();
        boolean result = false;

        if (input.equals("Y") || input.equals("N") || input.trim().equals("")) {
            result = input.equals("Y");
        } else {
            printErrorMessage("bad input, use 'Y' or 'N'");
        }

        return result;
    }

    private void searchLogEntries() {

        printSearchLogEntriesBanner();
        println("\n Select search method:  \n\n\tO - Organization ID \n\tA - by AS number \n\tR - by IP range \n\tI - by IP address\n\tV - View log entry\n\n\tC - Cancel \n");

        String input = readInput("\nEnter search method: ");

        switch (SearchTypes.valueOf(input.toUpperCase())) {
        case O:
            searchLogEntriesByOrgId();
            break;
        case A:
            searchLogEntriesByASNumber();
            break;
        // case D: searchLogEntriesByDomainName(); break;
        case R:
            searchLogEntriesByIPRange();
            break;
        case I:
            searchLogEntriesByIPAddress();
            break;
        case V:
            viewLogEntry();
            break;
        case C:
            return;
        default:
            printErrorMessage("invalid search method");
            break;
        }

    }

    private void findOrganization() {

        printFindOrgBanner();
        println("\n Select search method:  \n\n\tN - by part of name \n\tA - by AS number \n\tD - by domain name \n\tE - by contact email address \n\tR - by IP range \n\tI - by IP address\n\n\tC - Cancel \n");

        String input = readInput("\nEnter search method: ");

        switch (SearchTypes.valueOf(input.toUpperCase())) {

        case N:
            listOrganizations();
            break;
        case A:
            findOrganizationByASNumber();
            break;
        case D:
            findOrganizationByDomainName();
            break;
        case E:
            findOrganizationByEmailAddress();
            break;
        case R:
            findOrganizationByIPRange(null);
            break;
        case I:
            findOrganizationByIPAddress();
            break;
        case C:
            return;
        default:
            printErrorMessage("invalid search method");
            break;
        }
    }

    private void editOrganization(Organization org) {

        if (org == null) {
            org = fetchOrganization(null);
        }
        try {

            if (org != null) {
                printEditOrgBanner();
                print("\n Select edit method: \n\n\tA - AS number \n\tD - Domain names \n\tR - IP ranges \n\tP - organization Properties\n\n\tC - Cancel \n");
                String input = readInput("\nEnter edit option: ");

                switch (EditTypes.valueOf(input.toUpperCase())) {
                case A:
                    editASNumbers(org);
                    break;
                case D:
                    editDomainNames(org);
                    break;
                case R:
                    editIpRanges(org);
                    break;
                case P:
                    editOrgProperties(org);
                    break;
                case C:
                    return;
                default:
                    printErrorMessage("invalid edit option");
                    break;
                }
            }
        } catch (DbException e) {
            handleException("Could not edit organization", e);
        }
        usage();
    }

    private void showOrgSearchResult(Organization org, boolean showContacts) {

        if (org != null) {
            viewOrganization(org, showContacts);
        } else {
            printInfoMessage("\nNo organization matching the given search critera was found");
        }
    }

    private void outputLogEntrySearchResult(List<LogEntry> entries) {

        PrintWriter printWriter = null;
        boolean outputIsFile = false;
        boolean outputAllFields = false;

        if (entries == null || entries.size() == 0) {
            printInfoMessage("\nNo log entries mathcing the given search criteria where found.");
        } else {
            String outputType = readInput("Enter output type [a] all fields, [p| partial only: ");
            String outputFormat = readInput("Enter output format [t] table or [c] comma separated: ");
            String outputLocation = readInput("Enter output location [s] screen, [f] file: ");
            try {
                if (outputLocation.toLowerCase().equals("f")) {
                    outputIsFile = true;
                    FileWriter fstream;
                    fstream = new FileWriter(this.outputFilePath + "/"
                            + LOG_ENTRY_EXPORT_FILE_NAME, false);
                    printWriter = new PrintWriter(fstream);
                } else {
                    printWriter = getScreenWriter();
                }
                if (outputType.toLowerCase().startsWith("a")) {
                    outputAllFields = true;
                }
                outputLogEntries(entries, outputFormat, outputAllFields,
                        printWriter);
            } catch (IOException e) {
                handleException("Error when opening log entry export file", e);
            }
            printInfoMessage("\nTotal number of log entries found: "
                    + entries.size());

            if (outputIsFile) {
                printInfoMessage("\nDone exporting log entries to "
                        + this.outputFilePath + "/"
                        + LOG_ENTRY_EXPORT_FILE_NAME);
                printWriter.close();
            }
        }
    }

    private void searchLogEntriesByOrgId() {

        String orgId = readInput("\nEnter organization id to search for: ");
        List<LogEntry> entries = null;

        try {
            entries = dbManager.searchLogEntriesByOrgId(Integer.valueOf(orgId));
        } catch (DbException e) {
            handleException(
                    "could not search for log entries by organization id", e);
        } catch (NumberFormatException e) {
            handleException("invalid organization id", e);
        }
        outputLogEntrySearchResult(entries);
    }

    private void viewLogEntry() {
        String id = readInput("Enter log entry id: ");

        try {
            LogEntry entry = (LogEntry) dbManager.genericLoadObject("LogEntry",
                    "Id", new Long(id));
            List<LogEntry> entries = new ArrayList<LogEntry>();
            entries.add(entry);
            outputLogEntries(entries, "T", true, getScreenWriter());
        } catch (DbException e) {
            handleException(e.getMessage(), e);
        }
    }

    private void searchLogEntriesByASNumber() {

        String asn = readInput("\nEnter ASN to search for: ");
        List<LogEntry> entries = null;

        try {
            entries = dbManager.searchLogEntriesByASNumber(Long.valueOf(asn));
        } catch (DbException e) {
            handleException("could not search for log entries by AS number", e);
        } catch (NumberFormatException e) {
            handleException("invalid ASNumber", e);
        }
        outputLogEntrySearchResult(entries);
    }

    private void searchLogEntriesByIPAddress() {

        String address = readInput("Enter IP address to search for: ");
        List<LogEntry> entries = null;

        try {
            long longAddress = IpAddressUtil.convertIpAddress(address);
            entries = dbManager.searchLogEntriesByIPrange(longAddress,
                    longAddress);
        } catch (DbException | UnknownHostException e) {
            handleException("could not search for log entries by IP address", e);
        }
        outputLogEntrySearchResult(entries);
    }

    private void searchLogEntriesByIPRange() {

        String range = readInput("\nEnter IP range to search for (use format: x.x.x.x-y.y.y.y): ");
        Long startAddress;
        Long endAddress;
        List<LogEntry> entries = null;

        try {
            startAddress = IpAddressUtil.convertIpAddress(range.split("-")[0]);
            endAddress = IpAddressUtil.convertIpAddress(range.split("-")[1]);
            entries = dbManager.searchLogEntriesByIPrange(startAddress,
                    endAddress);
        } catch (DbException e) {
            handleException("could not search for log entries by IP range", e);
        } catch (UnknownHostException e) {
            handleException("something wrong with the given IP range", e);
        }
        outputLogEntrySearchResult(entries);
    }

    private void findOrganizationByEmailAddress() {

        String address = readInput("Enter email address to search for: ");
        List<Organization> orgs = null;

        try {
            orgs = dbManager.searchOrganizationByEmailAddress(address);

            if (orgs.isEmpty() == false) {
                if (orgs.size() == 1) {
                    showOrgSearchResult(orgs.get(0), true);
                } else {
                    String orgIds = "";
                    for (Object tmpOrg : orgs.toArray()) {
                        orgIds = orgIds + ((Organization) tmpOrg).getId() + " ";
                    }
                    printInfoMessage("Multiple organizations found matching the given input : "
                            + orgIds.trim());
                }
            } else {
                // Generate no orgs found message
                showOrgSearchResult(null, false);
            }
        } catch (DbException e) {
            handleException(
                    "could not search for organizations by email address", e);
        }
    }

    private void findOrganizationByIPAddress() {

        String address = readInput("Enter IP address to search for: ");
        Organization org = null;

        try {
            long longAddress = IpAddressUtil.convertIpAddress(address);
            List<Organization> orgs = dbManager.searchOrganizationsByIPrange(
                    longAddress, longAddress);

            if (orgs.isEmpty() == false) {
                org = orgs.get(0);
            }
        } catch (DbException | UnknownHostException e) {
            handleException("could not search for organizations by IP address",
                    e);
        }
        showOrgSearchResult(org, false);
    }

    private void findOrganizationByASNumber() {

        String asn = readInput("\nEnter ASN to search for: ");
        Organization org = null;

        try {
            org = dbManager.searchOrganizationByASNumber(Integer.valueOf(asn));
        } catch (DbException e) {
            handleException("could not search for organizations by AS number",
                    e);
        } catch (NumberFormatException e) {
            handleException("invalid ASNumber", e);
        }
        showOrgSearchResult(org, false);
    }

    private void findOrganizationByDomainName() {

        String domainName = readInput("\nEnter domain name to search for: ");
        Organization org = null;

        try {
            org = dbManager.searchOrgForDomain(domainName);
        } catch (DbException e) {
            handleException(
                    "could not search for organizations by domain name", e);
        }
        showOrgSearchResult(org, false);
    }

    private void findOrganizationByIPRange(String range) {

        if (range == null) {
            range = readInput("\nEnter IP range to search for (use format: x.x.x.x-y.y.y.y): ");
        }

        Long startAddress;
        Long endAddress;
        if (range.trim().equals("") == false) {
            try {
                startAddress = IpAddressUtil
                        .convertIpAddress(range.split("-")[0]);
                endAddress = IpAddressUtil
                        .convertIpAddress(range.split("-")[1]);
                List<Organization> orgs = dbManager
                        .searchOrganizationsByIPrange(startAddress, endAddress);

                if (orgs.isEmpty() == false) {
                    if (orgs.size() == 1) {
                        showOrgSearchResult(orgs.get(0), false);
                    } else {
                        String orgIds = "";
                        for (Object tmpOrg : orgs.toArray()) {
                            orgIds = orgIds + ((Organization) tmpOrg).getId()
                                    + " ";
                        }
                        printInfoMessage("Multiple organizations found matching the given IP range : "
                                + orgIds.trim());
                    }
                } else {
                    // Generate no orgs found message
                    showOrgSearchResult(null, false);
                }
            } catch (DbException e) {
                handleException(
                        "could not search for organizations by IP range", e);
            } catch (UnknownHostException e) {
                handleException(
                        "could not search for organizations by IP range", e);
            }
        }
    }

    private void listOrganizations() {

        String orgSearchName = this
                .readInput("Organization name to seach for (<enter>=all) : ");

        try {
            List<Organization> organizations = dbManager.searchOrganizations(
                    orgSearchName, 0, 1000);

            if (organizations.size() > 0) {
                println("\nOrganizations:");
                println("+------+----------------------------------------------------------------------------+");
                println("|   Id | Name                                                                       |");
                println("+------+----------------------------------------------------------------------------+");
                for (Iterator<Organization> iterator = organizations.iterator(); iterator
                        .hasNext();) {
                    Organization org = iterator.next();
                    printf("| %4d | %-74s |\n", org.getId(), org.getName());
                }
                println("+------+----------------------------------------------------------------------------+");
            } else {
                printInfoMessage("No organization with name containing '"
                        + orgSearchName + "' was found");
            }
        } catch (DbException e) {
            handleException("could not search for organizations", e);
        }
    }

    private void initNewOrg(Organization org) {

        // Setting default values
        org.setAutoUpdateMatchFields(true);
        org.setEnabled(true);
        org.setCountryCode(this.defaultCountryCode);
        org.setLanguageCode(this.defaultLanguageCode);
        org.setComment(getUserNameWithTimestamp() + "Created.");
        this.newOrg = true;
    }

    private void addOrganisation() {

        printAddOrgBanner();

        Organization org = new Organization();
        initNewOrg(org);
        editOrgProperties(org);
    }

    private Organization fetchOrganization(String orgId) {

        Organization org = null;

        if (orgId == null) {
            orgId = this
                    .readInput("Organization to edit (enter id or blank to quit): ");
        }

        if (orgId.trim().equals("")) {
            printInfoMessage("No organization id entered, exiting edit");
        } else {
            try {
                org = dbManager.getOrganization(Integer.valueOf(orgId));
                if (org == null || org.getName() == null) {
                    printErrorMessage("could not find Organisation with id = "
                            + orgId);
                }
            } catch (DbException e) {
                handleException("could not find Organisation with id = "
                        + orgId, e);
            } catch (NumberFormatException e) {
                handleException("not a valid org id: " + orgId, e);
            } catch (org.hibernate.ObjectNotFoundException e) {
                handleException("Error: could not find Organisation with id = "
                        + orgId, e);
                org = fetchOrganization(null);
            }
        }
        return org;
    }

    private void editOrgProperties(Organization org) {

        printInfoMessage("\nEnter new or changed values, empty input will not change the current value");

        // Name
        editOrgName(org);

        // Registration number
        editRegNumber(org);

        // Country code
        editCountryCode(org);

        // Language code
        editLanguageCode(org);

        // Description
        editDescription(org);

        // Enable / Disable
        editOrgEnabled(org);

        // Priority
        editPriority(org);

        // Comment
        addOrgComment(org);

        printInfoMessage("\nDone editing organization properties");
        boolean orgSaved = saveOrganization(org);
        if (orgSaved) {
            // Show organization values without contact information
            viewOrganization(org, false);
        }
    }

    private void editOrganizationContacts(Organization org) {

        printContactManBanner();

        if (org == null) {
            org = fetchOrganization(null);
        }

        if (org != null) {
            editContacts(org);
            if (this.orgChanged) {
                saveOrganization(org);
            }
        }
    }

    private void editOrgName(Organization org) {

        this.orgChanged = enterValue("Organization name", "Name", org,
                MANDATORY, "Organization must have a name", null);
    }

    private void addOrgComment(Organization org) {

        String userAndDate = getUserNameWithTimestamp();
        println("Add new comment (empty string will skip this):");
        String newComment = readInput("> ");

        if (newComment.trim().equals("") == false) {
            if (org.getComment() != null
                    && org.getComment().equals("null") != true) {
                org.setComment(org.getComment() + "\n" + userAndDate
                        + newComment);
            } else {
                org.setComment(userAndDate + newComment);
            }
            this.orgChanged = true;
        }
    }

    private String getUserNameWithTimestamp() {

        return "["
                + this.currentUser
                + " "
                + DateUtil.formatDateTime(
                        DateUtil.DATE_TIME_FORMAT_WITH_T_CHAR, new Date())
                + "] ";
    }

    private void editDescription(Organization org) {

        this.orgChanged = enterValue("Description is", "Description", org,
                NOT_MANDATORY, "", null) ? true : this.orgChanged;
        /*
         * if (confirm("Current description is: " + org.getDescription() +
         * ". Edit?")) {
         * org.setDescription(readInput("Enter new description: "));
         * this.orgChanged = true; }
         */
    }

    private void editLanguageCode(Organization org) {

        this.orgChanged = enterValue("Language code is", "LanguageCode", org,
                MANDATORY, "Organization must have a language code", null) ? true
                : this.orgChanged;
    }

    private void editCountryCode(Organization org) {

        this.orgChanged = enterValue("Country code is", "CountryCode", org,
                MANDATORY, "Organization must have a country code", null) ? true
                : this.orgChanged;
    }

    private void editRegNumber(Organization org) {

        this.orgChanged = enterValue("Registration number is",
                "RegistrationNo", org, NOT_MANDATORY, "", null) ? true
                : this.orgChanged;
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

    private void editDomainNames(Organization org) throws DbException {

        boolean domainsChanged = false;
        String names = getDomainNamesInTextFormat(org);
        names = names.equals("") ? TEXT_EMPTY : names;
        printInfoMessage("Current domain names: " + names + ".");
        String action = readInput("Add [A] or delete [D] domains? (empty input will exit): ");

        if (action.toUpperCase().startsWith("A")) {
            String name = readInput("Enter domain name to add (empty input will exit): ");
            while (name.isEmpty() == false) {
                DomainName tmpDomainName = (DomainName) dbManager
                        .genericLoadObject("DomainName", "Name", name);
                if (tmpDomainName == null) {
                    tmpDomainName = new DomainName();
                    tmpDomainName.setName(name);
                    tmpDomainName.setOrganizationId(org.getId());
                    org.getDomainNames().add(tmpDomainName);
                    printInfoMessage("Domain name " + name + " has been added");
                    domainsChanged = true;
                } else {
                    // Check if object already exists or belongs to another org.
                    debug("Owning org id: " + tmpDomainName.getOrganizationId()
                            + ", editing org id: " + org.getId());

                    if (tmpDomainName.getOrganizationId().intValue() == org
                            .getId().intValue()) {
                        printErrorMessage("The domain does already belong to this organization");
                    } else {
                        printErrorMessage("conflicting domain name " + name
                                + " with org id "
                                + tmpDomainName.getOrganizationId());
                    }
                }
                name = readInput("Enter domain name to add (empty input will exit): ");
            }
        } else if (action.toUpperCase().startsWith("D")) {
            String name = readInput("Enter domain name to delete (empty input will exit): ");
            while (name.isEmpty() == false) {
                DomainName tmpDomainName = (DomainName) dbManager
                        .genericLoadObject("DomainName", "Name", name);
                if (tmpDomainName == null) {
                    printErrorMessage("The domain does not exist.");
                } else if (tmpDomainName.getOrganizationId().intValue() != org
                        .getId().intValue()) {
                    printErrorMessage("This domain belongs to the organization with org id "
                            + tmpDomainName.getOrganizationId());
                } else {
                    if (confirm("Domain "
                            + name
                            + " was found, are you sure that you want to delete it?")) {
                        org.getDomainNames().remove(tmpDomainName);
                        domainsChanged = true;
                        printInfoMessage("Domain " + name + " had been deleted");
                    }
                }
                name = readInput("Enter domain name to delete (empty input will exit): ");
            }
        } else {
            printInfoMessage("Exiting edit domains");
        }
        printInfoMessage("Done edit domains");
        if (domainsChanged) {
            dbManager.updateOrganization(org, this.currentUser);
        }
    }

    private String getASNumbersInTextFormat(Organization org) {

        String asn = "";
        if (org.getASNumbers() != null) {
            for (ASNumber asNumber : org.getASNumbers()) {
                asn = asn + asNumber.getNumber().toString() + ",";
            }
            if (asn.endsWith(",")) {
                asn = asn.substring(0, asn.length() - 1);
            }
        }
        return asn;
    }

    private void editASNumbers(Organization org) throws DbException {

        boolean asnChanged = false;
        String asn = getASNumbersInTextFormat(org);
        asn = asn.length() == 0 ? TEXT_EMPTY : asn;

        printInfoMessage("Current AS-numbers are: " + asn);
        String action = readInput("Add [A] or delete [D] AS numbers? (empty input will exit): ");

        if (action.toUpperCase().startsWith("A")) {
            String number = readInput("Enter AS number to add (empty input will exit): ");
            while (number.isEmpty() == false) {
                ASNumber tmpASN = (ASNumber) dbManager.genericLoadObject(
                        "ASNumber", "Number", Long.valueOf(number.trim()));
                if (tmpASN == null) {
                    tmpASN = new ASNumber();
                    tmpASN.setNumber(Long.valueOf(number.trim()));
                    tmpASN.setOrganizationId(org.getId());
                    org.getASNumbers().add(tmpASN);
                    printInfoMessage("AS number " + number + " has been added");
                    asnChanged = true;
                } else {
                    // Check if object already exists or belongs to another org.
                    debug("Owning org id: " + tmpASN.getOrganizationId()
                            + ", editing org id: " + org.getId());

                    if (tmpASN.getOrganizationId().intValue() == org.getId()
                            .intValue()) {
                        printErrorMessage("The AS number does already belong to this organization");
                    } else {
                        printErrorMessage("AS number " + number
                                + " belongs to org with id "
                                + tmpASN.getOrganizationId());
                    }
                }
                number = readInput("Enter AS number to add (empty input will exit): ");
            }
        } else if (action.toUpperCase().startsWith("D")) {
            String number = readInput("Enter AS number to delete (empty input will exit): ");
            while (number.isEmpty() == false) {
                ASNumber tmpASN = (ASNumber) dbManager.genericLoadObject(
                        "ASNumber", "Number", Integer.valueOf(number.trim()));
                if (tmpASN == null) {
                    printErrorMessage("The AS number does not exist.");
                } else if (tmpASN.getOrganizationId().intValue() != org.getId()
                        .intValue()) {
                    printErrorMessage("This AS number belongs to the organization with org id "
                            + tmpASN.getOrganizationId());
                } else {
                    if (confirm("AS number "
                            + number
                            + " was found, are you sure that you want to delete it?")) {
                        // Funkar inte; fixa:
                        debug("Number of ASNs in org before: "
                                + org.getASNumbers().size());
                        debug("Contains = "
                                + org.getASNumbers().contains(tmpASN));
                        Set<ASNumber> newASNs = org.getASNumbers();
                        newASNs.remove(tmpASN);
                        org.setASNumbers(newASNs);
                        debug("Number of ASNs in org after: "
                                + org.getASNumbers().size());
                        asnChanged = true;
                        printInfoMessage("AS number " + number
                                + " had been deleted");
                    }
                }
                number = readInput("Enter AS number to delete (empty input will exit): ");
            }
        } else {
            printInfoMessage("Exiting edit AS numbers");
        }
        printInfoMessage("Done edit AS numbers");
        if (asnChanged) {
            debug("Updating org");
            dbManager.updateOrganization(org, this.currentUser);
        }
    }

    private void listContacts(Organization org, Set<Contact> contacts) {

        if (contacts == null || contacts.isEmpty()) {
            println("\nThere are no existing contacts for this organization.");
        } else {
            printContactsBanner();
            for (Contact contact : contacts) {
                showContact(contact);
            }
        }
    }

    private Contact initNewContact() {

        // Setting default values
        Contact contact = new Contact();
        contact.setModifiedBy(this.currentUser);
        contact.setEnabled(true);
        contact.setCreated(SqlUtil.convertTimestamp(new Date()));
        contact.setComment("New contact.");
        // Set email type to default value TO
        contact.setEmailType(Message.RecipientType.TO.toString());

        this.newContact = true;

        return contact;

    }

    private void editContacts(Organization org) {

        int contactId = -1;
        Set<Contact> contacts = org.getContacts();

        if (contacts == null || contacts.isEmpty()) {
            if (confirm(("Organization " + org.getName() + " has no contacts, add a new contact? "))) {
                contactId = 0;
            }
        } else {
            // List contacts
            String contactIdStr = null;
            while (contactIdStr == null) {
                listContacts(org, contacts);
                // Fetch contact id
                contactIdStr = readInput("\nEnter id-number of one of the above contacts or 0 (zero) to add a new contact, (empty or invalid id will exit): ");
                if (contactIdStr != null && contactIdStr.isEmpty() == false) {
                    try {
                        contactId = Integer.parseInt(contactIdStr);
                    } catch (java.lang.NumberFormatException e) {
                        printInfoMessage("Not a valid contact id, exiting");
                        debug(e);
                        return;
                    }
                }
            }
        }

        if (contactId >= 0) {
            try {
                Contact contact = null;

                if (contactId == 0) {
                    // New contact
                    contact = initNewContact();
                } else if (contactId > 0) {
                    contact = (Contact) dbManager.genericLoadObject("Contact",
                            "Id", contactId);
                }

                if (contact == null) {
                    printErrorMessage("No contact found with id = " + contactId);
                } else {
                    String orgComment = editContact(contact);
                    if (this.contactChanged) {
                        // Set last modified
                        contact.setLastModified(SqlUtil
                                .convertTimestamp(new Date()));
                        // Set comment to show what has been done with the
                        // contact.
                        org.setComment(org.getComment() + "\n"
                                + getUserNameWithTimestamp() + " " + orgComment);
                    }
                    // Edit done.
                    this.contactChanged = true;
                    showContact(contact);
                    if (contactId == 0) {
                        org.addToContacts(contact);
                    }
                    if (confirm("Contact edited, edit more contacts?")) {
                        editContacts(org);
                    }
                }
            } catch (DbException e) {
                handleException("Database problem in editContacts: "
                        + e.getMessage(), e);
            }
        }
    }

    private boolean valueIsValid(String input, String[] validValues) {

        boolean valid = false;

        if (validValues == null) {
            // No values to compare with
            valid = true;
        } else {
            Set<String> validSet = new HashSet<String>(Arrays
                    .asList(validValues));
            if (validSet.contains(input)) {
                valid = true;
            }
        }
        return valid;
    }

    private boolean enterValue(String printText, String methodName,
            Object object, boolean mandatory, String errorMsg,
            String[] validValues) {

        /*
         * printText: a text to be show at the promt methodName: name of set
         * method object: the object to operate on mandatory: true if the value
         * is mandatory errorMsg: message that is printed when the value
         * criterias are not met validValues: allowed values
         */

        boolean valueChanged = false;

        try {

            Object[] emptyParams = new Object[] {};
            @SuppressWarnings("rawtypes")
            Class[] argTypes = new Class[] { String.class };
            Method getMethod = object.getClass().getMethod("get" + methodName);
            Method setMethod = object.getClass().getMethod("set" + methodName,
                    argTypes);
            String input = null;
            boolean valueOK = false;

            // Check if entered value is empty and if the current value is
            // empty. If both are empty and the value is mandatory then prompt
            // for the value again.
            // If the value is not mandatory and the value entered is blank then
            // nothing is changed. If the entered value is stored if it
            // differers from the current value.

            while (!valueOK) {

                input = readInput(printText
                        + ": "
                        + (getMethod.invoke(object, emptyParams) == null ? TEXT_EMPTY
                                : getMethod.invoke(object, emptyParams))
                        + "\n> ");

                // Check if input is valid, if empty input use the stored value.
                boolean valueIsValid = valueIsValid(input, validValues);

                if ((input == null || input.isEmpty())
                        && (getMethod.invoke(object, emptyParams) == null || ((String) (getMethod
                                .invoke(object, emptyParams))).isEmpty() == true)
                        && mandatory) {
                    // Input is empty and current value is empty and value is
                    // mandatory, print error message.
                    println(errorMsg + "\n");
                } else if (input.isEmpty() == false
                        && input.equals(getMethod.invoke(object, emptyParams)) == false
                        && valueIsValid) {
                    // Input is not empty and input is different from current
                    // value and valid.
                    setMethod.invoke(object, input);
                    valueOK = true;
                    valueChanged = true;
                } else if ((input == null || input.isEmpty())
                        && ((String) getMethod.invoke(object, emptyParams) != null && ((String) getMethod
                                .invoke(object, emptyParams)).isEmpty() == false)) {
                    // Input is null or empty but current value is valid.
                    valueIsValid = true;
                    valueOK = true;
                } else if (!valueIsValid) {
                    // Input is not valid.
                    printErrorMessage("Valid values are: "
                            + Arrays.toString(validValues));
                } else {
                    valueOK = true;
                }
            }
        } catch (Exception e) {
            handleException("exception in enterValue ", e);
        }
        return valueChanged;
    }

    private String editContact(Contact contact) {

        println("\nEnter new or changed values, empty input will not change the current value.\n");

        boolean namesAndEmailDone = false;
        String orgComment = "";

        // First contact
        if (this.newContact) {

            String contactInput = readInput("Enter First name only or First name, Last name and Email address in one line using format: First Last <address@domain.xxx> ");

            if (contactInput != null && contactInput.isEmpty() == false
                    && contactInput.contains("@")) {

                String[] contactInfo = contactInput.split("\\s");
                debug("Length = " + contactInfo.length);
                if (contactInfo.length == 3) {
                    contact.setFirstName(contactInfo[0].trim());
                    contact.setLastName(contactInfo[1].trim());

                    if (contactInfo[2] != null && contactInfo[2].length() > 2) {
                        String emailAddress = contactInfo[2].trim();
                        debug("1 email address = " + emailAddress);
                        if (emailAddress.startsWith("<")
                                && emailAddress.endsWith(">")) {
                            emailAddress = emailAddress.substring(1,
                                    emailAddress.length() - 1);
                            debug("2 email address = " + emailAddress);
                            contact.setEmailAddress(emailAddress);
                        }
                        namesAndEmailDone = true;
                    }
                }
            }
        }

        if (namesAndEmailDone == false) {
            // First name
            enterValue("First name", "FirstName", contact, NOT_MANDATORY, "",
                    null);

            // Last name
            enterValue("Last name", "LastName", contact, NOT_MANDATORY, "",
                    null);

            // Email address
            enterValue("Email address", "EmailAddress", contact, MANDATORY,
                    "Contact must have an email address.", null);
        }

        // Role
        enterValue("Role", "Role", contact, NOT_MANDATORY, "Valid roles are: "
                + Arrays.toString(this.validRoles), this.validRoles);

        // Phone number
        enterValue("Phone number", "PhoneNumber", contact, NOT_MANDATORY, "",
                null);

        // Email type
        enterValue("Email type", "EmailType", contact, MANDATORY,
                "Email type is mandatory and must have a value of: "
                        + Arrays.toString(EMAIL_TYPES), EMAIL_TYPES);

        // Enabled
        editContactEnabled(contact);

        // Comment
        String comment = null;
        if (this.newContact) {
            comment = "Contact created.";
            orgComment = comment;
        } else {
            orgComment = "Edited contact ID: " + contact.getId();
        }
        while (comment == null || comment.isEmpty()) {
            comment = readInput("Please enter a comment:\n> ");
        }
        String userNameTimestamp = getUserNameWithTimestamp();
        contact.setComment(userNameTimestamp + comment + "\n");
        contact.setLastModified(SqlUtil.convertTimestamp(new Date()));
        this.orgChanged = true;

        return orgComment;

    }

    private void editContactEnabled(Contact contact) {

        String input = readInput("The contact is: "
                + ((contact.isEnabled() ? "ENABLED" : "DISABLED.") + " (Enter D to disable or E to enable)\n> "));

        if (input != null) {
            if (input.toUpperCase().startsWith("D")) {
                contact.setEnabled(false);
            } else if (input.toUpperCase().startsWith("E")) {
                contact.setEnabled(true);
            }
        }
    }

    private void editIpRanges(Organization org) throws DbException {
        String ranges = "";
        String inputText = "Enter IP range to add (valid formats x.x.x.x-y.y.y.y, x.x.x.x-y or x.x.x.x/y, empty input will exit): ";
        boolean rangeChanged = false;
        String[] attrNames = { "StartAddress", "EndAddress" };
        ranges = getIpRangesInTextFormat(org);
        ranges = ranges.equals("") ? TEXT_EMPTY : ranges;
        printInfoMessage("\nCurrent IP-ranges: " + ranges);

        try {
            String action = readInput("Add [A] or delete [D] IP ranges? (empty input will exit): ");

            if (action.toUpperCase().startsWith("A")) {

                String range = readInput(inputText);
                while (range.isEmpty() == false) {
                    long[] IPrange = se.sitic.megatron.util.IpAddressUtil
                            .convertIpRange(range);
                    Object[] addressValues = { IPrange[0], IPrange[1] };
                    IpRange tmpRange = (IpRange) dbManager.genericLoadObject(
                            "IpRange", attrNames, addressValues);

                    if (tmpRange == null) {

                        // Check if range conflicts with existing range
                        List<java.math.BigInteger> result = dbManager
                                .isRangeOverlappingOrgId(IPrange[0], IPrange[1]);

                        if (result == null || result.isEmpty()) {
                            tmpRange = new IpRange();
                            tmpRange.setStartAddress(Long.valueOf(IPrange[0]));
                            tmpRange.setEndAddress(Long.valueOf(IPrange[1]));
                            tmpRange.setOrganizationId(org.getId());
                            org.getIpRanges().add(tmpRange);
                            printInfoMessage("IP ranges are now "
                                    + getIpRangesInTextFormat(org));
                            rangeChanged = true;
                        } else {
                            printErrorMessage("IP-range "
                                    + range
                                    + " overlaps with existing range(s) from organization(s) "
                                    + Arrays.toString(result.toArray()));
                        }
                    } else {
                        // Check if object already exists or belongs to another
                        // org.
                        debug("Owning org id: " + range + ", editing org id: "
                                + org.getId());

                        if (tmpRange.getOrganizationId().intValue() == org
                                .getId().intValue()) {
                            printErrorMessage("The IP range does already belong to this organization");
                        } else {
                            printErrorMessage(inputText
                                    + tmpRange.getOrganizationId());
                        }
                    }
                    range = readInput(inputText);
                }
            } else if (action.toUpperCase().startsWith("D")) {

                String range = readInput(inputText);

                while (range.isEmpty() == false) {
                    long[] IPrange = se.sitic.megatron.util.IpAddressUtil
                            .convertIpRange(range);
                    Object[] addressValues = { IPrange[0], IPrange[1] };
                    IpRange tmpRange = (IpRange) dbManager.genericLoadObject(
                            "IpRange", attrNames, addressValues);
                    if (tmpRange == null) {
                        printErrorMessage("The IP range does not exist.");
                    } else if (tmpRange.getOrganizationId().intValue() != org
                            .getId().intValue()) {
                        printErrorMessage("This IP range belongs to the organization with org id "
                                + tmpRange.getOrganizationId());
                    } else {
                        if (confirm("IP range "
                                + range
                                + " was found, are you sure that you want to delete it?")) {
                            org.getDomainNames().remove(tmpRange);
                            rangeChanged = true;
                            printInfoMessage("IP range " + range
                                    + " had been deleted");
                        }
                    }
                    range = readInput("Enter IP range to delete (valid formats x.x.x.x-y.y.y.y, x.x.x.x-y or x.x.x.x/y, empty input will exit): ");
                }
            } else {
                printInfoMessage("Exiting edit IP ranges");
            }
            if (rangeChanged) {
                dbManager.updateOrganization(org, this.currentUser);
            }
        } catch (MegatronException e) {
            handleException("", e);
        }
        printInfoMessage("Done editing IP ranges");
    }

    private String getIpRangesInTextFormat(Organization org) {

        String ranges = "";

        if (org.getIpRanges() != null) {
            for (IpRange range : org.getIpRanges()) {
                ranges = ranges
                        + IpAddressUtil.convertIpAddress(range
                                .getStartAddress(), false)
                        + "-"
                        + IpAddressUtil.convertIpAddress(range.getEndAddress(),
                                false) + ", ";
            }
            if (ranges.endsWith(", ")) {
                ranges = ranges.substring(0, ranges.length() - 2);
            }
        }
        return ranges;
    }

    private void editOrgEnabled(Organization org) {

        String input = readInput("The organizaton is: "
                + ((org.isEnabled() ? "ENABLED" : "DISABLED") + " (Enter D to disable or E to enable).\n> "));

        if (input != null) {
            if (input.toUpperCase().startsWith("D")) {
                org.setEnabled(false);
            } else if (input.toUpperCase().startsWith("E")) {
                org.setEnabled(true);
            }
            this.orgChanged = true;
        }
    }

    private void listPriorities(List<Priority> prios) {

        println("\nPriorities:");
        println("+------+----------------------------------------------------------------------------+");
        println("| Prio | Name                                                                       |");
        println("+------+----------------------------------------------------------------------------+");
        for (Priority prio : prios) {
            System.out.printf("| %4d | %-74s |\n", prio.getPrio(), prio
                    .getName());
        }
        println("+------+----------------------------------------------------------------------------+");
    }

    private void editPriority(Organization org) {

        boolean change = true;

        try {
            if (org.getPriority() != null) {
                print("Current priority: " + org.getPriority().getName() + " ["
                        + org.getPriority().getPrio() + "]");
                change = confirm("Edit?");
            }
            if (change) {
                List<Priority> prios = dbManager.getAllPriorities();
                Priority newPrio = null;
                while (newPrio == null) {
                    listPriorities(prios);
                    println("\nEnter prio-number of one of the above priorities: ");
                    String prioNo = readInput("> ");
                    if (prioNo != null && prioNo.isEmpty() == false) {
                        newPrio = (Priority) dbManager.genericLoadObject(
                                "Priority", "Prio", prioNo);
                    }
                    if (newPrio != null) {
                        org.setPriority(newPrio);
                        this.orgChanged = true;
                    } else {
                        printErrorMessage("sorry, no priority with # " + prioNo
                                + " was found");
                    }
                }
            }
        } catch (DbException e) {
            handleException("database exception in editPriority", e);
        }
    }

    private boolean saveOrganization(Organization org) {

        boolean saved = false;

        try {
            if (this.newOrg) {
                dbManager.addOrganization(org, this.currentUser);
                saved = true;
                this.newOrg = false;
            } else if (this.orgChanged) {
                dbManager.updateOrganization(org, this.currentUser);
                saved = true;
                this.orgChanged = false;
            }
        } catch (DbException e) {
            handleException("Could not save organization", e);
        }

        return saved;
    }

    private void viewOrganization(Organization org, boolean showContacts) {

        if (org == null) {
            String orgId = this.readInput("Organization to show (enter id): ");

            try {
                org = dbManager.getOrganization(Integer.valueOf(orgId));
            } catch (DbException e) {
                handleException("could not find an Organization with id = "
                        + orgId, e);
                return;
            } catch (NumberFormatException e) {
                handleException("not a valid Organization id format: " + orgId,
                        e);
                return;
            } catch (Exception e) {
                handleException("unknown error", e);
                return;
            }
        }

        println("\n");
        printOrganizationBanner();
        println("");
        // println("+-----------------------------------------------------------------------------------+");
        // printf("| %14s:  %-64s |\n", "Organization" , org.getName());
        // println("+-----------------------------------------------------------------------------------+\n");
        printf(" %-16s: %d\n", "Id", org.getId());
        printf(" %-16s: %s\n", "Name", org.getName());
        printf(" %-16s: %s\n", "Org/Reg no", replaceNullValue(org
                .getRegistrationNo()));
        printf(" %-16s: %s\n", "Ip-ranges", getIpRangesInTextFormat(org));
        printf(" %-16s: %s\n", "Domain names", getDomainNamesInTextFormat(org));
        printf(" %-16s: %s\n", "AS-numbers", getASNumbersInTextFormat(org));
        printf(" %-16s: %s [%d]\n", "Priority", org.getPriority().getName(),
                org.getPriority().getPrio());
        printf(" %-16s: %s\n", "Country code", replaceNullValue(org
                .getCountryCode()));
        printf(" %-16s: %s\n", "Language code", replaceNullValue(org
                .getLanguageCode()));
        printf(" %-16s: %s\n", "Status", (org.isEnabled() ? "ENABLED"
                : "DISABLED"));
        printf(" %-16s: %s\n", "Description", replaceNullValue(org
                .getDescription()));
        printf(" %-16s: %s\n", "Created", toStringDate(org.getCreated()));
        printf(" %-16s: %s\n", "Last modified", toStringDate(org
                .getLastModified()));
        printf(" %-16s: \n%s\n", "Comments", replaceNullValue(org.getComment()));
        if (debug) {
            printf(" %-16s: %s\n", "All EmailAddresses", org.getEmailAddresses(
                    null, false));
            printf(" %-16s: %s\n", "Enabled EmailAddresses", org
                    .getEmailAddresses(null, true));
            printf(" %-16s: %s\n", "To EmailAddresses", org.getEmailAddresses(
                    "to", true));
            printf(" %-16s: %s\n", "CC EmailAddresses", org.getEmailAddresses(
                    "cc", true));
            printf(" %-16s: %s\n", "BCC EmailAddresses", org.getEmailAddresses(
                    "bcc", true));
        }
        println("");

        if (showContacts) {
            Set<Contact> contacts = org.getContacts();
            listContacts(org, contacts);
        }
    }

    private void showContact(Contact contact) {

        // println("+-----------------------------------------------------------------------------------+");
        // printf("| %8s:  %-70s |\n", "Contact" , contact.getFirstName() + " "
        // + contact.getLastName());
        // println("+-----------------------------------------------------------------------------------+\n");
        println("");
        printf(" %-18s: %d\n", "Id", contact.getId());
        printf(" %-18s: %s\n", "First name", contact.getFirstName());
        printf(" %-18s: %s\n", "Last name", replaceNullValue(contact
                .getLastName()));
        printf(" %-18s: %s\n", "Role", replaceNullValue(contact.getRole()));
        printf(" %-18s: %s\n", "Email address", contact.getEmailAddress());
        printf(" %-18s: %s\n", "Email type", contact.getEmailType());
        printf(" %-18s: %s\n", "Phone number", replaceNullValue(contact
                .getPhoneNumber()));
        printf(" %-18s: %s\n", "Status", (contact.isEnabled() ? "ENABLED"
                : "DISABLED"));
        printf(" %-18s: %s\n", "Created", toStringDate(contact.getCreated()));
        printf(" %-18s: %s\n", "Last modified", toStringDate(contact
                .getLastModified()));
        printf(" %-18s: %s\n", "ExternalReference", replaceNullValue(contact
                .getExternalReference()));
        printf(" %-18s: \n%s\n", "Comments", replaceNullValue(contact
                .getComment()));
    }

    private void outputLogEntries(List<LogEntry> entries, String format,
            boolean showFull, PrintWriter out) {

        debug("In outputLogEntries");

        if (format.toLowerCase().startsWith("c")) {
            debug("CSV format");
            out.append("# id,org id,IP address,port,host name,ASN,country code,URL,IP range start,IP range end,"
                    + "org id 2,IP address 2,port 2,host name 2,ASN 2,country code 2,log timestamp,created,"
                    + "original log entry,job id,job type,additional items\n");

            for (LogEntry entry : entries) {
                out.append(entry.getId() + ",");
                out.append(entry.getOrganization().getId() + ",");
                out.append((entry.getIpAddress() != null ? IpAddressUtil
                        .convertIpAddress(entry.getIpAddress(), false) : "")
                        + ",");
                out.append(replaceNullValue(entry.getPort(), "") + ",");
                out.append(replaceNullValue(entry.getHostname(), "") + ",");
                out.append(replaceNullValue(entry.getAsn(), "") + ",");
                out.append(replaceNullValue(entry.getCountryCode(), "") + ",");
                out.append(replaceNullValue(entry.getUrl(), "") + ",");
                out.append((entry.getIpRangeStart() != null ? IpAddressUtil
                        .convertIpAddress(entry.getIpRangeStart(), false) : "")
                        + ",");
                out.append((entry.getIpRangeEnd() != null ? IpAddressUtil
                        .convertIpAddress(entry.getIpRangeEnd(), false) : "")
                        + ",");
                out.append((entry.getOrganization2() != null ? entry
                        .getOrganization2().getId() : "")
                        + ",");
                out.append((entry.getIpAddress2() != null ? IpAddressUtil
                        .convertIpAddress(entry.getIpAddress2(), false) : "")
                        + ",");
                out.append(replaceNullValue(entry.getPort2(), "") + ",");
                out.append(replaceNullValue(entry.getHostname2(), "") + ",");
                out.append(replaceNullValue(entry.getAsn2(), "") + ",");
                out.append(replaceNullValue(entry.getCountryCode2(), "") + ",");
                out.append(entry.getLogTimestamp() == null ? TEXT_NULL
                        : toStringDate(entry.getLogTimestamp()) + ",");
                out.append(entry.getCreated() == null ? TEXT_NULL
                        : toStringDate(entry.getCreated()) + ",");
                out.append("\""
                        + replaceNullValue(entry.getOriginalLogEntry()
                                .getEntry()
                                + "\"", "") + ",");
                out.append(entry.getJob().getId() + ",");
                out.append(entry.getJob().getJobType().getName() + ",");
                // Print any additional items
                if (entry.getAdditionalItems() != null
                        && entry.getAdditionalItems().size() > 0) {

                    Map<String, String> items = entry.getAdditionalItems();
                    out.append('"');
                    for (String key : items.keySet()) {
                        out.append(key + ":" + items.get(key) + ",");
                    }
                    out.append('"');
                }
                out.append('\n');
            }
        }

        else {

            for (LogEntry entry : entries) {
                debug("Table format");
                out.printf("\n+-----------------------------------------------------------------------------------+\n");
                out.printf("| %12s:  %-66d |\n", "Log Entry ID", entry.getId());
                out.printf("+-----------------------------------------------------------------------------------+\n");
                out.printf(" %-18s: %s\n", "Organization name", entry
                        .getOrganization().getName());
                out.printf(" %-18s: %s\n", "Organization ID", entry
                        .getOrganization().getId());
                out.printf(" %-18s: %s\n", "IP address",
                        (entry.getIpAddress() != null ? IpAddressUtil
                                .convertIpAddress(entry.getIpAddress(), false)
                                : TEXT_NULL));
                out.printf(" %-18s: %s\n", "Port", replaceNullValue(entry
                        .getPort()));
                out.printf(" %-18s: %s\n", "Host name", replaceNullValue(entry
                        .getHostname()));
                out.printf(" %-18s: %s\n", "ASN", replaceNullValue(entry
                        .getAsn()));
                out.printf(" %-18s: %s\n", "Country code",
                        replaceNullValue(entry.getCountryCode()));
                out.printf(" %-18s: %s\n", "URL", replaceNullValue(entry
                        .getUrl()));
                if (showFull) {
                    // Print the rest
                    out.printf(" %-18s: %s\n", "Organization ID 2", entry
                            .getOrganization2() != null ? entry
                            .getOrganization2().getId() : TEXT_NULL);
                    out.printf(" %-18s: %s\n", "IP address 2", (entry
                            .getIpAddress2() != null ? IpAddressUtil
                            .convertIpAddress(entry.getIpAddress2(), false)
                            : TEXT_NULL));
                    out.printf(" %-18s: %s\n", "Port 2", replaceNullValue(entry
                            .getPort2()));
                    out.printf(" %-18s: %s\n", "Host name 2",
                            replaceNullValue(entry.getHostname2()));
                    out.printf(" %-18s: %s\n", "ASN 2", replaceNullValue(entry
                            .getAsn2()));
                    out.printf(" %-18s: %s\n", "Country code 2",
                            replaceNullValue(entry.getCountryCode2()));
                    out.printf(" %-18s: %s\n", "IP range start", (entry
                            .getIpRangeStart() != null ? IpAddressUtil
                            .convertIpAddress(entry.getIpRangeStart(), false)
                            : TEXT_NULL));
                    out.printf(" %-18s: %s\n", "IP range end", (entry
                            .getIpRangeEnd() != null ? IpAddressUtil
                            .convertIpAddress(entry.getIpRangeEnd(), false)
                            : TEXT_NULL));
                }
                out.printf(" %-18s: %s\n", "Log timestamp", entry
                        .getLogTimestamp() == null ? TEXT_NULL
                        : toStringDate(entry.getLogTimestamp()));
                out.printf(" %-18s: %s\n", "Created timestamp",
                        toStringDate(entry.getCreated()));
                out.printf(
                        " %-18s: %s\n",
                        "Original log entry",
                        replaceNullValue(entry.getOriginalLogEntry().getEntry()));
                out.printf(" %-18s: %s\n", "Job ID", entry.getJob().getId());
                out.printf(" %-18s: %s\n", "Job type", entry.getJob()
                        .getJobType().getName());
                // Print any additional items
                if (entry.getAdditionalItems() != null
                        && entry.getAdditionalItems().size() > 0) {
                    Map<String, String> items = entry.getAdditionalItems();
                    for (String key : items.keySet()) {
                        out.printf(" %-18s: %s\n", key, items.get(key));
                    }
                }
            }
            out.append("\n");
        }
        debug("Done outputLogEntries");
    }

    private String replaceNullValue(Object value) {
        return replaceNullValue(value, TEXT_NULL);
    }

    private String replaceNullValue(Object value, String newValue) {
        return value != null ? value.toString() : newValue;
    }

    private void quit() {

        println("\nOK, catch you later!\n");
        System.exit(0);
    }

    private void listRoleNames(String[] names) {

        println("\nContact role names:");
        for (String name : names) {
            println(name);
        }
    }

    private void exportContacts() {

        String roles = "";
        String prios = "";
        printExportContactsBanner();

        println("\nContacts to be exported can be searched for by org-id or prio number, together with role names.");
        println("If role name is skipped, all roles will be valid and searched for.");
        println("Multiple input values has to be entered comma separated e.g 4,7,9.");
        println("Empty input will exclude that search parameter.");
        println("The search criterias are AND:ed");

        String orgs = readInput("\nEnter organization IDs: ");

        try {
            List<String> roleNames = dbManager.getAllContactRoleNames();

            String[] roleList = roleNames.toArray(new String[roleNames.size()]);
            listRoleNames(roleList);
            roles = readInput("\nEnter role names: ");
        } catch (DbException e) {
            handleException("could not fetch all role names", e);
        }

        Set<Contact> contacts = null;

        if (orgs.isEmpty() == false) {

            // Search for organization IDs
            String[] orgIDs = orgs.split(",");

            try {
                debug("searching by orgid and roles");
                contacts = new TreeSet<Contact>(dbManager
                        .searchContactsByOrgIDsAndRoles(orgIDs, roles));
            } catch (DbException e) {
                handleException("Could not search contact by OrgID and roles",
                        e);
            }
        }

        else {
            try {
                List<Priority> allPrios = dbManager.getAllPriorities();
                listPriorities(allPrios);
                prios = readInput("\nEnter priority levels to include, from highest to lowest e.g. 90-50:");
            } catch (DbException e) {
                handleException("could not fetch all priorities", e);
            }

            if (prios.isEmpty() == false) {
                String[] searchPrios = prios.split("-");
                String searchRoles = roles;

                try {
                    debug("searching by prio and roles");
                    contacts = new TreeSet<Contact>(dbManager
                            .searcContactsByPrioAndRoles(searchPrios[0],
                                    searchPrios[1], searchRoles));
                } catch (DbException e) {
                    handleException(
                            "Could not search contact by prios and roles", e);
                }
            } else {
                try {
                    debug("searching by null orgid and empty roles");
                    contacts = new TreeSet<Contact>(dbManager
                            .searchContactsByOrgIDsAndRoles(null, roles));
                } catch (DbException e) {
                    handleException(
                            "Could not search contact by empty org id and empty roles",
                            e);
                }

            }
        }

        String outputType = readInput("Enter output type [a] all, [e| email only: ");
        String outputFormat = readInput("Enter output format [t] tab separated or [c] comma separated: ");
        String outputLocation = readInput("Enter output location [s] screen, [f] file: ");

        if (contacts != null && contacts.isEmpty() == false) {
            outputContacts((Set<Contact>) contacts, outputType, outputFormat,
                    outputLocation);
        } else {
            printInfoMessage("\nNo contacts where found using the given search criterias.");
        }
    }

    private void outputContacts(Set<Contact> contacts, String type,
            String format, String location) {

        String output = "";
        String header = "";
        String delim = ",";

        // If tab:ed output format selected, set delimiter to \t
        if (format != null && format.toUpperCase().startsWith("T")) {
            delim = "\t";
        }

        if (contacts != null) {
            for (Contact contact : contacts) {
                if (type.toUpperCase().startsWith("E")) {
                    header = "# Email type" + delim + "Email address";
                    output = output + contact.getEmailType() + delim
                            + contact.getEmailAddress() + "\n";
                } else {
                    header = "# First name" + delim + "Last name" + delim
                            + "Role" + delim + "Email address" + delim
                            + "Email" + delim + "type" + delim + "Phone number";
                    output = output + contact.getFirstName() + delim
                            + contact.getLastName() + delim + contact.getRole()
                            + delim + contact.getEmailAddress() + delim
                            + contact.getEmailType() + delim
                            + contact.getPhoneNumber() + "\n";
                }
            }

            // Replace any null strings with empty string
            output = output.replaceAll("null", "");

            if (location.toUpperCase().startsWith("F")) {
                PrintWriter writer = null;
                try {
                    FileWriter fstream = new FileWriter(this.outputFilePath
                            + "/" + CONTACT_EXPORT_FILE_NAME, false);
                    writer = new PrintWriter(fstream);
                    writer.write(header + "\n" + output);
                    printInfoMessage("Done writing contacts to "
                            + this.outputFilePath + "/"
                            + CONTACT_EXPORT_FILE_NAME);
                    writer.close();
                } catch (IOException e) {
                    System.err.println("Error: " + e.getMessage());
                }
            } else {
                println("\n+-----------------------------------------------------------------------------------+");
                println("| Exported Contacts                                                                 |");
                println("+-----------------------------------------------------------------------------------+");
                println(header + "\n" + output);
                println("-------------------------------------------------------------------------------------\n");
            }
        }
    }

    private String toStringDate(java.lang.Long timeStamp) {
        return timeStamp != null ? DateUtil.formatDateTime(
                this.exportTimstampFormat, SqlUtil.convertTimestamp(timeStamp))
                : "";
    }
}