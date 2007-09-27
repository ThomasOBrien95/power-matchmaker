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

package ca.sqlpower.matchmaker.munge;

import java.util.List;

import org.apache.commons.codec.language.Soundex;


/**
 * This munge step will output the soundex code of the given input.
 */
public class SoundexMungeStep extends AbstractMungeStep {

	private MungeStepOutput<String> out;
	
	public SoundexMungeStep() {
		out = new MungeStepOutput<String>("soundexOutput", String.class);
		addChild(out);
		InputDescriptor desc = new InputDescriptor("soundex", String.class);
		super.addInput(desc);
	}
	
	@Override
	public int addInput(InputDescriptor desc) {
		throw new UnsupportedOperationException("Soundex munge step does not support addInput()");
	}
	
	@Override
	public void removeInput(int index) {
		throw new UnsupportedOperationException("Soundex substitution munge step does not support removeInput()");
	}
	
	public void connectInput(int index, MungeStepOutput o) {
		if (index >= getInputs().size()) {
			throw new IndexOutOfBoundsException("There is no input at the given index");
		} else if (o.getType() != getInputDescriptor(index).getType()) {
			throw new UnexpectedDataTypeException(
				"Soundex munge step does not accept non-String inputs");
		} else {
			super.connectInput(index, o);
		}
	}
	
	public List<MungeStepOutput> call() throws Exception {
		MungeStepOutput<String> in = getInputs().get(0);
		String data = in.getData();
		out.setData(new Soundex().soundex(data));
		return getChildren();
	}

	public boolean canAddInput() {
		return false;
	}
}
