/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of DQguru
 *
 * DQguru is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * DQguru is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.matchmaker;

import java.util.Collections;
import java.util.List;

import ca.sqlpower.object.SPObject;
import ca.sqlpower.sqlobject.SQLColumn;
import ca.sqlpower.sqlobject.SQLIndex;
import ca.sqlpower.sqlobject.SQLObject;
import ca.sqlpower.sqlobject.SQLObjectException;
import ca.sqlpower.sqlobject.SQLTable;

public class TableIndex extends AbstractMatchMakerObject{
	
	public static final List<Class<? extends SPObject>> allowedChildTypes = 
		Collections.emptyList();
	
	AbstractMatchMakerObject mmo;
 	/**
     * The unique index of the source table that we're using.  Not necessarily one of the
     * unique indices defined in the database; the user can pick an arbitrary set of columns.
     */
    private SQLIndex sourceTableIndex;
    
    
    private CachableTable table;
	private String property;

	/**
	 * Manages a sqlindex on cachableTable table for mmo. 
	 * 
	 * The property argument is the property type the index should use for an event.
	 * We assume all arguments are non-null
	 */
	public TableIndex(CachableTable table, String property) {
		this.table = table;
		this.property = property;
	}
	
    /**
     * Hooks the index up to the source table, attempts to resolve the
     * column names to actual SQLColumn references on the source table,
     * and then returns it!
     */
    public SQLIndex getTableIndex() throws SQLObjectException {
    	if (sourceTableIndex != null && !sourceTableIndex.isPopulated()) sourceTableIndex.populate();
    	
    	if (table.getSourceTable() != null && sourceTableIndex != null) {
    		sourceTableIndex.setParent(table.getSourceTable());
    		resolveTableIndexColumns(sourceTableIndex);
    	}
    	return sourceTableIndex;
    }

    /**
     * Attempts to set the column property of each index column in the
     * sourceTableColumns.  The UserType for SQLIndex can't do this because
     * the source table isn't populated yet when it's invoked.
     */
    private void resolveTableIndexColumns(SQLIndex si) throws SQLObjectException {
    	SQLTable st = table.getSourceTable();
    	if (!st.isPopulated()) st.populate();
    	for (SQLObject sqo : si.getChildren()) {
    		SQLIndex.Column col = (SQLIndex.Column) sqo;
    		SQLColumn actualColumn = st.getColumnByName(col.getName());
    		col.setColumn(actualColumn);
    	}
	}

	public void setTableIndex(SQLIndex index) {
    	final SQLIndex oldIndex = sourceTableIndex;
    	sourceTableIndex = index;
    	//TODO: Find the right propertyName
    	firePropertyChange(property, oldIndex, index);
    }

	/**
	 * Returns true if the index is user created.  False if the
	 * index is not user created or is null
	 * we check if the index is user created by if the parent
	 * is null or dosn't contain the sql index.
	 */
	public boolean isUserCreated() throws SQLObjectException {
		if (getTableIndex() == null) return false;
		if (getTableIndex().getParent() == null ){ 
			return true;
		} else if (getTableIndex().getParent().getChildren().contains(sourceTableIndex)){
			return false;
		} else {
			return true;
		}
	}
	
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("Table Index @").append(System.identityHashCode(this));
		buf.append(", index: "+sourceTableIndex);
		return buf.toString();
	}

	@Override
	public MatchMakerObject duplicate(MatchMakerObject parent,
			MatchMakerSession session) {
		throw new RuntimeException("Don't bother because this is a mess.");
	}

	@Override
	public List<? extends SPObject> getChildren() {
		return Collections.emptyList();
	}

	@Override
	public List<Class<? extends SPObject>> getAllowedChildTypes() {
		return allowedChildTypes;
	}
}
