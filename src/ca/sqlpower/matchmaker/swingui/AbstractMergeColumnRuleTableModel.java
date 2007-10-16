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

package ca.sqlpower.matchmaker.swingui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import ca.sqlpower.architect.ArchitectException;
import ca.sqlpower.architect.SQLColumn;
import ca.sqlpower.architect.SQLIndex;
import ca.sqlpower.matchmaker.ColumnMergeRules;
import ca.sqlpower.matchmaker.MatchMakerUtils;
import ca.sqlpower.matchmaker.TableMergeRules;
import ca.sqlpower.matchmaker.event.MatchMakerEvent;
import ca.sqlpower.matchmaker.event.MatchMakerListener;
import ca.sqlpower.swingui.SPSUtils;

/**
 * Abstract table model of the column merge rules that belongs to the table merge rule.
 * The two implementations are for source and related merge rules, which require significantly
 * different table models.
 * row count = children count of table merge rule.
 */

public abstract class AbstractMergeColumnRuleTableModel extends AbstractTableModel implements MatchMakerListener {

	protected TableMergeRules mergeRule;
	protected List<Integer> primaryKeys = new ArrayList<Integer>();
	
	public abstract int getColumnCount();
	
	public abstract String getColumnName(int column);
	
	public abstract Object getValueAt(int rowIndex, int columnIndex);
	
	public abstract boolean isCellEditable(int rowIndex, int columnIndex);
	
	public abstract Class<?> getColumnClass(int columnIndex);
	
	public abstract void setValueAt(Object aValue, int rowIndex, int columnIndex);
	
	public AbstractMergeColumnRuleTableModel(TableMergeRules mergeRule) {
		this.mergeRule = mergeRule;
		MatchMakerUtils.listenToHierarchy(this, this.mergeRule);
	}
	

	public int getRowCount() {
		return mergeRule.getChildren().size();
	}

    public void mmChildrenInserted(MatchMakerEvent evt) {
        if(evt.getSource() == mergeRule){
            int[] changed = evt.getChangeIndices();
            ArrayList<Integer> changedIndices = new ArrayList<Integer>();
            for (int selectedRowIndex:changed){
                changedIndices.add(new Integer(selectedRowIndex));
            }
            Collections.sort(changedIndices);
            for (int i=1; i < changedIndices.size(); i++){
                if (changedIndices.get(i-1)!=changedIndices.get(i)-1){
                    fireTableStructureChanged();
                    return;
                }
            }
            for (Object columnMergeRule:evt.getChildren()){
                ((ColumnMergeRules) columnMergeRule).addMatchMakerListener(this);
            }
            fireTableRowsInserted(changedIndices.get(0), changedIndices.get(changedIndices.size()-1));
        }
    }

    public void mmChildrenRemoved(MatchMakerEvent evt) {
        if(evt.getSource() == mergeRule) {
            int[] changed = evt.getChangeIndices();
            ArrayList<Integer> changedIndices = new ArrayList<Integer>();
            for (int selectedRowIndex:changed){
                changedIndices.add(new Integer(selectedRowIndex));
            }
            Collections.sort(changedIndices);
            for (int i=1; i < changedIndices.size(); i++) {
                if (changedIndices.get(i-1)!=changedIndices.get(i)-1) {
                    fireTableStructureChanged();
                    return;
                }
            }
            for (Object columnMergeRule:evt.getChildren()) {
                ((ColumnMergeRules) columnMergeRule).removeMatchMakerListener(this);
            }
            fireTableRowsDeleted(changedIndices.get(0), changedIndices.get(changedIndices.size()-1));
        }
    }

    public void mmPropertyChanged(MatchMakerEvent evt) { 
        if(evt.getSource() instanceof ColumnMergeRules) {
            fireTableRowsUpdated(mergeRule.getChildren().indexOf(evt.getSource()), mergeRule.getChildren().indexOf(evt.getSource()));
        }
    }

    public void mmStructureChanged(MatchMakerEvent evt) {
        fireTableStructureChanged();
    }
    
    /**
     * This updates the list of primary keys in the source or parent table
     * of the merge rule. This is used to decide which column in the table
     * to set as non edit-able.
     */
	protected void updatePrimaryKeys() {
		primaryKeys = new ArrayList<Integer>();
		try {
			SQLIndex tableIndex = mergeRule.getTableIndex();
			if (tableIndex != null) {
				for (int i = 0; i < getRowCount(); i++) {
					SQLColumn column = (SQLColumn) getValueAt(i, 0);
					if (tableIndex.getChildByName(column.getName()) != null) {
						primaryKeys.add(i);
					}
				}
			}
		} catch (ArchitectException e) {
			MatchMakerSwingSession session = (MatchMakerSwingSession) mergeRule.getSession();
			SPSUtils.showExceptionDialogNoReport(session.getFrame(),
					"Failed to retrieve primary keys of source table", e);
		}
	}
}