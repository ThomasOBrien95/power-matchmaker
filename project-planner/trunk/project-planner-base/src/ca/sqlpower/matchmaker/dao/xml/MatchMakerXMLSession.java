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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import ca.sqlpower.architect.ArchitectException;
import ca.sqlpower.architect.SQLDatabase;
import ca.sqlpower.architect.SQLTable;
import ca.sqlpower.matchmaker.FolderParent;
import ca.sqlpower.matchmaker.MatchMakerFolder;
import ca.sqlpower.matchmaker.MatchMakerObject;
import ca.sqlpower.matchmaker.MatchMakerSession;
import ca.sqlpower.matchmaker.MatchMakerSessionContext;
import ca.sqlpower.matchmaker.PlFolder;
import ca.sqlpower.matchmaker.Project;
import ca.sqlpower.matchmaker.TranslateGroupParent;
import ca.sqlpower.matchmaker.WarningListener;
import ca.sqlpower.matchmaker.dao.MatchMakerDAO;
import ca.sqlpower.matchmaker.dao.ProjectDAO;
import ca.sqlpower.matchmaker.munge.MungeProcess;
import ca.sqlpower.matchmaker.swingui.SwingSessionContext;
import ca.sqlpower.matchmaker.swingui.SwingSessionContextImpl;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.util.Version;

/**
 * A MatchMakerSession that provides XML persistence to a backend server
 * running somewhere else.
 *
 */
public class MatchMakerXMLSession implements MatchMakerSession {

    private static final Logger logger = Logger.getLogger(MatchMakerXMLSession.class);
    
    /**
     * just temporary for testing. real thing uses client/server communication.
     */
    private final File projectFile = new File("/Users/fuerth/mm-planner-file.xml");
    
    private final MatchMakerSessionContext context;
    private final FolderParent folderParent = new FolderParent(this);
    private final PlFolder<Project> defaultFolder = new PlFolder<Project>();
    private final Date startTime = new Date();
    private List<WarningListener> warningListeners = new ArrayList<WarningListener>();
    private String dbUser = "SQL Power Person";
    
    public MatchMakerXMLSession(MatchMakerSessionContext context) {
        this.context = context;
        folderParent.addChild(defaultFolder);
        for (Project p : getDAO(Project.class).findAll()) {
            logger.debug("Adding " + p + " to default folder!");
            defaultFolder.addChild(p);
        }
        logger.debug("Default folder size: " + defaultFolder.getChildCount());
    }
    
    /**
     * Logs the warning to the log4j logger at WARN level as well as telling all the
     * warning listeners about the warning.
     */
    public void handleWarning(String message) {
        logger.warn("handleWarning: received warning message: "+message);
        synchronized (warningListeners) {
            for (int i = warningListeners.size()-1; i >= 0; i--) {
                warningListeners.get(i).handleWarning(message);
            }
        }
    }

    public void addWarningListener(WarningListener l) {
        synchronized (warningListeners) {
            warningListeners.add(l);
        }
    }

    public void removeWarningListener(WarningListener l) {
        synchronized (warningListeners) {
            warningListeners.remove(l);
        }
    }


    public boolean canSelectTable(SQLTable table) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public long countProjectByName(String name) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String createNewUniqueName() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public PlFolder findFolder(String foldername) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public SQLTable findPhysicalTableByName(String catalog, String schema, String tableName) throws ArchitectException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public SQLTable findPhysicalTableByName(String spDataSourceName, String catalog, String schema, String tableName) throws ArchitectException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String getAppUser() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String getAppUserEmail() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public FolderParent getBackupFolderParent() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Connection getConnection() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public MatchMakerSessionContext getContext() {
        return context;
    }

    public FolderParent getCurrentFolderParent() {
        return folderParent;
    }

    public <T extends MatchMakerObject> MatchMakerDAO<T> getDAO(Class<T> businessClass) {
        if (businessClass == Project.class) {
            IOHandler ioh = new IOHandler() {

                public InputStream createInputStream() {
                    try {
                        FileInputStream in = new FileInputStream(projectFile);
                        return in;
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }

                public OutputStream createOutputStream() {
                    try {
                        return new FileOutputStream(projectFile);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            return (MatchMakerDAO<T>) new ProjectDAOXML(this, ioh);
        } else {
            return null;
        }
    }

    public String getDBUser() {
        return dbUser;
    }

    public SQLDatabase getDatabase() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public SQLDatabase getDatabase(SPDataSource dataSource) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public PlFolder<Project> getDefaultPlFolder() {
        return defaultFolder;
    }

    public Version getPLSchemaVersion() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Project getProjectByName(String name) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Date getSessionStartTime() {
        return startTime;
    }

    public TranslateGroupParent getTranslations() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public boolean isThisProjectNameAcceptable(String name) {
        return true;
    }

    public boolean tableExists(String catalog, String schema, String tableName)
            throws ArchitectException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public boolean tableExists(String spDataSourceName, String catalog,
            String schema, String tableName) throws ArchitectException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public boolean tableExists(SQLTable table) throws ArchitectException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public static void main(String[] args) throws Exception {
        String configName = "log4j.properties";
        URL config = MatchMakerXMLSession.class.getClassLoader().getResource(configName);
        if (config == null) {
            // It is probably not a good idea to change this to use log4j logging...
            System.err.println("Warning: Couldn't find log4j config resource '"+configName+"'");
        } else {
            org.apache.log4j.PropertyConfigurator.configure(config);
            logger.info("Log4J configured successfully");
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    MatchmakerXMLSessionContext context = new MatchmakerXMLSessionContext();
                    SwingSessionContext sscontext = new SwingSessionContextImpl(Preferences.userNodeForPackage(MatchMakerXMLSession.class), context);
                    sscontext.launchDefaultSession();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }

    public void save(MatchMakerObject mmo) {
        if (mmo instanceof Project){
            Project project = (Project)mmo;
            ProjectDAO dao = (ProjectDAO) getDAO(Project.class);
            dao.save(project);
        } else if (mmo instanceof MatchMakerFolder){
            Project project = (Project)mmo.getParent();
            save(project);
        } else if (mmo instanceof PlFolder){
            for (Project project:((PlFolder<Project>)mmo).getChildren()) {
                save(project);
            }
        } else if (mmo instanceof MungeProcess) {
            MungeProcess cg = (MungeProcess)mmo;
            Project project = (Project)cg.getParentProject();
            save(project);
        } else {
            throw new UnsupportedOperationException("We do not yet support "+mmo.getClass() + " persistance");
        }        
    }
}
