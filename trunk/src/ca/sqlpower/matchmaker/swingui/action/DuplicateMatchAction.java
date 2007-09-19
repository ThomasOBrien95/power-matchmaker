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

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.log4j.Logger;

import ca.sqlpower.matchmaker.Match;
import ca.sqlpower.matchmaker.PlFolder;
import ca.sqlpower.matchmaker.swingui.MatchMakerObjectComboBoxCellRenderer;
import ca.sqlpower.matchmaker.swingui.MatchMakerSwingSession;
import ca.sqlpower.matchmaker.validation.MatchNameValidator;
import ca.sqlpower.swingui.DataEntryPanel;
import ca.sqlpower.swingui.DataEntryPanelBuilder;
import ca.sqlpower.validation.Validator;
import ca.sqlpower.validation.swingui.FormValidationHandler;
import ca.sqlpower.validation.swingui.StatusComponent;

public class DuplicateMatchAction extends AbstractAction {
	
	private static final Logger logger = Logger.getLogger(DuplicateMatchAction.class);
	StatusComponent status = new StatusComponent();

	private MatchMakerSwingSession swingSession;
	private Match match;
	private Callable<Boolean> okCall;
	private Callable<Boolean> cancelCall;
	private JComboBox folderComboBox;
	private FormValidationHandler handler;
	
	public DuplicateMatchAction(MatchMakerSwingSession swingSession, Match match) {
		super("Duplicate");
		this.swingSession = swingSession;
		this.match = match;
		handler = new FormValidationHandler(status);
	}
	
	private class DuplicatePanel implements DataEntryPanel {

		
		private final JPanel panel;
		private JTextField targetNameField;

		public DuplicatePanel(String newName, JComboBox folderComboBox) {
			panel = new JPanel(new GridLayout(5,1));
			panel.add(status);
			targetNameField = new JTextField(newName,60);
			panel.add(targetNameField);
			panel.add(new JLabel(""));
			panel.add(folderComboBox);
		}
		
		public boolean applyChanges() {
			return true;
		}

		public void discardChanges() {
		}

		public JComponent getPanel() {
			return panel;
		}
		public String getDupName() {
			return targetNameField.getText();
		}

		public JTextField getMatchNameField() {
			return targetNameField;
		}
	}
	public void actionPerformed(ActionEvent e) {

		String newName = null;
		for ( int count=0; ; count++) {
			newName = match.getName() +
								"_DUP" +
								(count==0?"":String.valueOf(count));
			if ( swingSession.isThisMatchNameAcceptable(newName) )
				break;
		}
		final JDialog dialog;

		final List<PlFolder> folders = swingSession.getCurrentFolderParent().getChildren();
		folderComboBox = new JComboBox(new DefaultComboBoxModel(folders.toArray()));
		folderComboBox.setRenderer(new MatchMakerObjectComboBoxCellRenderer());
		folderComboBox.setSelectedItem(match.getParent());
		
		final DuplicatePanel archPanel = new DuplicatePanel(newName,folderComboBox);

		okCall = new Callable<Boolean>() {
			public Boolean call() {
				String newName = archPanel.getDupName();
				PlFolder<Match> folder = (PlFolder<Match>) folderComboBox
				.getSelectedItem();
				Match newmatch = match.duplicate(folder,swingSession);
				newmatch.setName(newName);
				folder.addChild(newmatch);
				swingSession.save(newmatch);
				return new Boolean(true);
			}};
			
		cancelCall = new Callable<Boolean>() {
			public Boolean call() {
				return new Boolean(true);
			}};
			
		dialog = DataEntryPanelBuilder.createDataEntryPanelDialog(archPanel,
				swingSession.getFrame(),
				"Duplicate Match",
				"OK",
				okCall,
				cancelCall);
		
		dialog.pack();
		dialog.setLocationRelativeTo(swingSession.getFrame());
		dialog.setVisible(true);
		
		Validator v = new MatchNameValidator(swingSession,new Match());
        handler.addValidateObject(archPanel.getMatchNameField(),v);
	}

}
