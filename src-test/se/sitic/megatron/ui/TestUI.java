package se.sitic.megatron.ui;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.CommandLineParseException;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.db.DbManager;
import se.sitic.megatron.entity.ASNumber;
import se.sitic.megatron.entity.DomainName;
import se.sitic.megatron.entity.EntryType;
import se.sitic.megatron.entity.IpRange;
import se.sitic.megatron.entity.Job;
import se.sitic.megatron.entity.JobType;
import se.sitic.megatron.entity.LogEntry;
import se.sitic.megatron.entity.MailJob;
import se.sitic.megatron.entity.Organization;
import se.sitic.megatron.entity.OriginalLogEntry;
import se.sitic.megatron.entity.Priority;
import se.sitic.megatron.ui.OrganizationHandler;
import se.sitic.megatron.util.SqlUtil;

public class TestUI {

    private static Session session = null;
    private static Transaction tx = null;
    private static DbManager dBm = null;
    private static Logger log = null;
    private static boolean TRUNCATE = true;
    @SuppressWarnings("unused")
    private static boolean DONT_TRUNCATE = !TRUNCATE;
    

    

    /**
     * @param args
     */
    public static void main( String[] args ) {

        
        AppProperties props = AppProperties.getInstance();
        try {
            props.init(null);
        } catch (CommandLineParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (MegatronException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        if (props.getGlobalProperties() == null) return;
        
        OrganizationHandler orghandler = new OrganizationHandler(props.getGlobalProperties());
    
        orghandler.startUI();
        
    }

}
