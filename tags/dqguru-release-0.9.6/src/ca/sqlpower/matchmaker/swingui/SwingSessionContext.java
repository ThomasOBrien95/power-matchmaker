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

package ca.sqlpower.matchmaker.swingui;

import java.awt.Rectangle;
import java.awt.Window;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import ca.sqlpower.matchmaker.MatchMakerConfigurationException;
import ca.sqlpower.matchmaker.MatchMakerSession;
import ca.sqlpower.matchmaker.MatchMakerSessionContext;
import ca.sqlpower.matchmaker.dao.hibernate.RepositoryVersionException;
import ca.sqlpower.matchmaker.munge.MungeStep;
import ca.sqlpower.matchmaker.swingui.munge.AbstractMungeComponent;
import ca.sqlpower.matchmaker.swingui.munge.StepDescription;
import ca.sqlpower.security.PLSecurityException;
import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.sqlobject.SQLObjectException;
import ca.sqlpower.validation.swingui.FormValidationHandler;

public interface SwingSessionContext extends MatchMakerSessionContext {

    //////// MatchMakerSessionContext implementation //////////
    public MatchMakerSwingSession createSession(JDBCDataSource ds,
			String username, String password) throws PLSecurityException,
			SQLException, SQLObjectException, MatchMakerConfigurationException,
			RepositoryVersionException;

    public List<JDBCDataSource> getDataSources();

    public DataSourceCollection<JDBCDataSource> getPlDotIni();

    public String getLastImportExportAccessPath();

    public void setLastImportExportAccessPath(String lastExportAccessPath);

    /**
     * Returns the previous location for the MatchMaker frame, or some reasonable default
     * if the previous bounds are unknown.
     */
    public Rectangle getFrameBounds();

    /**
     * Stores (persistently) the current position of the main frame, so it can appear
     * in the same location next time you call {@link #getFrameBounds()}.
     */
    public void setFrameBounds(Rectangle bounds);

    /**
     * Remembers the name of the given data source, so you can get it back next
     * time you call getLastLoginDataSource, even if the application is quit and
     * restarted in the interim.
     * 
     * @param dataSource The data source to remember.
     */
    public void setLastLoginDataSource(SPDataSource dataSource);

    /**
     * Returns the last data source passed to setLastLoginDataSource, even if
     * that happened since the application was last restarted.
     */
    public SPDataSource getLastLoginDataSource();

    /**
     * Shows the database connection manager dialog, attaching it to the given parent
     * window.  There will only ever be one created no matter how many times you call
     * this method.
     */
    public void showDatabaseConnectionManager(Window owner);

    /**
     * Shows the login dialog, which can lead to the creation of a new session within
     * this context.
     * 
     * @param selectedDataSource The data source that should be selected in the dialog.
     * If null, the dialog's selected data source will remain unchanged.
     */
    public void showLoginDialog(SPDataSource selectedDataSource);
    
    /**
     * This is the normal way of starting up the MatchMaker GUI. Based on the
     * user's preferences, this method either presents the repository login
     * dialog, or delegates the "create default session" operation to the delegate
     * context.
     * <p>
     * Under normal circumstances, the delegate context will be a
     * MatchMakerHibernateSession, so delegating the operation ends up (creating
     * and) logging into the local HSQLDB repository.
     */
    public void launchDefaultSession();
    
    /**
     * Returns a new instance of the appropriate MungeComponent that is associated with the given MungeStep.
     * Theses are provided in the munge_component.properties in the ca.sqlpower.matchmaker.swingui.munge, and 
     * from the $HOME/.matchmaker/munge_components.properties file.
     *  
     * @param ms The mungeStep to create the component for
     * @param handler A from validation handler to use on the component
     * @param session The current matchmaker session
     * @return A new MungeComponent that goes with the given MungeStep 
     */
    public AbstractMungeComponent getMungeComponent(MungeStep ms, FormValidationHandler handler, 
    		MatchMakerSession session);
    
   /**
    * Returns the list of StepDescriptions.
    * 
    * @return The list
    */
    public Map<Class, StepDescription> getStepMap();

    /**
     * Returns a new instance of the given munge step class.
     * 
     * @param session The session the mungeStep will added to
     * @return A new MungeStep of the type given by the list
     */
    public MungeStep getMungeStep(Class<? extends MungeStep> create);
    
    /**
     * Preference: Should the launchDefaultSession method automatically create
     * the default session, or should it present a login dialog?
     */
    public boolean isAutoLoginEnabled();

    /**
     * Preference: Should the launchDefaultSession method automatically create
     * the default session, or should it present a login dialog?
     */
    public void setAutoLoginEnabled(boolean enabled);

    /**
     * Returns the data source for the auto-login repository.  If {@link #isAutoLoginEnabled()}
     * is true, the MatchMaker will attempt to connect to this data source
     * and use it as the repository.
     */
    public SPDataSource getAutoLoginDataSource();

    /**
     * Sets the data source for the auto-login repository.  If you want
     * auto-login to actually happen when the application starts, you will
     * have to enable it with the flag {@link #setAutoLoginEnabled(boolean)}.
     */
    public void setAutoLoginDataSource(SPDataSource selectedItem);


}