package ca.sqlpower.matchmaker.swingui.action;

import java.awt.event.ActionEvent;
import java.sql.SQLException;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import ca.sqlpower.architect.ArchitectUtils;
import ca.sqlpower.matchmaker.Match;
import ca.sqlpower.matchmaker.PlFolder;
import ca.sqlpower.matchmaker.Match.MatchMode;
import ca.sqlpower.matchmaker.swingui.MatchEditor;
import ca.sqlpower.matchmaker.swingui.MatchMakerSwingSession;

/**
 * Creates a new Match object and a GUI editor for it, then puts that editor in the split pane.
 */
public final class NewMatchAction extends AbstractAction {

    private final MatchMakerSwingSession swingSession;
	private PlFolder<Match> folder;

	public NewMatchAction(
            MatchMakerSwingSession swingSession,
            String name,
            PlFolder folder) {
		super(name);
        this.swingSession = swingSession;
		this.folder = folder;
	}

	public void actionPerformed(ActionEvent e) {
	    MatchEditor me;
		try {
			final Match match = new Match();
			match.setSession(swingSession);
			match.setType(MatchMode.FIND_DUPES);

			folder = ArchitectUtils.getTreeObject(swingSession.getTree(),PlFolder.class);
			if ( folder == null ) {
				JOptionPane.showMessageDialog(swingSession.getFrame(),
						"Please select a folder first",
						"Warning",
						JOptionPane.INFORMATION_MESSAGE);
				return;
			}

			me = new MatchEditor(swingSession,match,folder);
			swingSession.setCurrentEditorComponent(me);
		} catch (SQLException e2) {
			throw new RuntimeException(e2);
		}
	}
}
