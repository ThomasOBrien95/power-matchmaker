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

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import ca.sqlpower.matchmaker.MatchMakerObject;
import ca.sqlpower.matchmaker.MatchMakerSession;
import ca.sqlpower.matchmaker.event.MatchMakerListener;

/**
 * A non-functional implementation of MungeStep for the benefit of test cases.
 */
public class StubMungeStep implements MungeStep {

    private static final Logger logger = Logger.getLogger(StubMungeStep.class);
    
    public int addInput(InputDescriptor desc) {
        logger.debug("Stub call: StubMungeStep.addInput()");
        return 0;
    }

    public Boolean call() throws Exception {
        logger.debug("Stub call: StubMungeStep.call()");
        return null;
    }

    public boolean canAddInput() {
        logger.debug("Stub call: StubMungeStep.canAddInput()");
        return false;
    }

    public void close() throws Exception {
        logger.debug("Stub call: StubMungeStep.close()");
    }

    public void connectInput(int index, MungeStepOutput o) {
        logger.debug("Stub call: StubMungeStep.connectInput()");
    }

    public void disconnectInput(int index) {
        logger.debug("Stub call: StubMungeStep.disconnectInput()");
    }

    public InputDescriptor getInputDescriptor(int inputNumber) {
        logger.debug("Stub call: StubMungeStep.getInputDescriptor()");
        return null;
    }

    public List<MungeStepOutput> getMSOInputs() {
        logger.debug("Stub call: StubMungeStep.getInputs()");
        return null;
    }

    public String getParameter(String name) {
        logger.debug("Stub call: StubMungeStep.getParameter()");
        return null;
    }

    public Collection<String> getParameterNames() {
        logger.debug("Stub call: StubMungeStep.getParameterNames()");
        return null;
    }

    public MungeProcess getParent() {
        logger.debug("Stub call: StubMungeStep.getParent()");
        return null;
    }

    public void open(Logger logger) throws Exception {
        logger.debug("Stub call: StubMungeStep.open()");
    }

    public void removeInput(int index) {
        logger.debug("Stub call: StubMungeStep.removeInput()");
    }

    public void setParameter(String name, String newValue) {
        logger.debug("Stub call: StubMungeStep.setParameter()");
    }
    

	public void setParameter(String name, boolean newValue) {
		logger.debug("Stub call: StubMungeStep.setParameter()");
		
	}

	public void setParameter(String name, int newValue) {
		logger.debug("Stub call: StubMungeStep.setParameter()");
		
	}

    public void addChild(MungeStepOutput child) {
        logger.debug("Stub call: StubMungeStep.addChild()");
    }

    public void addMatchMakerListener(MatchMakerListener<MungeStep, MungeStepOutput> l) {
        logger.debug("Stub call: StubMungeStep.addMatchMakerListener()");
    }

    public boolean allowsChildren() {
        logger.debug("Stub call: StubMungeStep.allowsChildren()");
        return false;
    }

    public MungeStep duplicate(MatchMakerObject parent, MatchMakerSession session) {
        logger.debug("Stub call: StubMungeStep.duplicate()");
        return null;
    }

    public int getChildCount() {
        logger.debug("Stub call: StubMungeStep.getChildCount()");
        return 0;
    }

    public List<MungeStepOutput> getChildren() {
        logger.debug("Stub call: StubMungeStep.getChildren()");
        return null;
    }

    public String getName() {
        logger.debug("Stub call: StubMungeStep.getName()");
        return null;
    }

    public MatchMakerSession getSession() {
        logger.debug("Stub call: StubMungeStep.getSession()");
        return null;
    }

    public void removeChild(MungeStepOutput child) {
        logger.debug("Stub call: StubMungeStep.removeChild()");
    }

    public void removeMatchMakerListener(MatchMakerListener<MungeStep, MungeStepOutput> l) {
        logger.debug("Stub call: StubMungeStep.removeMatchMakerListener()");
    }

    public void setName(String string) {
        logger.debug("Stub call: StubMungeStep.setName()");
    }

    public void setParent(MatchMakerObject parent) {
        logger.debug("Stub call: StubMungeStep.setParent()");
    }

    public void setSession(MatchMakerSession matchMakerSession) {
        logger.debug("Stub call: StubMungeStep.setSession()");
    }

    public Date getCreateDate() {
        logger.debug("Stub call: StubMungeStep.getCreateDate()");
        return null;
    }

    public String getLastUpdateAppUser() {
        logger.debug("Stub call: StubMungeStep.getLastUpdateAppUser()");
        return null;
    }

    public Date getLastUpdateDate() {
        logger.debug("Stub call: StubMungeStep.getLastUpdateDate()");
        return null;
    }

    public String getLastUpdateOSUser() {
        logger.debug("Stub call: StubMungeStep.getLastUpdateOSUser()");
        return null;
    }

    public void registerUpdate() {
        logger.debug("Stub call: StubMungeStep.registerUpdate()");
    }

	public MungeStepOutput getOutputByName(String name) {
		// TODO Auto-generated method stub
		logger.debug("Stub call: StubMungeStep.getOutputByName()");
		return null;
	}

	public boolean isVisible() {
		return true;
	}

	public void setVisible(boolean v) {
	}

    public boolean isUndoing() {
		return false;
	}

	public void setUndoing(boolean isUndoing) {
	}

	public boolean isInputStep() {
		return false;
	}
}
