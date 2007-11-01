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

import org.apache.log4j.Logger;

import ca.sqlpower.matchmaker.TestingMatchMakerSession;

import junit.framework.TestCase;

public class ConcatMungeStepTest extends TestCase {

	private ConcatMungeStep step;
	
	private MungeStepOutput testInput;
	
	private final Logger logger = Logger.getLogger("testLogger");
	
	protected void setUp() throws Exception {
		super.setUp();
		step = new ConcatMungeStep(new TestingMatchMakerSession());
	}

	public void testCallConcatTwoStrings() throws Exception {
		testInput = new MungeStepOutput<String>("test1", String.class);
		testInput.setData("abc");
		step.connectInput(0, testInput);
		
		testInput = new MungeStepOutput<String>("test2", String.class);
		testInput.setData("123");
		step.connectInput(1, testInput);
		
		step.open(logger);
		step.call();
		List<MungeStepOutput> results = step.getChildren(); 
		
		MungeStepOutput output = results.get(0);
		String result = (String)output.getData();
		assertEquals("abc123", result);
	}

	
	public void testCallOneStringAndOneNull() throws Exception {
		testInput = new MungeStepOutput<String>("test1", String.class);
		testInput.setData("abc");
		step.connectInput(0, testInput);
		
		testInput = new MungeStepOutput<String>("test2", String.class);
		testInput.setData(null);
		step.connectInput(1, testInput);
		
		step.open(logger);
		step.call();
		List<MungeStepOutput> results = step.getChildren(); 
		
		MungeStepOutput output = results.get(0);
		String result = (String)output.getData();
		assertEquals("abc", result);
	}
	
	public void testCallonNull() throws Exception {
		testInput = new MungeStepOutput<String>("test", String.class);
		testInput.setData(null);
		step.connectInput(0, testInput);
		step.open(logger);
		step.call();
		List<MungeStepOutput> results = step.getChildren(); 
		MungeStepOutput output = results.get(0);
		String result = (String)output.getData();
		assertEquals(null, result);
	}
	
	public void testCallonInteger() throws Exception {
		testInput = new MungeStepOutput<Integer>("test", Integer.class);
		testInput.setData(new Integer(1));
		try {
			step.connectInput(0, testInput);
			fail("UnexpectedDataTypeException was not thrown as expected");
		} catch (UnexpectedDataTypeException ex) {
			// UnexpectedDataTypeException was thrown as expected
		}
	}
}