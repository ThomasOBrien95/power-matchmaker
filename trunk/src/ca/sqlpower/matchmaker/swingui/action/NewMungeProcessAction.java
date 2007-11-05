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

package ca.sqlpower.matchmaker.swingui.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import ca.sqlpower.architect.ArchitectException;
import ca.sqlpower.matchmaker.Project;
import ca.sqlpower.matchmaker.munge.MungeProcess;
import ca.sqlpower.matchmaker.swingui.MatchMakerSwingSession;
import ca.sqlpower.matchmaker.swingui.MungeProcessEditor;
import ca.sqlpower.swingui.SPSUtils;

/**
 * A simple action to adds a new munge process to the swing session and
 * opens up the editor for the new munge process.
 */
public class NewMungeProcessAction extends AbstractAction {
    
    private final MatchMakerSwingSession swingSession;
	private final Project parent;

	public NewMungeProcessAction(MatchMakerSwingSession swingSession, Project parent) {
	    super("New Munge Process");
        this.swingSession = swingSession;
        this.parent = parent;
        if (parent == null) throw new IllegalArgumentException("Parent must be non null");
	}
	
	public void actionPerformed(ActionEvent e) {
		MungeProcess g = new MungeProcess();
		g.setName("New Munge Process");
        parent.getMungeProcessesFolder().addChild(g);
		MungeProcessEditor editor;
		try {
			editor = new MungeProcessEditor(swingSession,parent, g);
			swingSession.setCurrentEditorComponent(editor);
		} catch (ArchitectException ex) {
			SPSUtils.showExceptionDialogNoReport(swingSession.getFrame(), 
					"Error Loading Source Table", 
					"There was an error loading the source table", ex);
		} 
	}

}
