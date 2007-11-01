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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.mail.MessagingException;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import ca.sqlpower.architect.ArchitectException;
import ca.sqlpower.architect.ddl.DDLUtils;
import ca.sqlpower.matchmaker.munge.MungeProcess;
import ca.sqlpower.matchmaker.munge.MungeProcessor;

/**
 * The MatchMaker's cleansing engine.  Runs all of the munge steps in the correct
 * order for each row of input.
 */
public class CleanseEngineImpl extends AbstractEngine {

	private static final Logger logger = Logger.getLogger(CleanseEngineImpl.class);

	private int jobSize;

	private int progress;

	private MungeProcessor munger;

	private String progressMessage;
	
	private Processor currentProcessor;
	
	public CleanseEngineImpl(MatchMakerSession session, Project project) {
		this.setSession(session);
		this.setProject(project);
	}

	public void checkPreconditions() throws EngineSettingException, ArchitectException {
		MatchMakerSession session = getSession();
        Project project = getProject();
        final MatchMakerSessionContext context = session.getContext();
        final MungeSettings settings = project.getMungeSettings();
        
        if ( context == null ) {
        	throw new EngineSettingException(
        			"PreCondition failed: session context must not be null");
        }
        
        if ( session.getDatabase() == null ) {
        	throw new EngineSettingException(
        			"PreCondition failed: database of the session must not be null");
        }
        if ( session.getDatabase().getDataSource() == null ) {
        	throw new EngineSettingException(
        			"PreCondition failed: data source of the session must not be null");
        }
        
        if (!Project.doesSourceTableExist(session, project)) {
            throw new EngineSettingException(
                    "Your project source table \""+
                    DDLUtils.toQualifiedName(project.getSourceTable())+
            "\" does not exist");
        }
        
        if (!session.canSelectTable(project.getSourceTable())) {
            throw new EngineSettingException(
            "PreCondition failed: can not select project source table");
        }

        if (settings.getSendEmail()) {
        	// First checks the email settings
        	if (!validateEmailSetting(context)) {
        		throw new EngineSettingException(
        				"missing email setting information," +
        				" the email sender requires smtp host name and" +
        		" smtp localhost name!");
        	}
        	
        	// Then tries to setup the emails to each status
        	try {
				setupEmails(context);
			} catch (Exception e) {
				throw new EngineSettingException("PreCondition failed: " +
						"error while setting up for sending emails.", e);
			}
        }

        if (!canWriteLogFile(settings)) {
            throw new EngineSettingException("The log file is not writable.");
        }
	} 
 
	/**
	 * Returns the logger for the MatchEngineImpl class.
	 */
	public Logger getLogger() {
		return logger;
	}
	
	@Override
	public EngineInvocationResult call() throws EngineSettingException {
		Level oldLevel = logger.getLevel();
		cancelled = false;
		FileAppender fileAppender = null;
		try {
			logger.setLevel(getMessageLevel());
			setFinished(false);
			setStarted(true);
			progress = 0;
			progressMessage = "Checking Cleanse Engine Preconditions";
			logger.info(progressMessage);
			
			try {
				checkPreconditions();
			} catch (ArchitectException e) {
				throw new RuntimeException(e);
			}
			
			String logFilePath = getProject().getMungeSettings().getLog().getAbsolutePath();
			boolean appendToFile = getProject().getMungeSettings().getAppendToLog();
			fileAppender = new FileAppender(new PatternLayout("%d %p %m\n"), logFilePath, appendToFile);
			logger.addAppender(fileAppender);
			
			progressMessage = "Starting Cleanse Engine";
			logger.info(progressMessage);
			
			Integer processCount = getProject().getMungeSettings().getProcessCount();
			int rowCount;
			if (processCount == null || processCount == 0) {
				Connection con = null;
				Statement stmt = null;
				try {
					con = getSession().getConnection();
					stmt = con.createStatement();
					String rowCountSQL = "SELECT COUNT(*) AS ROW_COUNT FROM " + DDLUtils.toQualifiedName(getProject().getSourceTable());
					ResultSet result = stmt.executeQuery(rowCountSQL);
					logger.debug("Getting source table row count with SQL statment " + rowCountSQL);
					result.next();
					rowCount = result.getInt("ROW_COUNT");
				} finally {
					if (stmt != null) stmt.close();
					if (con != null) con.close();
				}
			} else {
				rowCount = processCount.intValue();
			}
			
			List<MungeProcess> processes = new ArrayList<MungeProcess>();
			for (MungeProcess mp: getProject().getMungeProcessesFolder().getChildren()) {
				if (mp.getActive()) {
					processes.add(mp);
				}
			}
			
			jobSize = rowCount * processes.size();
			
			
			for (MungeProcess currentProcess: processes) {
				munger = new MungeProcessor(currentProcess, logger);
				currentProcessor = munger;
				progressMessage = "Running cleanse process " + currentProcess.getName();
				logger.debug(getMessage());
				munger.call();
				if (cancelled) {
					throw new UserAbortException();
				}
				progress += munger.getProgress();

			}
			
			currentProcessor = null;
			
			progressMessage = "Cleanse Engine finished successfully";
			logger.info(progressMessage);
			
			if (getProject().getMungeSettings().getSendEmail()) {
				try {
					greenEmail.setEmailSubject("Cleanse Engine success!");
					greenEmail.setEmailBody("Cleanse Engine finished successfully.");
					greenEmail.sendMessage();
				} catch (MessagingException e) {
					logger.error("Sending emails failed: ", e);
				}
			}
			
			return EngineInvocationResult.SUCCESS;
		} catch (UserAbortException uce) {
			//TODO: I don't know, clean up something?
			logger.info("Cleanse engine terminated by user");
			return EngineInvocationResult.ABORTED;
		} catch (Exception ex) {
			progressMessage = "Cleanse Engine failed";
			logger.error(getMessage());
			
			if (getProject().getMungeSettings().getSendEmail()) {
				try {
					redEmail.setEmailSubject("Cleanse Engine failed!");
					redEmail.setEmailBody("Cleanse Engine failed because: \n" +
						ex.getMessage());
					redEmail.sendMessage();
				} catch (MessagingException e) {
					logger.error("Sending emails failed: ", e);
				}
			}
			
			throw new RuntimeException(ex);
		} finally {
			logger.setLevel(oldLevel);
			setFinished(true);
			if (fileAppender != null) {
				logger.removeAppender(fileAppender);
			}
		}
	
	}

	///////// Monitorable support ///////////
	
	/**
	 * Right now the job size is always indeterminant
	 */
	
	@Override
	public Integer getJobSize() {
		return jobSize;
	}

	@Override
	public String getMessage() {
		return getProgress() + "/" + jobSize + ": " + progressMessage;
	}

	public int getProgress() {
		if (currentProcessor != null) {
			return progress + currentProcessor.getProgress();
		} else {
			return progress;
		}
	}
	
	public synchronized void setCancelled(boolean cancelled) {
		super.setCancelled(cancelled);
		this.cancelled = cancelled;
		if (cancelled && currentProcessor != null) {
			currentProcessor.setCancelled(true);
		}
		
	}
}