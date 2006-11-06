package ca.sqlpower.matchmaker.util.log;

import java.util.List;

public interface Log {
	/**
	 * Return the persistence associated with this log;
	 * the object may be a String (filename), or it may be
	 * something quite different in future
	 * (it will always implement a user-meaningful toString()).
	 */
	public Object getConstraint();

	/**
	 *  True if the log contents can be read back
	 */
	public boolean isReadable();
	/**
	 * True if you can write to the log via the log() methods
	 */
	public boolean isWritable();

	/**
	 * The size of the log contents, if known.
	 */
	public long size();

	/**
	 * log the message
	 * @param level the severity of the message
	 * @param message the message to be logged
	 */
	public void log(Level level,Object message );
	/**
	 *  Log a message with an exception
	 */
	public void log(Level level,Object message, Throwable t);

	/** truncate the log */
	public void truncate();

	/** if readable, return the log broken up by messages */
	public List<String> readAsList();

	/** close the log file */
	public void close();


}
