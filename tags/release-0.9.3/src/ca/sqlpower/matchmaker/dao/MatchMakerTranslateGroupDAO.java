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

package ca.sqlpower.matchmaker.dao;

import ca.sqlpower.matchmaker.MatchMakerTranslateGroup;

/**
 * The Data access interface for match maker translate group objects
 *
 * At this point this interface only extends the base DAO interface
 * and is put in for future expansion. 
 *
 * Remember to program to this interface rather than an implemenation
 */
public interface MatchMakerTranslateGroupDAO extends MatchMakerDAO<MatchMakerTranslateGroup> {
    /**
     * Finds the Translate Group having the given name (case sensitive).
     * @param name The name of the translate group to look for
     * @return The translate group object with the given name, or null if there
     * is no such translate group.
     */
    public MatchMakerTranslateGroup findByName(String name);
    
    /**
     * Finds the Translate Group having the given oid.
     * @param oid The oid of the translate group to look for
     * @return The translate group object with the given name, or null if there
     * is no such translate group.
     */
    public MatchMakerTranslateGroup findByOID(long oid);
}