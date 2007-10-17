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
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;

import ca.sqlpower.matchmaker.Match;
import ca.sqlpower.matchmaker.PlFolder;
import ca.sqlpower.matchmaker.swingui.MMSUtils;
import ca.sqlpower.matchmaker.swingui.MatchEditor;
import ca.sqlpower.matchmaker.swingui.MatchMakerSwingSession;
import ca.sqlpower.swingui.SPSUtils;

/**
 * Creates a new Match object and a GUI editor for it, then puts that editor in the split pane.
 */
public final class NewMatchAction extends AbstractAction {

	private static final Logger logger = Logger.getLogger(NewMatchAction.class);
	
	private final Match.MatchMode type;
    private final MatchMakerSwingSession swingSession;
    
	public NewMatchAction(MatchMakerSwingSession swingSession, String name, Match.MatchMode type) {
		super(name);
        this.swingSession = swingSession;
        this.type = type;
	}

	public void actionPerformed(ActionEvent e) {
		PlFolder<Match> folder = MMSUtils.getTreeObject(swingSession.getTree(), PlFolder.class);
		if (folder == null) {
			JOptionPane.showMessageDialog(swingSession.getFrame(),
					"Please select a folder first",
					"Warning",
					JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		final Match match = new Match();
		match.setSession(swingSession);	
		match.setType(type);
		try {
			MatchEditor me = new MatchEditor(swingSession, match, folder);
			swingSession.setCurrentEditorComponent(me);
		} catch (Exception ex) {
			SPSUtils.showExceptionDialogNoReport(swingSession.getFrame(), "Couldn't create match", ex);
		}
	}

}