package se.sitic.megatron.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.util.DateUtil;


// TODO Move code to DbManager and make use of Hibernate.


/**
 * Handles db operations for the asn-table.
 */
public class AsnLookupDbManager {
    private static final Logger log = Logger.getLogger(AsnLookupDbManager.class);
    
    private TypedProperties props;
    private Connection conn;

    
    public AsnLookupDbManager(TypedProperties props) throws DbException {
        this.props = props;

        try {
            conn = createConnection();
        } catch (Exception e) {
            // ClassNotFoundException, SQLException
            throw new DbException("Cannot create a database connection.", e);
        }
    }

    
    public int deleteAsnLookupData() throws DbException {
        int result = 0;
        String sql = "delete from asn_lookup";

        log.debug("Executing sql (deleteAsnLookupData): " + sql);
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(sql);
            result = stmt.executeUpdate();
            stmt.close();
            log.debug("Sql executed. No. of rows deleted: " + result); 
        } catch (SQLException e) {
            try { if (stmt != null) stmt.close(); } catch (Exception ignored) { }  
            throw new DbException("Cannot execute sql: " + sql, e);
        }
        return result;
    }

    
    public void addAsnLookup(long startAddress, long endAddress, long asn) throws DbException {
        String sql = "insert into asn_lookup (start_address, end_address, asn) values (?, ?, ?)";

        // log.debug("Executing sql (addAsnLookup): " + sql);
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, startAddress);
            stmt.setLong(2, endAddress);
            stmt.setLong(3, asn);
            stmt.executeUpdate();
            stmt.close();
            // log.debug("Sql executed."); 
        } catch (SQLException e) {
            try { if (stmt != null) stmt.close(); } catch (Exception ignored) { }  
            throw new DbException("Cannot execute sql: " + sql, e);
        }
    }

    
    public long searchAsn(long ipAddress) throws DbException {
        // IP intervals in the asn_looup-table may overlap. We are looking for
        // the smallest interval that the specified IP address is a member of.
        
        long result = -1;
        String sql = "select asn, start_address, end_address from asn_lookup where ? between start_address and end_address";

        long t1 = System.currentTimeMillis();
        log.debug("Executing sql (searchAsn): " + sql);
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, ipAddress);
            long rangeDiff = Long.MAX_VALUE;
            ResultSet resultSet = stmt.executeQuery();
            while (resultSet.next()) {
                long rangeDiffCanidate = resultSet.getLong(3) - resultSet.getLong(2);
                if ((rangeDiffCanidate > 0L) && (rangeDiffCanidate < rangeDiff)) {
                    result = resultSet.getLong(1);
                    rangeDiff = rangeDiffCanidate;
                }
            }
            stmt.close();
            String duration = DateUtil.formatDuration(System.currentTimeMillis() - t1);
            log.debug("Sql executed (" + duration + "). IP --> ASN: " + ipAddress + " --> " + result); 
        } catch (SQLException e) {
            try { if (stmt != null) stmt.close(); } catch (Exception ignored) { }  
            throw new DbException("Cannot execute sql: " + sql, e);
        }
        return result;
    }
    
    
    public void close() throws DbException {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                throw new DbException("Cannot close database connection.", e);
            }
        }
    }
    
    
    private Connection createConnection() throws ClassNotFoundException, SQLException {
        String driverClassName = props.getString(AppProperties.JDBC_DRIVER_CLASS_KEY, "com.mysql.jdbc.Driver");
        String url = AppProperties.getInstance().getJdbcUrl();
        String user = props.getString(AppProperties.DB_USERNAME_KEY, "megatron");
        String password = props.getString(AppProperties.DB_PASSWORD_KEY, "megatron");

        Class.forName(driverClassName);
        return DriverManager.getConnection(url, user, password);
    }
    
}
