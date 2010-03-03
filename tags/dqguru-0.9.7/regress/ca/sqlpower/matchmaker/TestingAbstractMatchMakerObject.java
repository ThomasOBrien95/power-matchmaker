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


package ca.sqlpower.matchmaker;

import java.util.Random;

import ca.sqlpower.matchmaker.event.MatchMakerEventSupport;
import ca.sqlpower.matchmaker.event.MatchMakerListener;

public class TestingAbstractMatchMakerObject
				extends AbstractMatchMakerObject<TestingAbstractMatchMakerObject, MatchMakerObject> {

	int i;

	public TestingAbstractMatchMakerObject( ) {
		Random rand  = new Random();
		i = rand.nextInt();
		MatchMakerSession session = new TestingMatchMakerSession();
		((TestingMatchMakerSession)session).setAppUser("app user name");
		this.setSession(session);
	}

	@Override
	public boolean equals(Object obj) {
		return this==obj;
	}

	@Override
	public int hashCode() {
		return i;
	}

	/**
	 * Made public so test cases can fire specific events on demand.
	 */
	@Override
	public MatchMakerEventSupport
		<TestingAbstractMatchMakerObject, MatchMakerObject> getEventSupport() {
		return super.getEventSupport();
	}

	public boolean hasListener(MatchMakerListener<?,?> listener) {
		return getEventSupport().getListeners().contains(listener);
	}

	public TestingAbstractMatchMakerObject duplicate(MatchMakerObject parent, MatchMakerSession session) {
		return null;
	}
}
