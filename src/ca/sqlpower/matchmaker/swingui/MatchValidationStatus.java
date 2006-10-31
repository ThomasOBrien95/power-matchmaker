package ca.sqlpower.matchmaker.swingui;

import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.RowSet;
import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import org.apache.log4j.Logger;

import ca.sqlpower.architect.ddl.DDLUtils;
import ca.sqlpower.architect.swingui.ASUtils;
import ca.sqlpower.architect.swingui.CommonCloseAction;
import ca.sqlpower.matchmaker.RowSetModel;
import ca.sqlpower.matchmaker.hibernate.PlMatch;
import ca.sqlpower.matchmaker.util.HibernateUtil;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.debug.FormDebugPanel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.sun.rowset.CachedRowSetImpl;

public class MatchValidationStatus extends JDialog {

	private static final Logger logger = Logger.getLogger(MatchValidationStatus.class);
	private PlMatch match;
	private final JTable status = new JTable();

	public MatchValidationStatus(PlMatch match, JFrame frameParent) throws HeadlessException {
		super(frameParent,"View Match Validation Status");
		setMatch(match);
		createUI();
	}
    
    public MatchValidationStatus(PlMatch match, JDialog dialogParent) throws HeadlessException {
        super(dialogParent,"View Match Validation Status");
        setMatch(match);
        createUI();
    }

	public PlMatch getMatch() {
		return match;
	}

	public void setMatch(PlMatch match) {
		if (this.match != match) {
			firePropertyChange("this.match", this.match, match);
			this.match = match;
		}
	}


	private RowSet getMatchStats(PlMatch match) throws SQLException {
    	Connection con = null;
    	Statement stmt = null;
    	ResultSet rs =  null;

    	try {
    		con = HibernateUtil.primarySession().connection();
    		StringBuffer sql = new StringBuffer();
    		sql.append("SELECT GROUP_ID,MATCH_PERCENT,MATCH_STATUS");
    		sql.append(",COUNT(*)/2");
    		sql.append(" FROM  ");
    		sql.append(DDLUtils.toQualifiedName(match.getResultsTableCatalog(),
    				match.getResultsTableOwner(),
    				match.getResultsTable()));
    		sql.append(" GROUP BY GROUP_ID,MATCH_PERCENT,MATCH_STATUS");
    		sql.append(" ORDER BY MATCH_PERCENT DESC,MATCH_STATUS");

    		PreparedStatement pstmt = con.prepareStatement(sql.toString());
    		rs = pstmt.executeQuery();
    		CachedRowSetImpl crset = new CachedRowSetImpl();
    		crset.setReadOnly(true);
    		crset.populate(rs);
    		return crset;
    	} finally {
    		if ( rs != null )
    			rs.close();
    		if ( stmt != null )
    			stmt.close();
    		if (con != null)
    			con.close();
    	}
    }
	private class MatchStatsTableModel extends AbstractTableModel {

		private AbstractTableModel model;

		public MatchStatsTableModel(AbstractTableModel model) {
			this.model = model;
		}
		public int getRowCount() {
			return model.getRowCount();
		}

		public int getColumnCount() {
			return model.getColumnCount();
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			return model.getValueAt(rowIndex,columnIndex);
		}

		@Override
		public String getColumnName(int column) {
			if ( column == 0 ) {
				return "Group";
			} else if ( column == 1 ) {
				return "Match Percent";
			} else if ( column == 2 ) {
				return "Match Status";
			} else if ( column == 3 ) {
				return "Row Count";
			} else {
				return "unknown column";
			}
		}
	}


	private void createUI() {

		DefaultComboBoxModel model = new DefaultComboBoxModel(MatchMakerFrame.getMainInstance().getMatches().toArray());
		final JComboBox matchComboBox = new JComboBox(model);
		matchComboBox.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e) {
				RowSetModel rsm = null;
				try {
					PlMatch selectedMatch = (PlMatch) matchComboBox.getSelectedItem();
					rsm = new RowSetModel(getMatchStats(selectedMatch));
					MatchStatsTableModel model = new MatchStatsTableModel(rsm);
					status.setModel(model);
					status.setName(DDLUtils.toQualifiedName(
							selectedMatch.getResultsTableCatalog(),
							selectedMatch.getResultsTableOwner(),
							selectedMatch.getResultsTable()));
				} catch (SQLException e1) {
					ASUtils.showExceptionDialog(MatchValidationStatus.this,
							"Unknown SQL Error", e1);
				}
			}});

		FormLayout layout = new FormLayout(
                "10dlu,fill:400dlu:grow, 10dlu",
         //		 1    2                  3    4                 5     6     7    8     9     10    11    12   13
                "10dlu,12dlu,4dlu,20dlu,10dlu,fill:250dlu:grow,4dlu,20dlu,4dlu");
        //		 1     2     3    4     5     6                7    8     9     10    11    12   13
        PanelBuilder pb;
        JPanel p = logger.isDebugEnabled() ? new FormDebugPanel(layout) : new JPanel(layout);
        pb = new PanelBuilder(layout, p);

        CellConstraints cc = new CellConstraints();

        pb.add(new JLabel("Match:"), cc.xy(2,2,"l,c"));
		pb.add(matchComboBox, cc.xy(2,4,"l,c"));

		pb.add(new JScrollPane(status), cc.xy(2,6,"f,f"));

        JButton save = new JButton(new AbstractAction("Save"){
			public void actionPerformed(ActionEvent e) {
				new JTableExporter(MatchValidationStatus.this,status );
			}});
        JButton close = new JButton(new CommonCloseAction(MatchValidationStatus.this));
        close.setText("Close");

        ButtonBarBuilder bb1 = new ButtonBarBuilder();
        bb1.addRelatedGap();
        bb1.addGridded(save);
        bb1.addRelatedGap();
        bb1.addGlue();
        bb1.addGridded(close);
        bb1.addRelatedGap();
        pb.add(bb1.getPanel(), cc.xy(2,8));

        getContentPane().add(pb.getPanel());
        if ( match != null ) {
        	matchComboBox.setSelectedItem(match);
        }
	}

}
