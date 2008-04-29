/*
 * Copyright (c) 2008, SQL Power Group Inc.
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

package ca.sqlpower.matchmaker.dao.xml;

import java.io.InputStream;
import java.io.OutputStream;

public interface IOHandler {
    
    /**
     * Returns an input stream containing all project definitions.
     * Later, we'll want a variant of this method that only returns
     * certain projects.
     */
    InputStream createInputStream();
    
    /**
     * Returns an output stream where any project definitions can be written
     * (as a single XML document).
     */
    OutputStream createOutputStream();
}
