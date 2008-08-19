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


package ca.sqlpower.matchmaker;

import java.util.Date;

public class MungeSettingsTest extends MatchMakerTestCase {

	MungeSettings ms;
	protected void setUp() throws Exception {
		super.setUp();
		ms = new MungeSettings();
	}

	@Override
	protected MatchMakerObject getTarget() {
		return ms;
	}

    public void testSetLastRunDateDefensive() {
        Date myDate = new Date();
        ms.setLastRunDate(myDate);
        assertEquals(myDate, ms.getLastRunDate());
        assertNotSame(myDate, ms.getLastRunDate());
    }
}