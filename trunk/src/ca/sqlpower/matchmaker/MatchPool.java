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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import ca.sqlpower.architect.ArchitectException;
import ca.sqlpower.architect.SQLColumn;
import ca.sqlpower.architect.SQLIndex;
import ca.sqlpower.architect.SQLTable;
import ca.sqlpower.architect.ddl.DDLUtils;
import ca.sqlpower.graph.BreadthFirstSearch;
import ca.sqlpower.graph.DijkstrasAlgorithm;
import ca.sqlpower.graph.GraphModel;
import ca.sqlpower.matchmaker.PotentialMatchRecord.MatchType;
import ca.sqlpower.matchmaker.PotentialMatchRecord.StoreState;
import ca.sqlpower.matchmaker.dao.MatchMakerDAO;
import ca.sqlpower.matchmaker.graph.GraphConsideringOnlyGivenNodes;
import ca.sqlpower.matchmaker.graph.NonDirectedUserValidatedMatchPoolGraphModel;
import ca.sqlpower.matchmaker.munge.MungeProcess;
import ca.sqlpower.sql.SQL;

/**
 * The MatchPool class represents the set of matching records for
 * a particular Match instance.  Taken together, it is a graph of
 * matching (and potentially matching) source table records, with
 * the edges between those records represented by the list of
 * PotentialMatchRecords.
 */
public class MatchPool {
    
    private static final Logger logger = Logger.getLogger(MatchPool.class);
    
    private final Match match;
    
    private final MatchMakerSession session;
    
    /**
     * The edge list for this graph.
     */
    private final Set<PotentialMatchRecord> potentialMatches;

    /**
     * A map of keys to node instances for this graph.  The values() set of
     * this map is the node set for the graph.
     */
    private final Map<List<Object>, SourceTableRecord> sourceTableRecords =
        new HashMap<List<Object>, SourceTableRecord>();

    /**
     * This is the list of potential match records that are still waiting to be deleted.
     */
    private final Set<PotentialMatchRecord> deletedMatches = new HashSet<PotentialMatchRecord>();
    
    public MatchPool(Match match) {
        this(match, new HashSet<PotentialMatchRecord>());
    }
    
    public MatchPool(Match match, Set<PotentialMatchRecord> potentialMatches) {
        this.match = match;
        this.session = match.getSession();
        this.potentialMatches = potentialMatches;
    }

    public Match getMatch() {
        return match;
    }
   
    /**
     * Finds all the potential match record (edges in the graph) that belongs to the
     * particular match group
     * @param matchGroupName
     * @return a list of potential match records that belong to the match critieria group
     */
    public List<PotentialMatchRecord> getAllPotentialMatchByMatchRuleSet
                        (String matchGroupName) {
        List<PotentialMatchRecord> matchList =
            new ArrayList<PotentialMatchRecord>();
        for (PotentialMatchRecord pmr : potentialMatches){
            if (pmr.getRuleSet().getName().equals(matchGroupName)){
                matchList.add(pmr);
            }
        }
        return matchList;
    }
          
    /**
     * Finds all the potential match record (edges in the graph) that belongs to the
     * particular match group
     * @param ruleSet
     * @return a list of potential match records that belong to the match critieria group
     */
    public List<PotentialMatchRecord> getAllPotentialMatchByMatchRuleSet(MungeProcess ruleSet) {
        List<PotentialMatchRecord> matchList =
            new ArrayList<PotentialMatchRecord>();
        for (PotentialMatchRecord pmr : potentialMatches){
            if (pmr.getRuleSet() == ruleSet){
                matchList.add(pmr);
            }
        }
        return matchList;
    }
    
    /**
	 * This removes all of the potential matches in the given match group from
	 * the pool ONLY. This does not remove the potential matches from the nodes
	 * it connects. Nor does this modify the database.
	 */
    public void removePotentialMatchesInMatchGroup(String groupName){
        potentialMatches.removeAll(getAllPotentialMatchByMatchRuleSet(groupName));        
    }
    
    /**
     * Executes SQL statements to initialize nodes {@link SourceTableRecord} and 
     * edges {@link PotentialMatchRecord}. It is also used to update the display
     * column values for the SourceTableRecord, which is done by passing in a 
     * List of SQLColumns which represent the columns that you wish to display
     * <p>
     * IMPORTANT NOTE ABOUT SIDE EFFECTS: before searching the table, this method will
     * attempt to remove redundant records from the match result table.  Its name implies
     * that it only reads the database.  This is not the case.  For details, see
     * {@link #deleteRedundantMatchRecords()}.
     * 
     * @param displayColumns A list of which SQLColumns to use to represent a SourceTableRecord
     * in the user interface. If null or empty, then the default will be the SourceTableRecord's 
     * primary key.
     * 
     * @throws SQLException if an unexpected error occurred running the SQL statements
     * @throws ArchitectException if SQLObjects fail to populate its children
     */
    public void findAll(List<SQLColumn> displayColumns) throws SQLException, ArchitectException {
        
        deleteRedundantMatchRecords();
        
        SQLTable resultTable = match.getResultTable();
        Connection con = null;
        Statement stmt = null;
        ResultSet rs = null;
        String lastSQL = null;
        try {
            con = session.getConnection();
            stmt = con.createStatement();
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT ");
            boolean first = true;
            for (SQLColumn col : resultTable.getColumns()) {
                if (!first) sql.append(",\n");
                sql.append(" result.");
                sql.append(col.getName());
                first = false;
            }
            if (displayColumns == null || displayColumns.size() == 0) {
            	displayColumns = new ArrayList<SQLColumn>();
            	for (SQLIndex.Column col: (List<SQLIndex.Column>)match.getSourceTableIndex().getChildren()) {
            		displayColumns.add(col.getColumn());
            	}
            }
            for (SQLColumn col : displayColumns) {
            	sql.append(",\n");
            	sql.append(" source1.");
            	sql.append(col.getName());
            	sql.append(" as disp1" + displayColumns.indexOf(col));
            	
            	sql.append(",\n");
            	sql.append(" source2.");
            	sql.append(col.getName());
            	sql.append(" as disp2" + displayColumns.indexOf(col));
            }
            
            sql.append("\n FROM ");
            sql.append(DDLUtils.toQualifiedName(resultTable));
            sql.append(" result,");
            SQLTable sourceTable = match.getSourceTable();
			sql.append(DDLUtils.toQualifiedName(sourceTable));
            sql.append(" source1,");
            sql.append(DDLUtils.toQualifiedName(sourceTable));
            sql.append(" source2");
            int index = 0;
            for (SQLIndex.Column col: (List<SQLIndex.Column>)match.getSourceTableIndex().getChildren()) {
            	if (index == 0) { 
            		sql.append("\n WHERE");
            	} else {
            		sql.append(" AND");
            	}
            	sql.append(" result.");
            	sql.append("DUP_CANDIDATE_1"+index);
            	sql.append("=");
            	sql.append("source1." + col.getColumn().getName());
            	sql.append(" AND");
            	sql.append(" result.");
            	sql.append("DUP_CANDIDATE_2"+index);
            	sql.append("=");
            	sql.append("source2." + col.getColumn().getName());
            	index++;
            }
            
            lastSQL = sql.toString();
            logger.debug("MatchPool's findAll method SQL: \n" + lastSQL);
            rs = stmt.executeQuery(lastSQL);
            while (rs.next()) {
                MungeProcess ruleSet = match.getMatchRuleSetByName(rs.getString("GROUP_ID"));
                if (ruleSet == null) {
                    session.handleWarning(
                            "Found a match record that refers to the " +
                            "non-existant rule set \""+rs.getString("GROUP_ID")+
                            "\". Ignoring it.");
                    continue;
                }
                String statusCode = rs.getString("MATCH_STATUS");
                MatchType matchStatus = MatchType.typeForCode(statusCode);
                if (statusCode != null && matchStatus == null) {
                    session.handleWarning(
                            "Found a match record with the " +
                            "unknown/invalid match status \""+statusCode+
                            "\". Ignoring it.");
                    continue;
                }
                int indexSize = match.getSourceTableIndex().getChildCount();
                List<Object> lhsKeyValues = new ArrayList<Object>(indexSize);
                List<Object> rhsKeyValues = new ArrayList<Object>(indexSize);
                for (int i = 0; i < indexSize; i++) {
                    lhsKeyValues.add(rs.getObject("DUP_CANDIDATE_1"+i));
                    rhsKeyValues.add(rs.getObject("DUP_CANDIDATE_2"+i));
                }
                List<Object> lhsDisplayValues = new ArrayList<Object>();
                List<Object> rhsDisplayValues = new ArrayList<Object>();
                for (int i = 0; i < displayColumns.size(); i++) {
                    lhsDisplayValues.add(rs.getObject("disp1"+i));
                    rhsDisplayValues.add(rs.getObject("disp2"+i));
                }
                SourceTableRecord lhs = makeSourceTableRecord(lhsDisplayValues, lhsKeyValues);
                SourceTableRecord rhs = makeSourceTableRecord(rhsDisplayValues, rhsKeyValues);
                PotentialMatchRecord pmr =
                	new PotentialMatchRecord(ruleSet, matchStatus, lhs, rhs, false);
                if (matchStatus == MatchType.MATCH || matchStatus == MatchType.AUTOMATCH) {
                	if (SQL.decodeInd(rs.getString("DUP1_MASTER_IND"))) {
                		pmr.setMaster(lhs);
                	} else {
                		pmr.setMaster(rhs);
                	}
                }
                if (pmr.getMatchStatus() == null) {
                	pmr.setMatchStatus(MatchType.UNMATCH);
                }
                pmr.setStoreState(StoreState.CLEAN);
                addPotentialMatch(pmr);
                logger.debug("Number of PotentialMatchRecords is now" + potentialMatches.size());
            }
            
        } catch (SQLException ex) {
            logger.error("Error in query: "+lastSQL, ex);
            session.handleWarning(
                    "Error in SQL Query!" +
                    "\nMessage: "+ex.getMessage() +
                    "\nSQL State: "+ex.getSQLState() +
                    "\nQuery: "+lastSQL);
            throw ex;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ex) { logger.error("Couldn't close result set", ex); }
            if (stmt != null) try { stmt.close(); } catch (SQLException ex) { logger.error("Couldn't close statement", ex); }
            if (con != null) try { con.close(); } catch (SQLException ex) { logger.error("Couldn't close connection", ex); }
        }

    }
    
    /**
	 * This algorithm stores and updates all of the potential match records
	 * in the database. If the potential match record is dirty it will be updated.
	 * If the potential match record is new it will be added to the database.
	 * If a potential match record is in the deletedMatches set it will be deleted
	 * from the database. If the record is clean it will not be modified in any way.
	 */
    public void store() throws SQLException {
        logger.debug("Starting to store");
        SQLTable resultTable = match.getResultTable();
        Connection con = null;
        String lastSQL = null;
        PreparedStatement ps = null;
        if (sourceTableRecords.size() == 0) return;
        int numKeyValues = ((SourceTableRecord)sourceTableRecords.values().toArray()[0]).getKeyValues().size();
        try {
            con = session.getConnection();
            StringBuilder sql = new StringBuilder();
            sql.append("UPDATE ");
            sql.append(DDLUtils.toQualifiedName(resultTable)); 
            sql.append("\n SET ");
            sql.append("MATCH_STATUS=?");
            sql.append(", MATCH_STATUS_DATE=" + SQL.escapeDateTime(con, new Date(System.currentTimeMillis())));
            sql.append(", MATCH_STATUS_USER=" + SQL.quote(session.getAppUser()));
            sql.append(", DUP1_MASTER_IND=? ");
            sql.append("\n WHERE ");
            for (int i = 0;; i++) {
            	sql.append("DUP_CANDIDATE_1" + i + "=?");
            	sql.append(" AND DUP_CANDIDATE_2" + i + "=?");
            	if (i + 1 >= numKeyValues) break;
            	sql.append(" AND ");
            }
            lastSQL = sql.toString();
            logger.debug("The SQL statement we are running is " + lastSQL);
            
            ps = con.prepareStatement(lastSQL);
            
            for (PotentialMatchRecord pmr : potentialMatches) {
            	if (pmr.getStoreState() == StoreState.DIRTY) {
            		logger.debug("The potential match " + pmr + " was dirty, storing");
            		ps.setObject(1, pmr.getMatchStatus().getCode());
            		if (pmr.isLhsMaster()) {
            			ps.setObject(2, "Y");
                    } else if (pmr.isRhsMaster()) {
                    	ps.setObject(2, "N");
                    } else {
                    	ps.setNull(2, Types.VARCHAR);
                    }
            		for (int i = 0; i < pmr.getOriginalLhs().getKeyValues().size(); i++) {
            			ps.setObject(i * 2 + 3, pmr.getOriginalLhs().getKeyValues().get(i));
            			ps.setObject(i * 2 + 4, pmr.getOriginalRhs().getKeyValues().get(i));
            		}
            		ps.executeUpdate();
            		pmr.setStoreState(StoreState.CLEAN);
            	}
            }
            
            sql = new StringBuilder();
            sql.append("INSERT INTO ").append(DDLUtils.toQualifiedName(resultTable)).append(" ");
            sql.append("(");
            for (int i = 0; i < numKeyValues; i++) {
            	sql.append("DUP_CANDIDATE_1").append(i).append(", ");
            	sql.append("DUP_CANDIDATE_2").append(i).append(", ");
            }
            sql.append("MATCH_PERCENT");
            sql.append(", GROUP_ID");
            sql.append(", MATCH_STATUS");
            sql.append(", DUP1_MASTER_IND");
            sql.append(", MATCH_DATE");
            sql.append(", MATCH_STATUS_DATE");
            sql.append(", MATCH_STATUS_USER");
            
            //These fields are only used for the old merge engine and will be removed when
            //the merging is rewritten in Java
            for (int i = 0; i < numKeyValues; i++) {
            	sql.append(", DUP_ID").append(i);
            	sql.append(", MASTER_ID").append(i);
            }
            
            sql.append(")");
            sql.append("\n VALUES (");
            
            for (int i = 0; i < numKeyValues * 2; i++) {
            	sql.append("?, ");
            }
            sql.append("?, ");
            sql.append("?, ");
            sql.append("?, ");
            sql.append("?, ");
            sql.append(SQL.escapeDateTime(con, new Date(System.currentTimeMillis()))).append(", ");
            sql.append(SQL.escapeDateTime(con, new Date(System.currentTimeMillis()))).append(", ");
            sql.append(SQL.quote(session.getAppUser()));
            for (int i = 0; i < numKeyValues * 2; i++) {
            	sql.append(", ?");
            }
            sql.append(")");
            lastSQL = sql.toString();
            logger.debug("The SQL statement we are running is " + lastSQL);
            
            ps = con.prepareStatement(lastSQL);
            
            for (PotentialMatchRecord pmr : potentialMatches) {
            	if (pmr.getStoreState() == StoreState.NEW) {
            		logger.debug("The potential match " + pmr + " was new, storing");
            		for (int i = 0; i < numKeyValues; i++) {
            			ps.setObject(i * 2 + 1, pmr.getOriginalLhs().getKeyValues().get(i));
            			ps.setObject(i * 2 + 2, pmr.getOriginalRhs().getKeyValues().get(i));
            		}
            		ps.setObject(numKeyValues * 2 + 1, pmr.getRuleSet().getMatchPercent());
            		ps.setObject(numKeyValues * 2 + 2, pmr.getRuleSet().getName());
            		ps.setObject(numKeyValues * 2 + 3, pmr.getMatchStatus().getCode());
            		
            		SourceTableRecord duplicate;
            		SourceTableRecord master;
            		if (pmr.isLhsMaster()) {
            			ps.setObject(numKeyValues * 2 + 4, "Y");
            			duplicate = pmr.getOriginalRhs();
            			master = pmr.getOriginalLhs();
                    } else if (pmr.isRhsMaster()) {
                    	ps.setObject(numKeyValues * 2 + 4, "N");
                    	duplicate = pmr.getOriginalLhs();
            			master = pmr.getOriginalRhs();
                    } else {
                    	ps.setObject(numKeyValues * 2 + 4, null);
                    	duplicate = null;
            			master = null;
                    }
            		
            		//These fields are only used for the old merge engine and will be removed when
                    //the merging is rewritten in Java
            		if (duplicate != null && master != null) {
            			for (int i = 0; i < numKeyValues; i++) {
            				int baseParamIndex = (numKeyValues + i) * 2;
							ps.setObject(baseParamIndex + 5, duplicate.getKeyValues().get(i));
            				ps.setObject(baseParamIndex + 6, master.getKeyValues().get(i));
            			}
            		}
            		
            		ps.executeUpdate();
            		pmr.setStoreState(StoreState.CLEAN);
            	}
            }
            
            sql = new StringBuilder();
            sql.append("DELETE FROM ").append(DDLUtils.toQualifiedName(resultTable));
            sql.append("\n WHERE ");
            for (int i = 0;; i++) {
            	sql.append("DUP_CANDIDATE_1" + i + "=?");
            	sql.append(" AND DUP_CANDIDATE_2" + i + "=?");
            	if (i + 1 >= numKeyValues) break;
            	sql.append(" AND ");
            }
            lastSQL = sql.toString();
            logger.debug("The SQL statement we are running is " + lastSQL);
            
            ps = con.prepareStatement(lastSQL);
            
            for (Iterator<PotentialMatchRecord> it = deletedMatches.iterator(); it.hasNext(); ) {
            	PotentialMatchRecord pmr = it.next();
            	logger.debug("Dropping " + pmr + " from the database.");
            	for (int i = 0; i < numKeyValues; i++) {
            		ps.setObject(i * 2 + 1, pmr.getOriginalLhs().getKeyValues().get(i));
            		ps.setObject(i * 2 + 2, pmr.getOriginalRhs().getKeyValues().get(i));
            	}
            	ps.executeUpdate();
            	it.remove();
            }
            
        } catch (SQLException ex) {
            logger.error("Error in query: "+lastSQL, ex);
            session.handleWarning(
                    "Error in SQL Query!" +
                    "\nMessage: "+ex.getMessage() +
                    "\nSQL State: "+ex.getSQLState() +
                    "\nQuery: "+lastSQL);
            throw ex;
        } finally {
            if (ps != null) try { ps.close(); } catch (SQLException ex) { logger.error("Couldn't close prepared statement", ex); }
            if (con != null) try { con.close(); } catch (SQLException ex) { logger.error("Couldn't close connection", ex); }
        }

    }
    
    /**
     * Attempts to look up the existing SourceTableRecord instance in
     * the cache, but makes a new one and puts it in the cache if not found.
     * 
     * @param diplayValues The values used to display this record in the UI
     * @param keyValues The values for this record's unique index
     * @return The source table record that corresponds with the given key values.
     * The return value is never null.
     */
    private SourceTableRecord makeSourceTableRecord(List<Object> displayValues, List<Object> keyValues) {
        SourceTableRecord node = sourceTableRecords.get(keyValues);
        if (node == null) {
            node = new SourceTableRecord(session, match, displayValues, keyValues);
            addSourceTableRecord(node);
        } else {
        	node.setDisplayValues(displayValues);
        }
        return node;
    }
    
    /**
     * Adds the given source table record to this match pool.  This is normally only
     * done from the test suite, which sets up various scenarios to test.  In real life,
     * source table records get added to the match pool on demand, and they come directly
     * from SQL SELECTs on the source table.  See {@link #findAll()} for details.
     * 
     * @param str The record to add. Its parent pool will be modified to point to
     * this pool.
     */
    public void addSourceTableRecord(SourceTableRecord str) {
    	str.setPool(this);
        sourceTableRecords.put(str.getKeyValues(), str);
    }

    /**
     * Adds the given potential match to this pool.  This is normally only
     * done from the test suite, which sets up various scenarios to test.  In real life,
     * source table records get added to the match pool via a
     * SQL SELECT on the match result table.  See {@link #findAll()} for details.
     * 
     * @param pmr The record to add
     */
    public void addPotentialMatch(PotentialMatchRecord pmr) {
    	pmr.setPool(this);
    	potentialMatches.add(pmr);
    	pmr.getOriginalLhs().addPotentialMatch(pmr);
        pmr.getOriginalRhs().addPotentialMatch(pmr);
        if (pmr.getMatchStatus() == null) {
        	pmr.setMatchStatus(MatchType.UNMATCH);
        }
    }
    
    /**
     * Returns the set of PotentialMatchRecords in this match pool.  
     * Before calling this, you should populate the pool by calling
     * one of the findXXX() methods.
     * <p>
     * Potential Match records are the edges of this graph of matching records.
     * For the nodes, see {@link #getSourceTableRecords()}.
     * 
     * @return The current list of potential match records.
     */
    public Set<PotentialMatchRecord> getPotentialMatches() {
        return potentialMatches;
    }
    
    /**
	 * Gets the first PotentialMatchRecord in the pool that has the given nodes
	 * as its original left and right nodes. Null is returned if no PotentialMatchRecord
	 * is found.
	 */
    public PotentialMatchRecord getPotentialMatchFromOriginals(SourceTableRecord node1, SourceTableRecord node2) {
    	for (PotentialMatchRecord pmr : node1.getOriginalMatchEdges()) {
    		if (pmr.getOriginalLhs() == node1 && pmr.getOriginalRhs() == node2 
    				|| pmr.getOriginalLhs() == node2 && pmr.getOriginalRhs() == node1) {
    			return pmr;
    		}
    	}
    	return null;
    }
    
    public Collection<SourceTableRecord> getSourceTableRecords() {
        return Collections.unmodifiableCollection(sourceTableRecords.values());
    }
    
    /**
     * For historical reasons, the match engine populates the result table
     * with two copies of each match record: one a-b, and one b-a.  We don't
     * want to deal with this duplication (isn't this a de-duping tool?), so
     * this method is designed to de-dupe the de-duping table.
     * @throws SQLException if there is a problem executing the DELETE statement
     * @throws ArchitectException if the source table index can't be populated
     */
    private void deleteRedundantMatchRecords() throws SQLException, ArchitectException {
        Connection con = null;
        Statement stmt = null;
        ResultSet rs = null;
        String lastSQL = null;
        try {
            con = session.getConnection();
            stmt = con.createStatement();
            StringBuilder sql = new StringBuilder();

            sql.append("DELETE FROM ").append(DDLUtils.toQualifiedName(match.getResultTable())).append(" M1");
            sql.append("\n WHERE EXISTS( SELECT 1 FROM ").append(DDLUtils.toQualifiedName(match.getResultTable())).append(" M2");
            sql.append("\n  WHERE ");
            for (int i = 0; i < match.getSourceTableIndex().getChildCount(); i++) {
                if (i > 0) sql.append("\n   AND ");
                sql.append("M1.DUP_CANDIDATE_1").append(i).append(" = M2.DUP_CANDIDATE_2").append(i);
                sql.append("\n AND ");
                sql.append("M1.DUP_CANDIDATE_2").append(i).append(" = M2.DUP_CANDIDATE_1").append(i);
                sql.append("\n AND ");
                sql.append("M1.DUP_CANDIDATE_1").append(i).append(" < M2.DUP_CANDIDATE_1").append(i);
            }
            sql.append(")");
            
            lastSQL = sql.toString();
            stmt.executeUpdate(lastSQL);
            
            con.commit();
            
        } catch (SQLException ex) {
            logger.error("Error in query: "+lastSQL, ex);
            session.handleWarning(
                    "Error in SQL Statement!" +
                    "\nMessage: "+ex.getMessage() +
                    "\nSQL State: "+ex.getSQLState() +
                    "\nQuery: "+lastSQL);
            throw ex;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ex) { logger.error("Couldn't close result set", ex); }
            if (stmt != null) try { stmt.close(); } catch (SQLException ex) { logger.error("Couldn't close statement", ex); }
            if (con != null) try { con.close(); } catch (SQLException ex) { logger.error("Couldn't close connection", ex); }
        }

    }
    
    public SourceTableRecord getSourceTableRecord(List<? extends Object> key) {
    	return sourceTableRecords.get(key);
    }

    /**
	 * Locates all records which are currently reachable from this record and
	 * the given (formerly potential) duplicate of it by user-validated matches,
	 * and points them to this record as the master (all the reachable records
	 * will be considered duplicates of this "offical version of the truth").
	 * All nodes in the decided edges of the duplicate will have their master
	 * set to the duplicate, or a duplicate of the duplicate, etc.
	 * 
	 * The graph used in this method should not have cycles in it as our output
	 * is guaranteed to not have cycles.
	 * 
	 * This method is done in several steps.
	 * <ol>
	 * <li>Set the edge between the duplicate and master to be undecided or
	 * unmatched if the match exists. This is for the case that the duplicate
	 * was originally the master and we don't want to follow this path when
	 * looking for the ultimate master.</li>
	 * <li>Find a graph of all the nodes reachable from the master and
	 * duplicate along decided edges. The graph will include only these nodes
	 * and only the edges connecting these nodes, whether they are decided or
	 * not.</li>
	 * <li>Find the ultimate master, the master of the chain of nodes that has
	 * no master itself, from the master node. Note: If there is a cycle in the
	 * graph then the node decided to be the ultimate master may be surprising
	 * for the user as it may not be the most obvious one but we guarantee that
	 * there will be no cycles in the end result.</li>
	 * <li>Using Dijkstra's algorithm, using all edges in the graph, find the
	 * shortest path to all nodes in the graph. If the duplicate cannot be
	 * reached at this point then a synthetic edge is needed between the
	 * duplicate and the master and Dijkstra's algorithm needs to be run again.</li>
	 * <li>Turn all of the edges walked using Dijkstra's algorithm into decided
	 * edges pointing in the direction of the ultimate master. All other edges
	 * will be undecided (or UNMATCH).</li>
	 * </ol>
	 * <p>
	 * Additionally, there is an isAutoMatch boolean flag that should be used when
	 * the method is being called by the AutoMatch feature. In this case, all of the
	 * PotentialMatchRecords will have their match status set to AUTOMATCH, and the
	 * respective records in the match result table will also be updated as AUTOMATCH 
	 * @param master The SourceTableRecord that we are defining as the master
	 * @param duplicate The SourceTableRecord that we are defining as a duplicate of the master
	 * @param isAutoMatch Indicate that this method is being used by the AutoMatch feature
	 */
    public void defineMaster(SourceTableRecord master, SourceTableRecord duplicate, boolean isAutoMatch) throws ArchitectException {
    	if (duplicate == master) {
    		defineMasterOfAll(master);
    		return;
    	}

    	logger.debug("DefineMaster: master="+master+"; duplicate="+duplicate);
    	
    	logger.debug("Remove the master from the edge between the master and duplicate if the master exists.");
    	PotentialMatchRecord potentialMatch = getPotentialMatchFromOriginals(master, duplicate);
    	if (potentialMatch != null) {
    		potentialMatch.setMaster(null);
    	}

    	logger.debug("Create the graph that contains only nodes reachable by decided edges");
    	GraphModel<SourceTableRecord, PotentialMatchRecord> nonDirectedGraph =
    		new NonDirectedUserValidatedMatchPoolGraphModel(this, new HashSet<PotentialMatchRecord>());
    	BreadthFirstSearch<SourceTableRecord, PotentialMatchRecord> bfs =
            new BreadthFirstSearch<SourceTableRecord, PotentialMatchRecord>();
        Set<SourceTableRecord> reachable = new HashSet<SourceTableRecord>(bfs.performSearch(nonDirectedGraph, master));
        reachable.addAll(bfs.performSearch(nonDirectedGraph, duplicate));

        GraphModel<SourceTableRecord, PotentialMatchRecord> considerGivenNodesGraph =
        	new GraphConsideringOnlyGivenNodes(this, reachable);
        logger.debug("Graph contains " + considerGivenNodesGraph.getNodes() + " nodes.");

        logger.debug("Find the ultimate master");
        SourceTableRecord ultimateMaster = findUltimateMaster(considerGivenNodesGraph,
				master, new ArrayList<SourceTableRecord>());
        
        if (ultimateMaster == duplicate) {
        	ultimateMaster = master;
        }
        
        logger.debug("Find the shortest path to all nodes in the graph");
    	DijkstrasAlgorithm da = new DijkstrasAlgorithm<SourceTableRecord, PotentialMatchRecord>();
    	Map<SourceTableRecord, SourceTableRecord> masterMapping = da.calculateShortestPaths(considerGivenNodesGraph, ultimateMaster);
    	
    	if (masterMapping.get(duplicate) == null) {
    		logger.debug("We could not reach the duplicate from the ultimate master in the current graph. A synthetic edge will be created");
    		addSyntheticPotentialMatchRecord(master, duplicate);
    		masterMapping = da.calculateShortestPaths(considerGivenNodesGraph, ultimateMaster);
    	}
    	
    	defineMatchEdges(considerGivenNodesGraph, masterMapping, isAutoMatch);
	}
    
    /**
     * Similar to {@link #defineMaster(SourceTableRecord, SourceTableRecord, boolean)} except the isAutoMatch
     * boolean flag is set to false by default. DO NOT use this version if you are performing an AutoMatch!
     * @throws ArchitectException
     */
    public void defineMaster(SourceTableRecord master, SourceTableRecord duplicate) throws ArchitectException {
    	defineMaster(master, duplicate, false);
    }

    /**
	 * This method adds a match rule set to the match in this pool
	 * for synthetic edges if the rule set does not already exist.
	 * IMPORTANT NOTE: In the case that the new match group for synthetic edges
	 * had to be created, this pool's match object will be saved using the current
	 * Match DAO from the session.  This is a bit of a strange side effect of this
	 * method, so be careful!
	 * <p>
	 * Once the match group for synthetic edges has been located or created, a
	 * new potential match record that is synthetic (created by the Match Maker)
	 * is added to the pool. This edge (which is a potential match record) belongs
	 * to the special synthetic edges rule set. The match type
	 * of the new potential match record is set to UNMATCH by default.
	 * <p>
	 * This new potential match record will be stored back to the match result table
	 * in the database next time you call the {@link #store()} method on this pool.
	 * 
	 * @param record1
	 *            One of the source table records attached to the new potential
	 *            match record.
	 * @param record
	 *            2 One of the source table records attached to the new
	 *            potential match record.
	 * @return The new potential match record that was added to the pool.
	 */
	private PotentialMatchRecord addSyntheticPotentialMatchRecord(SourceTableRecord record1,
			SourceTableRecord record2) {
		MungeProcess syntheticRuleSet = match.getMatchRuleSetByName(MungeProcess.SYNTHETIC_MATCHES);
		if (syntheticRuleSet == null) {
			syntheticRuleSet = new MungeProcess();
			syntheticRuleSet.setName(MungeProcess.SYNTHETIC_MATCHES);
			match.getMatchRuleSetFolder().addChild(syntheticRuleSet);
			MatchMakerDAO<Match> dao = session.getDAO(Match.class);
			dao.save(match);
		}
		
		PotentialMatchRecord pmr = new PotentialMatchRecord(syntheticRuleSet, MatchType.UNMATCH, record1, record2, true);
		addPotentialMatch(pmr);
		
		return pmr;
	}
	
    /**
	 * This method defines the given node to be the master of all nodes
	 * reachable by either a defined or undefined path. We use Dijkstra's
	 * algorithm to find the shortest path to the nodes and to make sure
	 * that we have no cycles.
	 */
	public void defineMasterOfAll(SourceTableRecord master) throws ArchitectException {
		GraphModel<SourceTableRecord, PotentialMatchRecord> nonDirectedGraph =
    		new NonDirectedUserValidatedMatchPoolGraphModel(this, new HashSet<PotentialMatchRecord>());
		BreadthFirstSearch<SourceTableRecord, PotentialMatchRecord> bfs =
            new BreadthFirstSearch<SourceTableRecord, PotentialMatchRecord>();
        Set<SourceTableRecord> reachable = new HashSet<SourceTableRecord>(bfs.performSearch(nonDirectedGraph, master));
        Set<SourceTableRecord> noMatchNodes = findNoMatchNodes(reachable);
        
        for (PotentialMatchRecord pmr : master.getOriginalMatchEdges()) {
        	if (pmr.getMatchStatus() == MatchType.UNMATCH) {
        		logger.debug("Looking at record " + pmr);
        		SourceTableRecord str;
        		if (pmr.getOriginalLhs() == master) {
        			str = pmr.getOriginalRhs();
        		} else {
        			str = pmr.getOriginalLhs();
        		}
        		if (noMatchNodes.contains(str)) continue;
        		logger.debug("Adding " + str + " to reachable nodes");
        		GraphModel<SourceTableRecord, PotentialMatchRecord> newNonDirectedGraph =
            		new NonDirectedUserValidatedMatchPoolGraphModel(this, new HashSet<PotentialMatchRecord>());
                Set<SourceTableRecord> newReachableNodes = new HashSet<SourceTableRecord>(bfs.performSearch(newNonDirectedGraph, str));
                reachable.addAll(newReachableNodes);
        		noMatchNodes.addAll(findNoMatchNodes(newReachableNodes));
        	}
        }
        
        
        GraphModel<SourceTableRecord, PotentialMatchRecord> considerGivenNodesGraph =
        	new GraphConsideringOnlyGivenNodes(this, reachable);
        
        DijkstrasAlgorithm da = new DijkstrasAlgorithm<SourceTableRecord, PotentialMatchRecord>();
    	Map<SourceTableRecord, SourceTableRecord> masterMapping = da.calculateShortestPaths(considerGivenNodesGraph, master);
    	
    	defineMatchEdges(considerGivenNodesGraph, masterMapping);
	}
	
	/**
	 * This method finds all of the source table records that are connected to
	 * the given node by a no match edge directly and all source table records
	 * that are connected to a different node by a no match edge where the
	 * different node is connected to the given node by matched edges.
	 */
	private Set<SourceTableRecord> findNoMatchNodes(Set<SourceTableRecord> strs) {
		Set<SourceTableRecord> noMatchNodes = new HashSet<SourceTableRecord>();
        for (SourceTableRecord reachableNode : strs) {
        	for (PotentialMatchRecord pmr : reachableNode.getOriginalMatchEdges()) {
        		if (pmr.getMatchStatus() == MatchType.NOMATCH) {
        			SourceTableRecord str;
        			if (pmr.getOriginalLhs() == reachableNode) {
        				str =pmr.getOriginalRhs();
        			} else {
        				str = pmr.getOriginalLhs();
        			}
        			GraphModel<SourceTableRecord, PotentialMatchRecord> nonDirectedGraph =
        	    		new NonDirectedUserValidatedMatchPoolGraphModel(this, new HashSet<PotentialMatchRecord>());
        			BreadthFirstSearch<SourceTableRecord, PotentialMatchRecord> bfs =
        	            new BreadthFirstSearch<SourceTableRecord, PotentialMatchRecord>();
        	        Set<SourceTableRecord> reachable = new HashSet<SourceTableRecord>(bfs.performSearch(nonDirectedGraph, str));
        	        noMatchNodes.addAll(reachable);
        		}
        	}
        }
		
		return noMatchNodes;
	}
	
	/**
	 * This sets the two given SourceTableRecords to have no match between them.
	 * If the source table records are the same all potential match records
	 * connected to the node will be set to no match. If no edge exists between
	 * the nodes than an edge will be created to store the fact that there is no
	 * match between the nodes for later use. If the records are connected
	 * somehow the lhs and rhs will be separated as in the
	 * {@link #defineUnmatched(SourceTableRecord, SourceTableRecord)} method.
	 */
    public void defineNoMatch(SourceTableRecord lhs, SourceTableRecord rhs) throws ArchitectException {
    	if (lhs == rhs) {
    		defineNoMatchOfAny(lhs);
    		return;
    	}
        PotentialMatchRecord pmr = getPotentialMatchFromOriginals(lhs, rhs);
        if (pmr != null && pmr.getMatchStatus() == MatchType.NOMATCH) {
        	return;
        } 
        
        defineUnmatched(lhs, rhs);
        if (pmr != null) {
        	pmr.setMatchStatus(MatchType.NOMATCH);
        } else {
        	addSyntheticPotentialMatchRecord(lhs, rhs).setMatchStatus(MatchType.NOMATCH);
        }
    }

    /**
	 * This method sets all of the potential match records connecting the given
	 * source table record to any other source table record to be no match.
	 */
	public void defineNoMatchOfAny(SourceTableRecord record1) throws ArchitectException {
		for (PotentialMatchRecord pmr : record1.getOriginalMatchEdges()) {
			if (pmr.getOriginalLhs() == pmr.getOriginalRhs()) continue;
			logger.debug("Setting no match between " + pmr.getOriginalLhs() + " and " + pmr.getOriginalRhs());
			if (pmr.getOriginalLhs() == record1) {
				defineNoMatch(pmr.getOriginalRhs(), pmr.getOriginalLhs());
			} else {
				defineNoMatch(pmr.getOriginalLhs(), pmr.getOriginalRhs());
			}
		}
	}
	
	/**
	 * Sets the potential match record between two source table records to be an
	 * undefined match. If the source table records are the same then the
	 * {@link #defineUnmatchAll(SourceTableRecord)} algorithm will be run
	 * instead. If no potential match record exists between the two source table
	 * records then no new potential match record will be created. If the nodes
	 * are connected by decided edges then the rhs record will be removed from
	 * the chain of matches. 
	 * <p>
	 * The algorithm below is as follows:
	 * <ol>
	 * <li>Use bfs to check if the nodes are connected by decided edges.</li>
	 * <li>If the nodes are connected make a graph of all of the connected
	 * edges.</li>
	 * <li>Find the ultimate master of all of the nodes. If the rhs record is
	 * the ultimate master then set the ultimate master to be the previous
	 * record as we will be removing the ultimate master from this chain.</li>
	 * <li>Remove the rhs record from the graph.</li>
	 * <li>Use Dijkstra to find the shortest path to all of the nodes in the
	 * graph. If there is a node that was not reached in the graph after running
	 * Dijkstra add a synthetic edge between the node and the ultimate master
	 * and run this step again.</li>
	 * <li>Set all edges walked by Dijkstra to be decided edges and all other
	 * edges to be undecided.</li>
	 * </ol>
	 */
	public void defineUnmatched(SourceTableRecord lhs, SourceTableRecord rhs) throws ArchitectException {
		logger.debug("Unmatching " + rhs + " from " + lhs);
		if (lhs == rhs) {
    		defineUnmatchAll(lhs);
    	}
		
		PotentialMatchRecord possibleNoMatchEdge = getPotentialMatchFromOriginals(lhs, rhs);
		if (possibleNoMatchEdge != null) {
			if (possibleNoMatchEdge.getMatchStatus() == MatchType.NOMATCH) {
				possibleNoMatchEdge.setMatchStatus(MatchType.UNMATCH);
				
			}
		}
		
    	GraphModel<SourceTableRecord, PotentialMatchRecord> nonDirectedGraph =
    		new NonDirectedUserValidatedMatchPoolGraphModel(this, new HashSet<PotentialMatchRecord>());
    	BreadthFirstSearch<SourceTableRecord, PotentialMatchRecord> bfs =
            new BreadthFirstSearch<SourceTableRecord, PotentialMatchRecord>();
        Set<SourceTableRecord> reachable = new HashSet<SourceTableRecord>(bfs.performSearch(nonDirectedGraph, lhs));
        if (!reachable.contains(rhs)) {
        	logger.debug("The node " + lhs + " could not reach " + rhs + " already.");
        	return;
        }

        GraphModel<SourceTableRecord, PotentialMatchRecord> considerGivenNodesGraph =
        	new GraphConsideringOnlyGivenNodes(this, reachable);
        logger.debug("Graph contains " + considerGivenNodesGraph.getNodes().size() + " nodes.");

        logger.debug("Find the ultimate master");
        SourceTableRecord ultimateMaster = findUltimateMaster(considerGivenNodesGraph,
				rhs, new ArrayList<SourceTableRecord>());
        if (rhs == ultimateMaster) {
        	List<SourceTableRecord> nodesToSkip = new ArrayList<SourceTableRecord>();
        	nodesToSkip.add(rhs);
        	ultimateMaster = findUltimateMaster(considerGivenNodesGraph, lhs, nodesToSkip);
        }
        
        reachable.remove(rhs);
        considerGivenNodesGraph = new GraphConsideringOnlyGivenNodes(this, reachable);
        for (PotentialMatchRecord pmr : rhs.getOriginalMatchEdges()) {
        	if (pmr.isMatch()) {
        		pmr.setMaster(null);
        	}
        }
        logger.debug("Graph now contains " + considerGivenNodesGraph.getNodes().size() + " nodes.");
        
        logger.debug("Find the shortest path to all nodes in the graph");
    	DijkstrasAlgorithm da = new DijkstrasAlgorithm<SourceTableRecord, PotentialMatchRecord>();
    	Map<SourceTableRecord, SourceTableRecord> masterMapping = da.calculateShortestPaths(considerGivenNodesGraph, ultimateMaster);
    	
    	for (SourceTableRecord str : reachable) {
    		if (masterMapping.get(str) == null && str != ultimateMaster) {
    			logger.debug("We could not reach " + str + " from the ultimate master in the current graph. A synthetic edge will be created");
    			addSyntheticPotentialMatchRecord(ultimateMaster, str);
    			masterMapping = da.calculateShortestPaths(considerGivenNodesGraph, ultimateMaster);
    		}
    	}
    	
    	defineMatchEdges(considerGivenNodesGraph, masterMapping);
	}

	/**
	 * This method sets the edges defined by the <duplicate, master> pairs of
	 * the map to be matched edges with the master the master node in the pair.
	 * All other edges in the graph are set to be undefined.
	 * <p>
	 * Additionally, there is an isAutoMatch boolean flag that should be used when
	 * the method is being called by the AutoMatch feature. In this case, all of the
	 * PotentialMatchRecords will have their match status set to AUTOMATCH, and the
	 * respective records in the match result table will also be updated as AUTOMATCH 
	 * 
	 * @param graph
	 *            The graph that contains the edges to modify.
	 * @param masterMapping
	 *            The mapping of all matched edges in the graph given in
	 *            <duplicate, master> pairs.
	 @param isAutoMatch Indicate that this method is being used by the AutoMatch feature
	 */
	private void defineMatchEdges(
			GraphModel<SourceTableRecord, PotentialMatchRecord> graph,
			Map<SourceTableRecord, SourceTableRecord> masterMapping,
			boolean isAutoMatch) throws ArchitectException {
		logger.debug("Removing all decided edges from the given graph");
    	for (PotentialMatchRecord pmr : graph.getEdges()) {
   			pmr.setMaster(null);
    	}
    	
    	logger.debug("Setting the new decided edges for this graph of nodes");
    	//XXX This is a fairly poor way of obtaining the potential match records. We should be able to make this faster.
    	for (Map.Entry<SourceTableRecord, SourceTableRecord> nodeMasterPair : masterMapping.entrySet()) {
    		logger.debug("Setting " + nodeMasterPair.getValue() + " to be the master of " + nodeMasterPair.getKey());
    		PotentialMatchRecord matchRecord = getPotentialMatchFromOriginals(nodeMasterPair.getValue(), nodeMasterPair.getKey());
   			matchRecord.setMaster(nodeMasterPair.getValue(), isAutoMatch);
    	}
	}
	
	/**
	 * Similar to {@link #defineMatchEdges(GraphModel, Map, boolean)} except the isAutoMatch
	 * boolean flag is set to false by default. DO NOT use this version if you are performing an AutoMatch!
	 * @throws ArchitectException
	 */
	private void defineMatchEdges(
			GraphModel<SourceTableRecord, PotentialMatchRecord> graph,
			Map<SourceTableRecord, SourceTableRecord> masterMapping) throws ArchitectException {
		defineMatchEdges(graph, masterMapping, false);
	}

	/**
	 * This method finds the ultimate master, the master with no masters, of a
	 * given node on a given graph ignoring the nodes already contained in the
	 * given list.
	 * 
	 * @param graph
	 *            The graph to traverse to find the ultimate master.
	 * @param startingPoint
	 *            The starting point to travel from to find the ultimate master.
	 * @param nodesToSkip
	 *            A starting list of nodes that will not be crossed when looking
	 *            for the ultimate master.
	 * @return The source table record that represents the ultimate master
	 * 
	 * XXX This is package private for the pre merge data fudger. Once the pre
	 * merge data fudger goes away we can make this private again.
	 */
	SourceTableRecord findUltimateMaster(
			GraphModel<SourceTableRecord, PotentialMatchRecord> graph,
			SourceTableRecord startingPoint,
			List<SourceTableRecord> nodesToSkip) {
		SourceTableRecord ultimateMaster = startingPoint;
		List<SourceTableRecord> nodesCrossed = new ArrayList<SourceTableRecord>(nodesToSkip);
		while (!graph.getOutboundEdges(ultimateMaster).isEmpty()) {
        	logger.debug("The outbound edges for " + ultimateMaster + " is " + graph.getOutboundEdges(ultimateMaster));
        	nodesCrossed.add(ultimateMaster);
        	
        	SourceTableRecord newUltimateMaster = ultimateMaster;
        	for (PotentialMatchRecord pmr : graph.getOutboundEdges(ultimateMaster)) {
        		if (pmr.getOriginalLhs() != ultimateMaster) {
        			if (!nodesCrossed.contains(pmr.getOriginalLhs())) {
        				newUltimateMaster = pmr.getOriginalLhs();
        				break;
        			}
        		} else {
        			if (!nodesCrossed.contains(pmr.getOriginalRhs())) {
        				newUltimateMaster = pmr.getOriginalRhs();
        				break;
        			}
        		}
        	}
        	if (newUltimateMaster == ultimateMaster) {
        		break;
        	}
        	ultimateMaster = newUltimateMaster;
        }
        logger.debug("The ultimate master is " + ultimateMaster);
		return ultimateMaster;
	}

	/**
	 * Sets all potential match records connected to the given source table record
	 * to be undefined matches.
	 */
	public void defineUnmatchAll(SourceTableRecord record1) throws ArchitectException {
		logger.debug("unmatching " + record1 + " from everything");
        for (PotentialMatchRecord pmr : record1.getOriginalMatchEdges()) {
        	if (pmr.getOriginalLhs() == pmr.getOriginalRhs()) continue;
        	if (pmr.getOriginalLhs() == record1) {
        		defineUnmatched(pmr.getOriginalRhs(), pmr.getOriginalLhs());
        	} else {
        		defineUnmatched(pmr.getOriginalLhs(), pmr.getOriginalRhs());
        	}
        }
	}
	
	/**
	 * This resets all of the edges in the entire pool to be unmatched. Edges
	 * that are synthetic as they were created by the MatchMaker will be
	 * removed.
	 */
	public void resetPool() {
		for (Iterator<PotentialMatchRecord> it = potentialMatches.iterator(); it.hasNext(); ) {
        	PotentialMatchRecord pmr = it.next();
			if (pmr.isSynthetic()) {
				it.remove();
				removePotentialMatch(pmr);
				continue;
			}
			if (pmr.getMatchStatus() != MatchType.UNMATCH) {
				logger.debug("Unmatching " + pmr + " for resetting the pool.");
				pmr.setMatchStatus(MatchType.UNMATCH);
			}
		}
	}

	/**
	 * Removes a potential match record from this pool and the source table
	 * records that the potential match connects. This also removes the potential
	 * match record from the database.
	 */
	public void removePotentialMatch(PotentialMatchRecord pmr) {
		if (potentialMatches.remove(pmr)) {
			pmr.getOriginalLhs().removePotentialMatch(pmr);
			pmr.getOriginalRhs().removePotentialMatch(pmr);
		}
		deletedMatches.add(pmr);
	}

	/**
	 * Performs the 'Auto-Match' function which is either for lazy people who
	 * think their rule set is perfect or for people who like to gamble.
	 * Either way, we warn them heavily in the UI that this will make mistakes.
	 * Our goal here is to make as many matches along edges in the supplied
	 * rule set without putting the graph into an illegal state or taking
	 * forever. We do not claim, nor should we, that the total number of matches
	 * created by this method is maximal or predictable.
	 * <p>
	 * NOTE: This depends on {@link #findNoMatchNodes(Set)} to return the set
	 * of nodes that should never be matched to any node in the provided set
	 * because of the current no-match and match edge configuration
	 * <p>
	 * This is the current algorithm:
	 * <ol>
	 * <li> Define a set of nodes called 'visited'. Add to it all nodes that are
	 * not incident with an edge that:
	 * <ul>
	 * <li> Is not a NoMatch edge and </li>
	 * <li> Belongs to the provided rule set </li>
	 * </ul>
	 * </li>
	 * <li> Pick a node in the graph that is not in the 'visited' set, call it
	 * 'selected' and add it to the 'visited' set. </li>
	 * <li> Find all edges incident with the selected node that are in the
	 * rule set and are not NoMatch edges. Add the nodes that are incident
	 * with the edges we just found and are not in the visited set to a list.
	 * </li>
	 * <li> Set the selected node to be the master of each node in the newly
	 * created list via the
	 * {@link #defineMaster(SourceTableRecord, SourceTableRecord)} to ensure
	 * that the graph's state is kept legal. </li>
	 * <li> For each node in the list, call one of them 'selected', add it to
	 * the visited set and go to 3 </li>
	 * </ol>
	 * 
	 * @param rule
	 *            The name of the rule along which to make matches
	 * @throws ArchitectException
	 * @throws SQLException
	 */
	public void doAutoMatch(String ruleName) throws SQLException, ArchitectException {
		MungeProcess ruleSet = match.getMatchRuleSetByName(ruleName);
		if (ruleSet == null) {
			throw new IllegalArgumentException("Auto-Match invoked with an " +
					"invalid rule group name: " + ruleName);
		}
		Collection<SourceTableRecord> records = sourceTableRecords.values();
		
		logger.debug("Auto-Matching with " + records.size() + " records.");
		
		Set<SourceTableRecord> visited = new HashSet<SourceTableRecord>();
		SourceTableRecord selected = null;
		for (SourceTableRecord record : records) {
			boolean addToVisited = true;
			for (PotentialMatchRecord pmr : record.getOriginalMatchEdges()) {
				if (pmr.getMatchStatus() != MatchType.NOMATCH
						&& pmr.getRuleSet() == ruleSet) {
					addToVisited = false;
				}
			}
			if (addToVisited) {
				visited.add(record);
			} else {
				// rather than iterating through all the records again, looking
				// for one that isn't in visited...
				selected = record;
			}
		}
		
		logger.debug("The size of visited is " + visited.size());

		Set<SourceTableRecord> neighbours = findAutoMatchNeighbours(ruleSet, selected, visited);
		makeAutoMatches(ruleSet, selected, neighbours, visited);
		//If we haven't visited all the nodes, we are not done!
		while (visited.size() != records.size()) {
			SourceTableRecord temp = null;
			for (SourceTableRecord record : records) {
				if (!visited.contains(record)) {
					temp = record;
					break;
				}
			}
			neighbours = findAutoMatchNeighbours(ruleSet, temp, visited);
			makeAutoMatches(ruleSet, temp, neighbours, visited);
		}
	}

	/**
	 * Creates the matches necessary in an auto-match while maintaining the
	 * 'visited' set and propagating the algorithm to neighbours of selected
	 * nodes.
	 */
	private void makeAutoMatches(MungeProcess ruleSet,
			SourceTableRecord selected,
			Set<SourceTableRecord> neighbours,
			Set<SourceTableRecord> visited) throws SQLException, ArchitectException {
		logger.debug("makeAutoMatches called, selected's key values = " + selected.getKeyValues());
		visited.add(selected);
		GraphModel<SourceTableRecord, PotentialMatchRecord> nonDirectedGraph =
			new NonDirectedUserValidatedMatchPoolGraphModel(this, new HashSet<PotentialMatchRecord>());
		BreadthFirstSearch<SourceTableRecord, PotentialMatchRecord> bfs =
			new BreadthFirstSearch<SourceTableRecord, PotentialMatchRecord>();
		Set<SourceTableRecord> reachable = new HashSet<SourceTableRecord>(bfs.performSearch(nonDirectedGraph, selected));
		Set<SourceTableRecord> noMatchNodes = findNoMatchNodes(reachable);
		for (SourceTableRecord record : neighbours) {
			if (!noMatchNodes.contains(record)) {
				defineMaster(selected, record, true);
				nonDirectedGraph = new NonDirectedUserValidatedMatchPoolGraphModel(this, new HashSet<PotentialMatchRecord>());
				bfs = new BreadthFirstSearch<SourceTableRecord, PotentialMatchRecord>();
				reachable = new HashSet<SourceTableRecord>(bfs.performSearch(nonDirectedGraph, selected));
				noMatchNodes = findNoMatchNodes(reachable);
			}
		}
		for (SourceTableRecord record : neighbours) {
			if (!visited.contains(record)) {
				makeAutoMatches(ruleSet, record, findAutoMatchNeighbours(ruleSet, record, visited), visited);
			}
		}
	}

	/**
	 * Finds all the neighbours that auto-match worries about as explained in
	 * the comment for doAutoMatch in the context that 'record' is selected in
	 * step 3
	 */
	private Set<SourceTableRecord> findAutoMatchNeighbours(MungeProcess ruleSet,
			SourceTableRecord record,
			Set<SourceTableRecord> visited) {
		logger.debug("The size of visited is " + visited.size());
		Set<SourceTableRecord> ret = new HashSet<SourceTableRecord>();
		for (PotentialMatchRecord pmr : record.getOriginalMatchEdges()) {
			if (pmr.getRuleSet() == ruleSet 
					&& pmr.getMatchStatus() != MatchType.NOMATCH) {
				if (record == pmr.getOriginalLhs() && !visited.contains(pmr.getOriginalRhs())) {
					ret.add(pmr.getOriginalRhs());
				} else if (record == pmr.getOriginalRhs() && !visited.contains(pmr.getOriginalLhs())) {
					ret.add(pmr.getOriginalLhs());
				}
			}
		}
		logger.debug("findAutoMatchNeighbours: The neighbours to automatch for " + record + " are " + ret);
		return ret;
	}
}