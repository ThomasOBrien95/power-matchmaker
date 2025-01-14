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

package ca.sqlpower.matchmaker.swingui.action;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.log4j.Logger;

import ca.sqlpower.matchmaker.PlFolder;
import ca.sqlpower.matchmaker.Project;
import ca.sqlpower.matchmaker.dao.xml.ProjectDAOXML;
import ca.sqlpower.matchmaker.swingui.MatchMakerObjectComboBoxCellRenderer;
import ca.sqlpower.matchmaker.swingui.MatchMakerSwingSession;
import ca.sqlpower.matchmaker.swingui.NoEditEditorPane;
import ca.sqlpower.matchmaker.validation.ProjectNameValidator;
import ca.sqlpower.swingui.DataEntryPanelBuilder;
import ca.sqlpower.validation.Validator;
import ca.sqlpower.validation.swingui.FormValidationHandler;
import ca.sqlpower.validation.swingui.StatusComponent;

public class DuplicateProjectAction extends AbstractAction {
	
	private static final Logger logger = Logger.getLogger(DuplicateProjectAction.class);
	StatusComponent status = new StatusComponent();

	private MatchMakerSwingSession swingSession;
	private Project project;
	private Callable<Boolean> okCall;
	private Callable<Boolean> cancelCall;
	private JComboBox folderComboBox;
	private FormValidationHandler handler;
	
	public DuplicateProjectAction(MatchMakerSwingSession swingSession, Project project) {
		super("Duplicate Project...");
		this.swingSession = swingSession;
		this.project = project;
		handler = new FormValidationHandler(status);
	}
	
	private class DuplicatePanel extends NoEditEditorPane {

		private JTextField targetNameField;

		public DuplicatePanel(String newName, JComboBox folderComboBox) {
			JPanel panel = new JPanel(new GridLayout(5,1));
			panel.add(status);
			targetNameField = new JTextField(newName,60);
			panel.add(targetNameField);
			panel.add(new JLabel(""));
			panel.add(folderComboBox);
			setPanel(panel);
		}

		public String getDupName() {
			return targetNameField.getText();
		}

		public JTextField getProjectNameField() {
			return targetNameField;
		}
	}
	
	public void actionPerformed(ActionEvent e) {

		String newName = null;
        for (int count = 0;; count++) {
            newName = "Copy" + (count == 0 ? "" : " " + count) + " of " + project.getName();
            if (swingSession.isThisProjectNameAcceptable(newName)) {
                break;
            }
        }
		final JDialog dialog;

		final List<PlFolder> folders = swingSession.getCurrentFolderParent().getChildren(PlFolder.class);
		folderComboBox = new JComboBox(new DefaultComboBoxModel(folders.toArray()));
		folderComboBox.setRenderer(new MatchMakerObjectComboBoxCellRenderer());
		folderComboBox.setSelectedItem(project.getParent());
		
		final DuplicatePanel archPanel = new DuplicatePanel(newName,folderComboBox);

		okCall = new Callable<Boolean>() {
			public Boolean call() {
				String newName = archPanel.getDupName();
				PlFolder folder = (PlFolder) folderComboBox.getSelectedItem();
				
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				ProjectDAOXML outdao = new ProjectDAOXML(out);
				outdao.save(project);
				
				ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
				ProjectDAOXML indao = new ProjectDAOXML(swingSession, in);
				Project newProject = indao.findAll().get(0);
				newProject.setName(newName);
				folder.addChild(newProject);
				return Boolean.TRUE;
			}};
			
		cancelCall = new Callable<Boolean>() {
			public Boolean call() {
				return Boolean.TRUE;
			}};
			
		dialog = DataEntryPanelBuilder.createDataEntryPanelDialog(archPanel,
				swingSession.getFrame(),
				"Duplicate Project",
				"OK",
				okCall,
				cancelCall);
		
		dialog.pack();
		dialog.setLocationRelativeTo(swingSession.getFrame());
		dialog.setVisible(true);
		
		Validator v = new ProjectNameValidator(swingSession,new Project());
        handler.addValidateObject(archPanel.getProjectNameField(),v);
	}

}
