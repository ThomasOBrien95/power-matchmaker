/*
 * Copyright (c) 2007, SQL Power Group Inc.
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

package ca.sqlpower.matchmaker;

import java.sql.Connection;
import java.sql.Date;

import ca.sqlpower.architect.ArchitectException;
import ca.sqlpower.architect.ArchitectRuntimeException;
import ca.sqlpower.architect.SQLDatabase;
import ca.sqlpower.architect.SQLIndex;
import ca.sqlpower.architect.ddl.DDLGenerator;
import ca.sqlpower.architect.ddl.DDLStatement;
import ca.sqlpower.architect.ddl.DDLUtils;
import ca.sqlpower.matchmaker.dao.MatchMakerDAO;
import ca.sqlpower.matchmaker.dao.StubMatchMakerDAO;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.sql.SQL;

/**
 * This is a class to test the merge processor on a sql server test server.
 */
public class MergeProcessorSQLServerTest extends AbstractMergeProcessorTest {
	
	protected void setUp() throws Exception {
		match = new Match();
		
        SPDataSource ds = DBTestUtil.getSqlServerDS();
        SQLDatabase db = new SQLDatabase(ds);
        session = new TestingMatchMakerSession() {
			SPDataSource ds = DBTestUtil.getSqlServerDS();
			SQLDatabase db = new SQLDatabase(ds);
			
			@Override
			public Connection getConnection() {
				try {
					return db.getConnection();
				} catch (ArchitectException e) {
					throw new ArchitectRuntimeException(e);
				}
			}
			
			@Override
			public <T extends MatchMakerObject> MatchMakerDAO<T> getDAO(Class<T> businessClass) {
		        return new StubMatchMakerDAO<T>(businessClass);
		    }
		};
		session.setDatabase(db);
		
		match.setSession(session);
        session.setConnection(db.getConnection());
	}
	
	@Override
	protected void populateTables() throws Exception {
		super.populateTables();
		
    	SQLDatabase db = session.getDatabase();
    	SPDataSource ds = session.getDatabase().getDataSource();
    	Connection con = db.getConnection();
		String sql = "DROP TABLE " + getFullTableName();
		execSQL(con,sql);
		
		//Creates the source table
		sql = "CREATE TABLE " + getFullTableName() + " ("+
					"\n ID NUMERIC NOT NULL PRIMARY KEY," +
					"\n COL_STRING VARCHAR(20) NULL," +
					"\n COL_DATE DATETIME NULL," +
					"\n COL_NUMBER NUMERIC NULL)";
		execSQL(con,sql);
		String testString = "ABCDE";
		for (int i = 0; i < 5; i++) {
			sql = "INSERT INTO " + getFullTableName() + " VALUES(" +
				i + ", " +
				SQL.quote(testString.charAt(i)) + ", " +
				SQL.escapeDateTime(con, new Date((long) i*1000*60*60*24)) + ", " +
				i + ")";
			execSQL(con,sql);
		}
        sql = "INSERT INTO " + getFullTableName() + " (ID) VALUES(5)";
        execSQL(con,sql);
        match.setSourceTable(db.getTableByName("MM_TEST", "MM_TEST", 
        	"MERGE_PROCESSOR_TEST"));
        match.setSourceTableIndex(db.getTableByName("MM_TEST", "MM_TEST", 
			"MERGE_PROCESSOR_TEST").getPrimaryKeyIndex());

        //Creates the result Table
        DDLGenerator ddlg = null;
    	try {
    		ddlg = DDLUtils.createDDLGenerator(ds);
    	} catch (ClassNotFoundException e) {
    		fail("DDLUtils.createDDLGenerator(SPDataSource) threw a ClassNotFoundException");
    	}
    	assertNotNull("DDLGenerator error", ddlg);
		ddlg.setTargetSchema(ds.getPlSchema());
		match.setResultTableName("MERGE_PROCESSOR_TEST_RESULT");
		match.setResultTableSchema("MM_TEST");
		match.setResultTableCatalog("MM_TEST");
		
		if (Match.doesResultTableExist(session, match)) {
			ddlg.dropTable(match.getResultTable());
		}
		ddlg.addTable(match.createResultTable());
		ddlg.addIndex((SQLIndex) match.getResultTable().getIndicesFolder().getChild(0));
		
	    for (DDLStatement sqlStatement : ddlg.getDdlStatements()) {
	    	sql = sqlStatement.getSQLText();
	    	execSQL(con,sql);
	    }
	    sql = "INSERT INTO " + getFullTableName() + "_RESULT " +
	    	"(DUP_CANDIDATE_10, DUP_CANDIDATE_20, MATCH_PERCENT, MATCH_STATUS, DUP1_MASTER_IND, GROUP_ID)" +
	    	"VALUES " + 
	    	"(1,5,1,'AUTO_MATCH','N', 'test')";
	    execSQL(con,sql);
	    sql = "INSERT INTO " + getFullTableName() + "_RESULT " +
	    	"(DUP_CANDIDATE_10, DUP_CANDIDATE_20, MATCH_PERCENT, MATCH_STATUS, DUP1_MASTER_IND, GROUP_ID)" +
	    	"VALUES " + 
	    	"(2,3,1,'MATCH','Y', 'test')";
	    execSQL(con,sql);
	    sql = "INSERT INTO " + getFullTableName() + "_RESULT " +
	    	"(DUP_CANDIDATE_10, DUP_CANDIDATE_20, MATCH_PERCENT, MATCH_STATUS, DUP1_MASTER_IND, GROUP_ID)" +
	    	"VALUES " + 
	    	"(4,3,1,'UNMATCH','', 'test')";
	    execSQL(con,sql);
	    
	    cmr_string.setColumn(match.getSourceTable().getColumnByName("COL_STRING"));   	
		cmr_date.setColumn(match.getSourceTable().getColumnByName("COL_DATE"));
		cmr_number.setColumn(match.getSourceTable().getColumnByName("COL_NUMBER"));
		
		tmr.setTable(match.getSourceTable());
		
		mpor = new MergeProcessor(match, session);
	}
	
	@Override
	protected String getFullTableName() {
		return "MM_TEST.MM_TEST.MERGE_PROCESSOR_TEST";
	}
}
