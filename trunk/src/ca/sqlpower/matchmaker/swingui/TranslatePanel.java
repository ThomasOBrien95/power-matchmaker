package ca.sqlpower.matchmaker.swingui;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import org.apache.log4j.Logger;

import ca.sqlpower.architect.swingui.ArchitectPanel;
import ca.sqlpower.architect.swingui.table.TableModelSearchDecorator;
import ca.sqlpower.matchmaker.MatchMakerTranslateGroup;
import ca.sqlpower.matchmaker.MatchMakerTranslateWord;
import ca.sqlpower.matchmaker.util.EditableJTable;

import com.jgoodies.forms.builder.ButtonStackBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.debug.FormDebugPanel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class TranslatePanel implements ArchitectPanel {
	
	private static final Logger logger = Logger.getLogger(TranslatePanel.class);
	
    private final MatchMakerSwingSession swingSession;

    private JTable translateTable;
	private JPanel translatePanel;
	private JComboBox translationGroup;
	private JTextField searchGroup;
	private JButton createGroup;
	private JButton deleteGroup;
	private JButton copyGroup;
	private JButton moveItemUp;
	private JButton moveItemDown;
	private JButton moveItemToTop;
	private JButton moveItemToBottom;
	private JScrollPane tableScrollPane;
	private TableModelSearchDecorator tms;

	
	public TranslatePanel(MatchMakerSwingSession swingSession) {
        this.swingSession = swingSession;
		buildUI();
	}
	
	private void buildUI(){
		translateTable = new EditableJTable();
		tms = new TableModelSearchDecorator(
                new MatchTranslateTableModel(swingSession.getTranslations().get(0)));
		tms.setTableTextConverter((EditableJTable) translateTable);
		translateTable.setModel(tms);
		translationGroup = new JComboBox();
		translationGroup.setModel(new TranslationComboBoxModel(swingSession));
		if (translationGroup.getModel().getSize() > 0) {
			translationGroup.setSelectedIndex(0);
		}
		translationGroup.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tms.setTableModel(new MatchTranslateTableModel(
                        (MatchMakerTranslateGroup) translationGroup.getSelectedItem()));
                tms.fireTableStructureChanged();
            }
        });
		searchGroup = new JTextField();
		tms.setDoc(searchGroup.getDocument());
		createGroup = new JButton(createGroupAction);
		deleteGroup = new JButton(deleteGroupAction);
		copyGroup = new JButton(copyGroupAction);
		moveItemUp = new JButton(moveItemUpAction);
		moveItemDown = new JButton(moveItemDownAction);
		moveItemToTop = new JButton(moveItemTopAction);
		moveItemToBottom = new JButton (moveItemBottomAction);
		
		tableScrollPane = new JScrollPane(translateTable);
		
		translateTable.setDragEnabled(true);
		
		FormLayout layout = new FormLayout(
				"4dlu,pref,4dlu,fill:120dlu:grow,4dlu, pref, 10dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu,pref,4dlu");				
		PanelBuilder pb;
		JPanel p = logger.isDebugEnabled() ? new FormDebugPanel(layout) : new JPanel(layout);
		pb = new PanelBuilder(layout, p);
		CellConstraints cc = new CellConstraints();
		
		pb.appendRow("2dlu");
		pb.appendRow("pref");
		pb.add(new JLabel("Group Name:"), cc.xy(2,2));
		pb.add(searchGroup, cc.xy(4,2));
		pb.add(createGroup, cc.xy(8,2));
		pb.add(deleteGroup, cc.xy(10,2));
		pb.appendRow("4dlu");
		pb.appendRow("pref");
		pb.add(copyGroup, cc.xy(8,4));
		pb.add(translationGroup, cc.xy(4,4));
		pb.appendRow("4dlu");
		pb.appendRow("fill:80dlu:grow");
		pb.add(tableScrollPane, cc.xyw(2,6,10,"f,f"));
		
		ButtonStackBuilder bsb = new ButtonStackBuilder();
		bsb.addGridded(moveItemToTop);
		bsb.addRelatedGap();
		bsb.addGlue();
		bsb.addGridded(moveItemUp);
		bsb.addRelatedGap();
		bsb.addGlue();
		bsb.addGridded(moveItemDown);
		bsb.addRelatedGap();
		bsb.addGlue();
		bsb.addGridded(moveItemToBottom);
		bsb.addRelatedGap();
		bsb.addGlue();
		
		pb.add(bsb.getPanel(), cc.xy(14, 6,"c,c"));
		pb.appendRow("4dlu");
		pb.appendRow("pref");
		translatePanel = pb.getPanel();
	}
	
	
	
	public boolean applyChanges() {
		return true;
	}

	public void discardChanges() {
        // not necessary
	}

	public JComponent getPanel() {
		return translatePanel;
	}
	
	public JTable getTranslateTable() {
		return translateTable;
	}
    
    
	////////////Action variables///////////////////////
	
	Action createGroupAction = new AbstractAction("Create Group"){

		public void actionPerformed(ActionEvent e) {
			//	TODO Auto-generated method stub
            JOptionPane.showMessageDialog(translatePanel, "Not implemented yet.");
		}		
	};
	
	Action deleteGroupAction = new AbstractAction("Delete Group"){

		public void actionPerformed(ActionEvent e) {
			//the index is one before the selectedcolumn integer
			if (translateTable.getSelectedRow() >= 0){
				swingSession.getTranslations().remove(translateTable.getSelectedRow());
			}
		}
		
	};
	
	Action copyGroupAction = new AbstractAction("Copy Group"){

		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
            JOptionPane.showMessageDialog(translatePanel, "Not implemented yet.");
		}
		
	};
	
	Action addCommonWordsAction = new AbstractAction("Add Common Words"){

		public void actionPerformed(ActionEvent e) {
		    //TODO: should probably have a better implementation than this
		    JOptionPane.showMessageDialog(translatePanel, "Not implemented yet.");
		}
		
	};
	
	Action helpAction = new AbstractAction("Help"){
		public void actionPerformed(ActionEvent e){
            //TODO
            JOptionPane.showMessageDialog(translatePanel, "Not implemented yet.");
		}
	};
	
	Action moveItemUpAction = new AbstractAction("^"){
		public void actionPerformed(ActionEvent e){
			final int index = getTranslateTable().getSelectedRow();
			if (index >=0 && index < translateTable.getRowCount() ){
				if (getTranslateTable().getSelectedRowCount() == 1 && index > 0){						
					Collections.swap(getTranslations()
									, (index - 1), index);
					translateTable.setRowSelectionInterval(index-1, index-1);
					scrollToSelected(index-1);
				}
			}
		}
	};
	
	Action moveItemDownAction = new AbstractAction("v"){
		public void actionPerformed(ActionEvent e) {
			final int index = getTranslateTable().getSelectedRow();
			if (index >=0 && index < translateTable.getRowCount() ){
				if (getTranslateTable().getSelectedRowCount() == 1 && index < (translateTable.getRowCount() -1) ){						
					Collections.swap( getTranslations()
									, (index + 1), index);

				 	translateTable.setRowSelectionInterval(index+1, index+1);
				 	scrollToSelected(index+1);
				}
			}
		}	
	};
	
	Action moveItemTopAction = new AbstractAction("^^"){
		public void actionPerformed(ActionEvent e){
			final int index = getTranslateTable().getSelectedRow();
			if (index >=0 && index < translateTable.getRowCount() ){
				if (getTranslateTable().getSelectedRowCount() == 1 && index > 0){
					List<MatchMakerTranslateWord> translateList= getTranslations();
                    MatchMakerTranslateWord selectedTranslate=translateList.get(index);
					translateList.remove(index);
					translateList.add(0, selectedTranslate);
					translateTable.setRowSelectionInterval(0,0);
					scrollToSelected(0);
				}
			}
		}
	};
	
	Action moveItemBottomAction = new AbstractAction("vv"){
		public void actionPerformed(ActionEvent e) {
			final int index = getTranslateTable().getSelectedRow();
			if (index >=0 && index < translateTable.getRowCount() ){
				if (getTranslateTable().getSelectedRowCount() == 1 && index < (translateTable.getRowCount() -1) ){						
					List <MatchMakerTranslateWord> translateList=  getTranslations();
                    MatchMakerTranslateWord selectedTranslate=translateList.get(index);
					translateList.remove(index);
					translateList.add(translateList.size(), selectedTranslate);
					translateTable.setRowSelectionInterval(translateList.size()-1,translateList.size()-1);
					scrollToSelected(translateList.size()-1);
				}
			}
		}

			
	};
	private List<MatchMakerTranslateWord> getTranslations() {
		return ((MatchMakerTranslateGroup)translationGroup.getSelectedItem()).getChildren();
	}
	
	/**
	 * By giving it the location of a certain column in the table, it scrolls through the  
	 * translateTable to make sure that the column is visible.
	 * 
	 * @param index - the index location of the column you want to focus on in the translateTable
	 */
	private void scrollToSelected(int index){
		Rectangle cellRect = translateTable.getCellRect(index,0, false);
		if (!translateTable.getVisibleRect().getBounds().contains(cellRect)){	
			translateTable.scrollRectToVisible(cellRect);
		}
	}
	
	

}
