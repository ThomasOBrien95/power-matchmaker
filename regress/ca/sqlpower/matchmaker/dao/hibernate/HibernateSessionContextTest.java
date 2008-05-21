/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of Power*MatchMaker.
 *
 * Power*MatchMaker is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Power*MatchMaker is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.matchmaker.dao.hibernate;

import java.sql.Connection;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import junit.framework.TestCase;
import ca.sqlpower.matchmaker.DBTestUtil;
import ca.sqlpower.matchmaker.MatchMakerSession;
import ca.sqlpower.matchmaker.MatchMakerSessionContext;
import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.PLSchemaException;
import ca.sqlpower.sql.PlDotIni;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.util.Version;

public class HibernateSessionContextTest extends TestCase {

    /**
     * The session context we're testing.
     */
	private MatchMakerSessionContext ctx;
    
    /**
     * The sole data source the setUp() method puts in the session context.  Provided
     * here for convenience.  You could get the same data source with ctx.getDataSources().get(0).
     */
	private SPDataSource ds;
    
	/**
	 * The Preferences node that we will use in this test. We want to keep
	 * this separate from the regular MatchMaker Preferences to ensure the test
	 * suite doesn't interfere with the user's preferences.
	 */
	Preferences prefs = Preferences.userNodeForPackage(MatchMakerHibernateSessionContext.class).node("test");
	
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        DataSourceCollection ini = new PlDotIni();
        ds = DBTestUtil.getOracleDS();
        ini.addDataSource(ds);
        ctx = new MatchMakerHibernateSessionContext(prefs, ini);
    }
    
    public void testGetDataSources() {
        assertNotNull(ctx.getDataSources());
    }
    
	public void testCreateSession() throws Exception {
		MatchMakerSession session = ctx.createSession(ds, ds.getUser(), ds.getPass());
		assertNotNull(session);
	}
    
    public void testCheckSchemaVersion() throws Exception {
        SPDataSource ds = DBTestUtil.getHSQLDBInMemoryDS();

        Version v = new Version();
        v.setMajor(RepositoryUtil.MIN_PL_SCHEMA_VERSION.getMajor() - 1);
        v.setMinor(RepositoryUtil.MIN_PL_SCHEMA_VERSION.getMinor());
        v.setTiny(RepositoryUtil.MIN_PL_SCHEMA_VERSION.getTiny());
        
        // this is very simplistic, and assumes that the startup sequence of
        // MatchMakerHibernateSessionImpl checks the schema version before accessing
        // the database in any other way.  The only thing in the whole database is a
        // DEF_PARAM table with one column!
        Connection con = DBTestUtil.connectToDatabase(ds);
        Statement stmt = con.createStatement();
        stmt.executeUpdate("CREATE TABLE "+ds.getPlSchema()+".MM_SCHEMA_INFO (PARAM_NAME VARCHAR(50), PARAM_VALUE VARCHAR(2000))");
        stmt.executeUpdate("INSERT INTO "+ds.getPlSchema()+".MM_SCHEMA_INFO VALUES ('schema_version', '"+v.toString()+"')");
        stmt.close();
        
        try {
            // if this fails for any reason other than version mismatch, you will have
            // to enhance the "pl schema create script" above, or modify the HibernateSession
            // to check the schema version earlier.
            ctx.createSession(ds, ds.getUser(), ds.getPass());
            fail("Session init failed to report bad schema version");
        } catch (PLSchemaException ex) {
            assertEquals(RepositoryUtil.MIN_PL_SCHEMA_VERSION.toString(), ex.getRequiredVersion());
            assertEquals(v.toString(), ex.getCurrentVersion());
        }
    }
    /**
     * The context might have to alter the username and password in the data source
     * we give it, but we don't want this to permanently alter the settings in pl.ini.
     * This test insures the data source we give it remains untouched.
     */
    public void testDataSourceNotModifiedByLogin() {
        Map<String, String> originalProps = new HashMap<String, String>(ds.getPropertiesMap());
        try {
            ctx.createSession(ds, "cows", "moo");
        } catch (Exception ex) {
            // the username/password is wrong, so the login will fail. That's not what we're testing
        }
        assertEquals("DataSource should not have been modified", originalProps, ds.getPropertiesMap());
    }
    
    public void testGetHibernateSessionFactory() throws Exception{
        MatchMakerSession mmSession = ctx.createSession(ds, ds.getUser(), ds.getPass());
        //we're testing this since the purpose of the getHibernateSessionFactory is to
        //initialize the PlFolder
        assertNotNull(mmSession.getCurrentFolderParent().getChildren());
    }
}