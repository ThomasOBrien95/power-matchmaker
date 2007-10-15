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

import ca.sqlpower.architect.ArchitectException;
import ca.sqlpower.architect.ArchitectRuntimeException;
import ca.sqlpower.architect.SQLIndex;
import ca.sqlpower.architect.SQLTable;
import ca.sqlpower.architect.ddl.DDLUtils;

/**
 *
 * Merge strategy handles a per table setup of the merge engine
 * The best way to think of this is a per row merge rules.
 */
public class TableMergeRules
	extends AbstractMatchMakerObject<TableMergeRules, ColumnMergeRules> {

	private Long oid;

	/**
	 * Whether or not we should delete the duplicate row 
	 */
	private boolean deleteDup;
	
	
	/**
	 * The table on which we're merging
	 */
	private CachableTable cachableTable = new CachableTable(this, "table");;
	
	
	/**
	 * The index for table 
	 */
	private TableIndex tableIndex;
	
	/**
	 * The parent tableMergeRule
	 */
	private String parentTable;
	
	public TableMergeRules() {
		tableIndex = new TableIndex(this,cachableTable,"tableIndex");
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof TableMergeRules)){
			return false;
		}
		TableMergeRules other = (TableMergeRules) obj;
		if (getParent() != null ){
			if (other == null || !getParent().equals(other.getParent())) {
				return false;
			}
		} else {
			if (other.getParent() != null){
				return false;
			}
		}
		if (getSourceTable() == null) {
			if (other.getSourceTable() != null){
				return false;
			}
		} else {
			if (other == null || !getSourceTable().equals(other.getSourceTable())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Creates a new table merge rules with the parent and session that 
	 * are passed in.
	 * 
	 * It makes a copy of all non mutable objects.  Except the index when the
	 * index is the default index on the table.
	 */
	public TableMergeRules duplicate(MatchMakerObject parent, MatchMakerSession session) {
		TableMergeRules newMergeStrategy = new TableMergeRules();
		newMergeStrategy.setParent(parent);
		newMergeStrategy.setName(getName());
		newMergeStrategy.setSession(session);
		newMergeStrategy.setDeleteDup(isDeleteDup());
		newMergeStrategy.setTableName(getTableName());
		newMergeStrategy.setCatalogName(getCatalogName());
		newMergeStrategy.setSchemaName(getSchemaName());
		try {
			if (tableIndex.isUserCreated()) {
				newMergeStrategy.setTableIndex(new SQLIndex(getTableIndex()));
			} else {
				newMergeStrategy.setTableIndex(getTableIndex());
			}
		} catch (ArchitectException e) {
			throw new ArchitectRuntimeException(e);
		}

		for (ColumnMergeRules c : getChildren()) {
			ColumnMergeRules newColumnMergeRules = c.duplicate(newMergeStrategy,session);
			newMergeStrategy.addChild(newColumnMergeRules);
		}
		return newMergeStrategy;
	}

	public String getCatalogName() {
		return cachableTable.getCatalogName();
	}

	public String getSchemaName() {
		return cachableTable.getSchemaName();
	}

	public SQLTable getSourceTable() {
		return cachableTable.getSourceTable();
	}
	
	public String getTableName() {
		return cachableTable.getTableName();
	}

	public void setCatalogName(String sourceTableCatalog) {
		cachableTable.setCatalogName(sourceTableCatalog);
	}

	public void setSchemaName(String sourceTableSchema) {
		cachableTable.setSchemaName(sourceTableSchema);
	}

	public void setTable(SQLTable table) {
		this.cachableTable.setTable(table);
		setName(table==null?null:DDLUtils.toQualifiedName(table));
	}

	public void setTableName(String sourceTableName) {
		cachableTable.setTableName(sourceTableName);
		setName(DDLUtils.toQualifiedName(getCatalogName(),getSchemaName(),sourceTableName));
	}

	public boolean isDeleteDup() {
		return deleteDup;
	}

	public void setDeleteDup(boolean deleteDup) {
		boolean oldValue = this.deleteDup;
		this.deleteDup = deleteDup;
		getEventSupport().firePropertyChange("deleteDup", oldValue, this.deleteDup);
	}
	

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("Merge Strategy@"+System.identityHashCode(this)+"->'").append(getName()).append("' ");
		buf.append("Parent->'").append(getParent()).append("' ");
		buf.append("isDeletedDup()->'").append(isDeleteDup()).append("' ");
		return buf.toString();
	}

	public SQLIndex getTableIndex() throws ArchitectException {
		return tableIndex.getTableIndex();
	}

	public void setTableIndex(SQLIndex index) {
		tableIndex.setTableIndex(index);
	}
	
	/**
     * Gets the grandparent of this object in the MatchMaker object tree.  If the parent
     * (a folder) is null, returns null.
     */
    public Match getParentMatch() {
        MatchMakerObject parentFolder = getParent();
        if (parentFolder == null) {
            return null;
        } else {
            return (Match) parentFolder.getParent();
        }
    }

    /**
     * Sets the parent of this object to be the match rule set folder of the given match object
     *
     * this will fire a <b>parent</b> changed event not a parent match event
     */
    public void setParentMatch(Match grandparent) {
        if (grandparent == null) {
            setParent(null);
        } else {
            setParent(grandparent.getTableMergeRulesFolder());
        }
    }

	public Long getOid() {
		return oid;
	}

	public void setOid(Long oid) {
		this.oid = oid;
	}

	public String getParentTable() {
		return parentTable;
	}

	public void setParentTable(String parentTable) {
		this.parentTable = parentTable;
	}

}