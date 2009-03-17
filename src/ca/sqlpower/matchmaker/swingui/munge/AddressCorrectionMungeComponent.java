/*
 * Copyright (c) 2009, SQL Power Group Inc.
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

package ca.sqlpower.matchmaker.swingui.munge;

import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JPanel;

import ca.sqlpower.matchmaker.MatchMakerSession;
import ca.sqlpower.matchmaker.munge.MungeStep;
import ca.sqlpower.validation.swingui.FormValidationHandler;

public class AddressCorrectionMungeComponent extends AbstractMungeComponent {

	private JButton showAllButton;
	private JButton hideAllButton;
	
	public AddressCorrectionMungeComponent(MungeStep step,
			FormValidationHandler handler, MatchMakerSession s) {
		super(step, handler, s);
	}

	@Override
	protected JPanel buildUI() {
		showAllButton = new JButton(new HideShowAllLabelsAction("Show All", true, true, true));
		hideAllButton = new JButton(new HideShowAllLabelsAction("Hide All", true, true, false));
		JPanel content = new JPanel(new FlowLayout());
		content.add(showAllButton);
		content.add(hideAllButton);
				
		setOutputShowNames(true);				
		setInputShowNames(true);
		return content;
	}

}