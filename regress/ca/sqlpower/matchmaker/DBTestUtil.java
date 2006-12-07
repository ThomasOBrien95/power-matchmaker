package ca.sqlpower.matchmaker;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import ca.sqlpower.architect.ArchitectDataSource;
import ca.sqlpower.matchmaker.dao.hibernate.TestingConnection;

/**
 * A collection of useful static methods that you will probably need when
 * developing database-related test cases.
 */
public class DBTestUtil {

    private static final Logger logger = Logger.getLogger(DBTestUtil.class);
    
    /**
     * A cache of the connections we've made so far.
     */
    private static final Map<ArchitectDataSource, Connection> connections =
        new HashMap<ArchitectDataSource, Connection>();

    /**
     * Returns a JDBC connection to the given data source.  Once a successful connection
     * is established to a particular data source, the same one will be returned over
     * and over.  That means that test methods using this facility should make sure they
     * leave the connection in a good default state (not disabled and not closed).
     * 
     * @param dataSource The data source to connect to
     * @return A TestingConnection which is connected to the given data source
     * @throws InstantiationException If the JDBC driver can't be created
     * @throws IllegalAccessException If the JDBC driver can't be created
     * @throws ClassNotFoundException If the JDBC driver can't be found
     * @throws SQLException If there is a JDBC error (invalid username, password, or database url)
     */
    public static TestingConnection connectToDatabase(ArchitectDataSource dataSource)
    throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
        
        if (connections.get(dataSource) == null) {
            logger.info("*** Connecting to Database: "+dataSource);
            Driver driver = (Driver) Class.forName(dataSource.getDriverClass()).newInstance();
            if (!driver.acceptsURL(dataSource.getUrl())) {
                throw new SQLException("Couldn't connect to database:\n"
                        +"JDBC Driver "+dataSource.getDriverClass()+"\n"
                        +"does not accept the URL "+dataSource.getUrl());
            }
            Properties connectionProps = new Properties();
            connectionProps.setProperty("user", dataSource.getUser());
            connectionProps.setProperty("password", dataSource.getPass());
            Connection mycon = driver.connect(dataSource.getUrl(), connectionProps);
            if (mycon == null) {
                throw new SQLException("Couldn't connect to datasource " + dataSource +
                        " (driver returned null connection)");
            }
            connections.put(dataSource, mycon);
        }
        return new TestingConnection(connections.get(dataSource));
    }

    /**
     * Returns a new ArchitectDataSource which is configured to connect to
     * our SQL Server 2000 test database.  The pl schema is in plautotest.
     */
    public static ArchitectDataSource getSqlServerDS() { 
    	/*
    	 * Setup information for SQL Server
    	 */
    	final String ssUserName = "plautotest";
    	final String ssPassword = "TIhR2Es0";
    	final String ssUrl ="jdbc:microsoft:sqlserver://deepthought:1433;SelectMethod=cursor;DatabaseName=plautotest";
    	
    	ArchitectDataSource sqlServerDS = new ArchitectDataSource();
    	sqlServerDS.setDriverClass("com.microsoft.jdbc.sqlserver.SQLServerDriver");
    	sqlServerDS.setName("Test SQLServer");
    	sqlServerDS.setUser(ssUserName);
    	sqlServerDS.setPass(ssPassword);
    	sqlServerDS.setPlDbType("sql server");
    	sqlServerDS.setPlSchema("plautotest");
    	sqlServerDS.setUrl(ssUrl);
    	return sqlServerDS;
    }

    /**
     * Returns a new ArchitectDataSource which is configured to connect to
     * our Oracle 8i test database.  The PL schema is in mm_test.
     */
    public static ArchitectDataSource getOracleDS() { 
        /*
         * Setup information for Oracle
         */
        final String oracleUserName = "mm_test";
        final String oraclePassword = "cowmoo";
        final String oracleUrl = "jdbc:oracle:thin:@arthur:1521:test";
        
        ArchitectDataSource oracleDataSource = new ArchitectDataSource();
        oracleDataSource.setDriverClass("oracle.jdbc.driver.OracleDriver");
        oracleDataSource.setName("Test Oracle");
    
        oracleDataSource.setUser(oracleUserName);
        oracleDataSource.setPass(oraclePassword);
        oracleDataSource.setPlDbType("ORACLE");
        oracleDataSource.setPlSchema("mm_test");
        oracleDataSource.setUrl(oracleUrl);
        return oracleDataSource;
    }

    /**
     * Returns a new ArchitectDataSource which is configured to create
     * an in-memory (non persistent) HSQLDB instance.  The PL schema is in pl.
     */
    public static ArchitectDataSource getHSQLDBInMemoryDS() {
        final String hsqlUserName = "sa";
        final String hsqlPassword = "";
        final String hsqlUrl = "jdbc:hsqldb:mem:aname";
        
        ArchitectDataSource hsqlDataSource = new ArchitectDataSource();
        hsqlDataSource.setDriverClass("org.hsqldb.jdbcDriver");
        hsqlDataSource.setName("In-memory HSQLDB");
    
        hsqlDataSource.setUser(hsqlUserName);
        hsqlDataSource.setPass(hsqlPassword);
        hsqlDataSource.setPlDbType("hsql");
        hsqlDataSource.setPlSchema("pl");
        hsqlDataSource.setUrl(hsqlUrl);
        return hsqlDataSource;
    }

}
