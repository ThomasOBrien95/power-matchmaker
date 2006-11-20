package ca.sqlpower.matchmaker.swingui;

import java.awt.HeadlessException;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import ca.sqlpower.architect.ArchitectException;
import ca.sqlpower.architect.ArchitectUtils;
import ca.sqlpower.architect.SQLCatalog;
import ca.sqlpower.architect.SQLDatabase;
import ca.sqlpower.architect.SQLIndex;
import ca.sqlpower.architect.SQLSchema;
import ca.sqlpower.architect.SQLTable;
import ca.sqlpower.architect.swingui.ASUtils;
import ca.sqlpower.architect.swingui.ArchitectPanelBuilder;
import ca.sqlpower.matchmaker.Match;
import ca.sqlpower.matchmaker.MatchType;
import ca.sqlpower.matchmaker.PlFolder;
import ca.sqlpower.matchmaker.dao.MatchMakerDAO;
import ca.sqlpower.matchmaker.swingui.action.NewMatchGroupAction;
import ca.sqlpower.matchmaker.util.SourceTable;
import ca.sqlpower.validation.Status;
import ca.sqlpower.validation.ValidateResult;
import ca.sqlpower.validation.Validator;
import ca.sqlpower.validation.swingui.FormValidationHandler;
import ca.sqlpower.validation.swingui.StatusComponent;

import com.jgoodies.forms.builder.ButtonStackBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.debug.FormDebugPanel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class MatchEditor {

	private static final Logger logger = Logger.getLogger(MatchEditor.class);

	private SQLObjectChooser sourceChooser;
	private SQLObjectChooser resultChooser;

	private JPanel panel;

	StatusComponent status = new StatusComponent();
    private JTextField matchId = new JTextField();
    private JComboBox folderComboBox = new JComboBox();
    private JTextArea desc = new JTextArea();
    private JComboBox matchType = new JComboBox();

    private JTextField resultTableName = new JTextField();

    private JButton viewBuilder;
    private JButton createResultTable;

    private JButton saveMatch;
    private JButton showAuditInfo;
    private JButton runMatch;
    private JButton validationStatus;
    private JButton validateMatch;
    private FilterComponentsPanel filterPanel;

    private final MatchMakerSwingSession swingSession;

    /**
     * The match that this editor is editing.  If you want to edit a different match,
     * create a new MatchEditor.
     */
	private final Match match;
	private final PlFolder<Match> folder;
	private FormValidationHandler handler;

	/**
	 * the constructor, for a match that is not new, we create a backup for it,
	 * and give it the name of the old one, when we save it, we will remove the
	 * the backup from the folder, and insert the new one.
	 * @param swingSession  -- a MatchMakerSession
	 * @param match			-- a Match Object to be edited
	 * @param folder		-- where the match is
	 * @param newMatch		-- a flag indicates it's a new match or not
	 * @throws HeadlessException
	 * @throws ArchitectException
	 */
    public MatchEditor(MatchMakerSwingSession swingSession, Match match,
    		PlFolder<Match> folder) throws HeadlessException, ArchitectException {
    	super();
        this.swingSession = swingSession;
        if (match == null) throw new NullPointerException("You can't edit a null plmatch");
        this.match = match;
        this.folder = folder;
        handler = new FormValidationHandler(status);
        newMatchGroupAction = new NewMatchGroupAction(swingSession, match);
        buildUI();
        handler.addPropertyChangeListener(new PropertyChangeListener(){
			public void propertyChange(PropertyChangeEvent evt) {
				refershButtons();
			}
        });
    }

    /**
     * Saves the current match (which is referenced in the plMatch member variable of this editor instance).
     * If there is no current plMatch, a new one will be created and its properties will be set just like
     * they would if one had existed.  In either case, this action will then use Hibernate to save the
     * match object back to the database (but it should use the MatchHome interface instead).
     */
	private Action saveAction = new AbstractAction("Save") {
		public void actionPerformed(ActionEvent e) {
            try {
                boolean ok = saveMatch();
                if ( ok ) {
                	JOptionPane.showMessageDialog(swingSession.getFrame(),
                			"Match Interface Save Successfully",
                			"Saved",JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                ASUtils.showExceptionDialog(swingSession.getFrame(),
                		"Match Interface Not Saved", ex);
            }
		}
	};

	private Window getParentWindow() {
	    return SwingUtilities.getWindowAncestor(panel);
	}

    /**
     * Returns the parent (owning) frame of this match editor.  If the owner
     * isn't a frame (it might be a dialog or AWT Window) then null is returned.
     * You should always use {@link #getParentWindow()} in preference to
     * this method unless you really really need a JFrame.
     *
     * @return the parent JFrame of this match editor's panel, or null if
     * the owner is not a JFrame.
     */
    private JFrame getParentFrame() {
        Window owner = getParentWindow();
        if (owner instanceof JFrame) return (JFrame) owner;
        else return null;
    }

	private Action showAuditInfoAction = new AbstractAction("Show Audit Info") {
		public void actionPerformed(ActionEvent e) {

			MatchInfoPanel p = new MatchInfoPanel(match);
			JDialog d = ArchitectPanelBuilder.createSingleButtonArchitectPanelDialog(
					p, getParentWindow(),
					"Audit Information", "OK");
			d.pack();
			d.setVisible(true);
		}};

	private Action runMatchAction = new AbstractAction("Run Match") {
		public void actionPerformed(ActionEvent e) {
			RunMatchDialog p = new RunMatchDialog(swingSession, match, getParentFrame());
			p.pack();
			p.setVisible(true);
		}};

	private Action validationStatusAction = new AbstractAction("View Validation ValidateResult") {
		public void actionPerformed(ActionEvent e) {
			MatchValidationStatus p = new MatchValidationStatus(swingSession, match,
                    ArchitectPanelBuilder.makeOwnedDialog(getPanel(),"View Match Validation Status"));
			p.pack();
			p.setVisible(true);
		}};
	private Action validateMatchAction = new AbstractAction("Validate Match") {
		public void actionPerformed(ActionEvent e) {
			try {
				MatchValidation v = new MatchValidation(swingSession, match);
				v.pack();
				v.setVisible(true);
			} catch (HeadlessException e1) {
				ASUtils.showExceptionDialog(swingSession.getFrame(),
						"Unknown Error",e1);
			} catch (SQLException e1) {
				ASUtils.showExceptionDialog(swingSession.getFrame(),
						"Unknown SQL Error",e1);
			} catch (ArchitectException e1) {
				ASUtils.showExceptionDialog(swingSession.getFrame(),
						"Unknown Error",e1);
			}
		}};
	private Action viewBuilderAction = new AbstractAction("View Builder") {
		public void actionPerformed(ActionEvent e) {
            SQLTable t = (SQLTable)sourceChooser.getTableComboBox().getSelectedItem();
            JDialog d;
			if (t !=null){
                try {
                    d = new ViewBuilderDialog(swingSession, getParentFrame(), t);
                    d.pack();
                    d.setSize(800, d.getPreferredSize().height);
                    d.setVisible(true);
                } catch (ArchitectException ex) {
                    ASUtils.showExceptionDialog(swingSession.getFrame(),
                    		"Couldn't create view builder", ex);
                }
            }
		}};

	private Action createResultTableAction = new AbstractAction("Create Table") {
		public void actionPerformed(ActionEvent e) {
            // TODO
            JOptionPane.showMessageDialog(swingSession.getFrame(),
            		"We can't create tables yet, sorry.");
		}
	};



	private NewMatchGroupAction newMatchGroupAction;



    private void buildUI() throws ArchitectException {

    	matchId.setName("Match ID");
		sourceChooser = new SQLObjectChooser(swingSession.getFrame(),
        		swingSession.getContext().getDataSources());
        resultChooser = new SQLObjectChooser(swingSession.getFrame(),
        		swingSession.getContext().getDataSources());

        filterPanel = new FilterComponentsPanel();

        List<String> types = new ArrayList<String>();
        for ( MatchType mt : MatchType.values() ) {
        	types.add(mt.getName());
        }

        matchType.setModel(new DefaultComboBoxModel(Match.MatchType.values()));

        sourceChooser.getTableComboBox().addItemListener(new ItemListener(){
        	public void itemStateChanged(ItemEvent e) {
        		filterPanel.getFilterTextArea().setText("");
        	}});

    	viewBuilder = new JButton(viewBuilderAction);
    	createResultTable = new JButton(createResultTableAction);
    	saveMatch = new JButton(saveAction);
    	showAuditInfo = new JButton(showAuditInfoAction);
    	runMatch= new JButton(runMatchAction);
    	validationStatus = new JButton(validationStatusAction);
    	validateMatch = new JButton(validateMatchAction);

    	FormLayout layout = new FormLayout(
				"4dlu,pref,4dlu,fill:min(pref;"+new JComboBox().getMinimumSize().width+"px):grow, 4dlu,pref,10dlu, pref,4dlu", // columns
				"10dlu,pref,4dlu,pref,4dlu,pref,4dlu,pref,4dlu,pref,   16dlu,pref,4dlu,pref,4dlu,pref,4dlu,pref,   4dlu,32dlu,  16dlu,pref,4dlu,pref,4dlu,pref,10dlu"); // rows
    	//		 1     2    3    4    5    6    7    8    9    10      11    12   13   14  15   16       17    18     19  20    21   22    23   24    25

		PanelBuilder pb;

		JPanel p = logger.isDebugEnabled() ? new FormDebugPanel(layout) : new JPanel(layout);
		pb = new PanelBuilder(layout, p);
		CellConstraints cc = new CellConstraints();

		pb.add(new JLabel("Match ID:"), cc.xy(2,4,"r,c"));
		pb.add(new JLabel("Folder:"), cc.xy(2,6,"r,c"));
		pb.add(new JLabel("Description:"), cc.xy(2,8,"r,t"));
		pb.add(new JLabel("Type:"), cc.xy(2,10,"r,c"));

		pb.add(status, cc.xy(4,2));
		pb.add(matchId, cc.xy(4,4));
		pb.add(folderComboBox, cc.xy(4,6));
		pb.add(new JScrollPane(desc), cc.xy(4,8,"f,f"));
		pb.add(matchType, cc.xy(4,10));

		pb.add(sourceChooser.getCatalogTerm(), cc.xy(2,12,"r,c"));
		pb.add(sourceChooser.getSchemaTerm(), cc.xy(2,14,"r,c"));
		pb.add(new JLabel("Table Name:"), cc.xy(2,16,"r,c"));
		pb.add(new JLabel("Unique Index:"), cc.xy(2,18,"r,t"));
		pb.add(new JLabel("Filter:"), cc.xy(2,20,"r,t"));

		pb.add(sourceChooser.getCatalogComboBox(), cc.xy(4,12));
		pb.add(sourceChooser.getSchemaComboBox(), cc.xy(4,14));
		pb.add(sourceChooser.getTableComboBox(), cc.xy(4,16));
		pb.add(sourceChooser.getUniqueKeyComboBox(), cc.xy(4,18,"f,f"));
		pb.add(filterPanel, cc.xy(4,20,"f,f"));

		pb.add(resultChooser.getCatalogTerm(), cc.xy(2,22,"r,c"));
		pb.add(resultChooser.getSchemaTerm(), cc.xy(2,24,"r,c"));
		pb.add(new JLabel("Table Name:"), cc.xy(2,26,"r,c"));

		pb.add(resultChooser.getCatalogComboBox(), cc.xy(4,22));
		pb.add(resultChooser.getSchemaComboBox(), cc.xy(4,24));
		pb.add(resultTableName, cc.xy(4,26));



		pb.add(viewBuilder, cc.xy(6,12,"f,f"));
		pb.add(createResultTable, cc.xywh(6,22,1,3));

		ButtonStackBuilder bb = new ButtonStackBuilder();
		bb.addGridded(saveMatch);
		bb.addRelatedGap();
		bb.addRelatedGap();
		bb.addGridded(new JButton(newMatchGroupAction));
		bb.addRelatedGap();
		bb.addRelatedGap();
		bb.addGridded(showAuditInfo);
		bb.addRelatedGap();
		bb.addRelatedGap();
		bb.addGridded(runMatch);
		bb.addRelatedGap();
		bb.addRelatedGap();
		bb.addGridded(validationStatus);
		bb.addRelatedGap();
		bb.addRelatedGap();
		bb.addGridded(validateMatch);



		pb.add(bb.getPanel(), cc.xywh(8,4,1,14,"f,f"));
		panel = pb.getPanel();
		setDefaultSelections();
    }


    private void setDefaultSelections() {

    	final List<PlFolder> folders = swingSession.getFolders();
    	final SQLDatabase loginDB = swingSession.getDatabase();
        sourceChooser.getDataSourceComboBox().setSelectedItem(loginDB.getDataSource());
        resultChooser.getDataSourceComboBox().setSelectedItem(loginDB.getDataSource());

        sourceChooser.getCatalogComboBox().setRenderer(new SQLObjectComboBoxCellRenderer());
        sourceChooser.getSchemaComboBox().setRenderer(new SQLObjectComboBoxCellRenderer());
        sourceChooser.getTableComboBox().setRenderer(new SQLObjectComboBoxCellRenderer());
        resultChooser.getCatalogComboBox().setRenderer(new SQLObjectComboBoxCellRenderer());
        resultChooser.getSchemaComboBox().setRenderer(new SQLObjectComboBoxCellRenderer());

        folderComboBox.setModel(new DefaultComboBoxModel(folders.toArray()));
        folderComboBox.setRenderer(new MatchMakerObjectComboBoxCellRenderer());
        if ( match.getParent() != null) {
       		folderComboBox.setSelectedItem(match.getParent());
        }

        matchId.setText(match.getName());
        desc.setText(match.getMatchSettings().getDescription());
        matchType.setSelectedItem(match.getType());
        filterPanel.getFilterTextArea().setText(match.getFilter());

        Validator v = new MatchNameValidator(swingSession);
        handler.addValidateObject(matchId,v);

        Validator v2 = new MatchSourceTableValidator(swingSession);
        handler.addValidateObject(sourceChooser.getTableComboBox(),v2);

        Validator v3 = new MatchResultTableNameValidator(swingSession);
        handler.addValidateObject(resultTableName,v3);


        if ( match.getSourceTable() != null ) {

        	SQLTable tableByName = match.getSourceTable().getTable();
        	if (tableByName == null) {
        	} else {
        		filterPanel.setTable(tableByName);
        		SQLCatalog cat = tableByName.getCatalog();
	    		SQLSchema sch = tableByName.getSchema();
	    		if ( cat != null ) {
	    			sourceChooser.getCatalogComboBox().setSelectedItem(cat);
	    		}
	    		if ( sch != null ) {
	    			sourceChooser.getSchemaComboBox().setSelectedItem(sch);
	    		}
	    		sourceChooser.getTableComboBox().setSelectedItem(tableByName);

    			SQLIndex pk = null;
				pk = match.getSourceTable().getUniqueIndex();
    			if ( pk != null ) {
    				sourceChooser.getUniqueKeyComboBox().setSelectedItem(pk);
    			}
        	}
    	}
    	SQLTable resultTable = match.getResultTable();
    	if ( resultTable != null ) {
    		SQLCatalog cat = resultTable.getCatalog();
    		SQLSchema sch = resultTable.getSchema();
    		if ( cat != null ) {
    			resultChooser.getCatalogComboBox().setSelectedItem(cat);
    		}
    		if ( sch != null ) {
    			resultChooser.getSchemaComboBox().setSelectedItem(sch);
    		}
    		resultTableName.setText(match.getResultTable().getName());
    	}

        //This listener is put here to update the SQLTable in FilterPanel so the
        //FilterMakerDialog two dropdown boxes can work properly
        sourceChooser.getTableComboBox().addItemListener(new ItemListener(){
            public void itemStateChanged(ItemEvent e) {
                filterPanel.setTable((SQLTable)(sourceChooser.getTableComboBox().getSelectedItem()));
            }
         });
    }

	public JPanel getPanel() {
		return panel;
	}



    /**
     * Copies all the values from the GUI components into the PlMatch
     * object this component is editing, then persists it to the database.
     * @throws ArchitectException
     * @return true if save OK
     */
    private boolean saveMatch() throws ArchitectException {

    	List<String> fail = handler.getFailResults();
    	List<String> warn = handler.getWarnResults();

    	if ( fail.size() > 0 ) {
    		StringBuffer failMessage = new StringBuffer();
    		for ( String f : fail ) {
    			failMessage.append(f).append("\n");
    		}
    		JOptionPane.showMessageDialog(swingSession.getFrame(),
    				"You have to fix these errors before saving:\n"+failMessage.toString(),
    				"Match error",
    				JOptionPane.ERROR_MESSAGE);
    		return false;
    	} else if ( warn.size() > 0 ) {
    		StringBuffer warnMessage = new StringBuffer();
    		for ( String w : warn ) {
    			warnMessage.append(w).append("\n");
    		}
    		JOptionPane.showMessageDialog(swingSession.getFrame(),
    				"Warning: match will be saved, but you may not be able to run it, because of these wanings:\n"+warnMessage.toString(),
    				"Match warning",
    				JOptionPane.INFORMATION_MESSAGE);
    	}

    	final String matchName = matchId.getText().trim();
        match.setType((Match.MatchType)matchType.getSelectedItem());
        match.getMatchSettings().setDescription(desc.getText());

        if ( match.getSourceTable() == null ) {
        	match.setSourceTable(new SourceTable());
        }
        match.getSourceTable().setTable(
        		((SQLTable) sourceChooser.getTableComboBox().
        				getSelectedItem()));
        match.getSourceTable().setUniqueIndex(
        		((SQLIndex) sourceChooser.getUniqueKeyComboBox().
        				getSelectedItem()));

        if ((matchName == null || matchName.length() == 0) &&
        		match.getSourceTable().getTable() == null ) {
        	JOptionPane.showMessageDialog(getPanel(),
        			"Match Name can not be empty",
        			"Error",
        			JOptionPane.ERROR_MESSAGE);
        	return false;
        }

        String id = matchName;
        if ( id == null || id.length() == 0 ) {
        	StringBuffer s = new StringBuffer();
        	s.append("MATCH_");
        	SQLTable table = match.getSourceTable().getTable();
			if ( table != null &&
					table.getCatalogName() != null &&
        			table.getCatalogName().length() > 0 ) {
        		s.append(table.getCatalogName()).append("_");
        	}
			if ( table != null &&
					table.getSchemaName() != null &&
        			table.getSchemaName().length() > 0 ) {
        		s.append(table.getSchemaName()).append("_");
        	}
			if ( table != null ) {
				s.append(table.getName());
			}
        	id = s.toString();
        	matchId.setText(id);
        }

        SQLDatabase resultTableParentDB = null;
        if ( resultChooser.getCatalogComboBox().isEnabled() &&
        		resultChooser.getCatalogComboBox().getSelectedItem() != null ) {
        	resultTableParentDB = ((SQLCatalog)resultChooser.getCatalogComboBox().
        			getSelectedItem()).getParentDatabase();
        } else if ( resultChooser.getSchemaComboBox().isEnabled() &&
        		resultChooser.getSchemaComboBox().getSelectedItem() != null ) {
        	resultTableParentDB = ArchitectUtils.getAncestor(
        			(SQLSchema)resultChooser.getSchemaComboBox().getSelectedItem(),
        			SQLDatabase.class );
        } else {
        	resultTableParentDB = (SQLDatabase)resultChooser.getDb();
        }

        String trimedresultTableName = resultTableName.getText().trim();
        if ( trimedresultTableName == null || trimedresultTableName.length() == 0 ) {
        	trimedresultTableName = "MM_"+match.getName();
        }
        match.setResultTable(new SQLTable(resultTableParentDB,
        		trimedresultTableName,
        		"MatchMaker result table",
        		"TABLE", true));

        match.setFilter(filterPanel.getFilterTextArea().getText());

        if ( !matchId.getText().equals(match.getName()) ) {
        	if ( !swingSession.isThisMatchNameAcceptable(matchId.getText()) ) {
        		JOptionPane.showMessageDialog(getPanel(),
        				"Match name \""+matchId.getText()+
        				"\" exist or invalid. The match has not been saved",
        				"Match name invalid",
        				JOptionPane.ERROR_MESSAGE);
        		return false;
        	}
        	match.setName(matchId.getText());
        }

        logger.debug("Saving Match:" + match.getName());

        if ( !folder.getChildren().contains(match)) {
        	folder.addChild(match);
        }

        MatchMakerDAO<Match> dao = swingSession.getDAO(Match.class);
        dao.save(match);
		return true;

    }

    private void refershButtons() {
    	ValidateResult worst = handler.getWorstValidationStatus();
    	saveAction.setEnabled(true);
		newMatchGroupAction.setEnabled(true);
		runMatchAction.setEnabled(true);

    	if ( worst.getStatus() == Status.FAIL ) {
    		saveAction.setEnabled(false);
    		newMatchGroupAction.setEnabled(false);
    		runMatchAction.setEnabled(false);
    	} else if ( worst.getStatus() == Status.WARN ) {
    		runMatchAction.setEnabled(false);
    	}
    }

    private class MatchNameValidator implements Validator {

		private MatchMakerSwingSession session;

		public MatchNameValidator(MatchMakerSwingSession session) {
    		this.session = session;
		}

		public ValidateResult validate(Object contents) {

			String value = (String)contents;
			if ( value == null || value.length() == 0 ) {
				return ValidateResult.createValidateResult(Status.WARN,
						"Match name is required");
			} else if ( !value.equals(match.getName()) &&
						!session.isThisMatchNameAcceptable(value) ) {
				return ValidateResult.createValidateResult(Status.FAIL,
						"Match name is invalid or already exists.");
			}
			return ValidateResult.createValidateResult(Status.OK, "");
		}
    }

    private class MatchSourceTableValidator implements Validator {

		private MatchMakerSwingSession session;

		public MatchSourceTableValidator(MatchMakerSwingSession session) {
    		this.session = session;
		}

		public ValidateResult validate(Object contents) {

			SQLTable value = (SQLTable)contents;
			if ( value == null ) {
				return ValidateResult.createValidateResult(Status.WARN,
						"Match source table is required");
			} else {
				// TODO: check the table existence here, if does not exist, set
				// warning as well.

			}
			return ValidateResult.createValidateResult(Status.OK, "");
		}
    }

    private class MatchResultTableNameValidator implements Validator {

		private MatchMakerSwingSession session;

		public MatchResultTableNameValidator(MatchMakerSwingSession session) {
    		this.session = session;
		}

		public ValidateResult validate(Object contents) {

			String value = (String)contents;
			if ( value == null || value.length() == 0 ) {
				return ValidateResult.createValidateResult(Status.WARN,
						"Match result table name is required");
			} else {
				// TODO: check the table existence here, if does not exist, set
				// warning as well.
			}
			return ValidateResult.createValidateResult(Status.OK, "");
		}
    }
}