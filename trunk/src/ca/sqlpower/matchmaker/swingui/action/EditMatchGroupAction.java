package ca.sqlpower.matchmaker.swingui.action;

import java.awt.Window;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JDialog;

import ca.sqlpower.architect.ArchitectException;
import ca.sqlpower.architect.ArchitectRuntimeException;
import ca.sqlpower.architect.swingui.ArchitectPanelBuilder;
import ca.sqlpower.matchmaker.hibernate.PlMatchGroup;
import ca.sqlpower.matchmaker.swingui.PlMatchGroupPanel;


public class EditMatchGroupAction extends AbstractAction {
	
	PlMatchGroup matchGroup;
	private Window window;
	

	public EditMatchGroupAction(PlMatchGroup matchGroup, Window parentWindow) {
		super("Edit Match Group");
		this.matchGroup = matchGroup;
		this.window = parentWindow;
	}


	public void actionPerformed(ActionEvent e)  {
		JDialog d;
		try {
			d = ArchitectPanelBuilder.createArchitectPanelDialog(new PlMatchGroupPanel(matchGroup), window, "Edit Match Group", "Save Match Group");
		} catch (ArchitectException e1) {
			throw new ArchitectRuntimeException(e1);
		}
		d.setVisible(true);
		
	}

}
