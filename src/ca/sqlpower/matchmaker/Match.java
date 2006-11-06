package ca.sqlpower.matchmaker;

import ca.sqlpower.architect.SQLTable;
import ca.sqlpower.matchmaker.util.SourceTable;
import ca.sqlpower.matchmaker.util.ViewSpec;

public class Match extends AbstractMatchMakerObject<MatchMakerFolder> {

	public enum MatchType {
		FIND_DUPES("Find Duplicates"), BUILD_XREF("Build Cross-Reference");

		String displayName;

		private MatchType(String displayName) {
			this.displayName = displayName;
		}

		@Override
		public String toString() {
			return displayName;
		}
	}

	/** The id of this object. It must be unique across all match objects */
	String name;

	/** The type of match */
	MatchType type;

	/** A little note on this match object */
	String description;

	/**
	 * The table where we get the match data.
	 */
	SourceTable sourceTable;

	/**
	 * The table where the engine stores the results of a match
	 */
	SQLTable resultTable;

	/** The settings for the match engine */
	MatchSettings matchSettings;

	/** the settings for the merge engine */
	MergeSettings mergeSettings;

	/** a filter for the tables that are matched */
	String filter;

	/** FIXME can't remember what the view does */
	ViewSpec view;

	/** The point above which matches are done automatically */
	int autoMatchThreshold;

	public Match(String appUserName) {
		super(appUserName);

	}

	public int getAutoMatchThreshold() {
		return autoMatchThreshold;
	}

	public void setAutoMatchThreshold(int autoMatchThreshold) {
		int oldValue = this.autoMatchThreshold;
		this.autoMatchThreshold = autoMatchThreshold;
		getEventSupport().firePropertyChange("autoMatchThreshold", oldValue,
				this.autoMatchThreshold);
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		String oldValue = this.description;
		this.description = description;
		getEventSupport().firePropertyChange("description", oldValue,
				this.description);
	}

	public String getFilter() {
		return filter;
	}

	public void setFilter(String filter) {
		String oldValue = this.filter;
		this.filter = filter;
		getEventSupport().firePropertyChange("filter", oldValue, this.filter);
	}

	public MatchSettings getMatchSettings() {
		return matchSettings;
	}

	public void setMatchSettings(MatchSettings matchSettings) {
		MatchSettings oldValue = this.matchSettings;
		this.matchSettings = matchSettings;
		getEventSupport().firePropertyChange("matchSettings", oldValue,
				this.matchSettings);
	}

	public MergeSettings getMergeSettings() {
		return mergeSettings;
	}

	public void setMergeSettings(MergeSettings mergeSettings) {
		MergeSettings oldValue = this.mergeSettings;
		this.mergeSettings = mergeSettings;
		getEventSupport().firePropertyChange("mergeSettings", oldValue,
				this.mergeSettings);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		String oldValue = this.name;
		this.name = name;
		getEventSupport().firePropertyChange("name", oldValue, this.name);
	}

	public SQLTable getResultTable() {
		return resultTable;
	}

	public void setResultTable(SQLTable resultTable) {
		SQLTable oldValue = this.resultTable;
		this.resultTable = resultTable;
		getEventSupport().firePropertyChange("resultTable", oldValue,
				this.resultTable);
	}

	public SourceTable getSourceTable() {
		return sourceTable;
	}

	public void setSourceTable(SourceTable sourceTable) {
		SourceTable oldValue = this.sourceTable;
		this.sourceTable = sourceTable;
		getEventSupport().firePropertyChange("sourceTable", oldValue,
				this.sourceTable);
	}

	public MatchType getType() {
		return type;
	}

	public void setType(MatchType type) {
		MatchType oldValue = this.type;
		this.type = type;
		getEventSupport().firePropertyChange("type", oldValue, this.type);
	}

	public ViewSpec getView() {
		return view;
	}

	public void setView(ViewSpec view) {
		ViewSpec oldValue = this.view;
		this.view = view;
		getEventSupport().firePropertyChange("view", oldValue, this.view);
	}

}
