package ca.sqlpower.matchmaker.hibernate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.event.ChangeEvent;

// Generated 19-Sep-2006 12:08:38 PM by Hibernate Tools 3.2.0.beta7



/**
 * PlFolder generated by hbm2java, but maintained by hand
 */
public class PlFolder extends DefaultHibernateObject implements  java.io.Serializable {

    // Fields
     private String folderName;
     private String folderDesc;
     private String folderStatus;
     private Long lastBackupNo;
     private Set<PlMatch> matches = new TreeSet<PlMatch>();

     // Constructors

    /** default constructor */
    public PlFolder() {
    }

	/** minimal constructor */
    public PlFolder(String folderName) {
        this.folderName = folderName;
    }

    /** full constructor */
    public PlFolder(String folderName, String folderDesc, String folderStatus, long lastBackupNo) {
       this.folderName = folderName;
       this.folderDesc = folderDesc;
       this.folderStatus = folderStatus;
       this.lastBackupNo = lastBackupNo;
    }

    /** Copy Constructor */
    public PlFolder(PlFolder orig) {
    	this.folderName = orig.folderName;
        this.folderDesc = orig.folderDesc;
        this.folderStatus = orig.folderStatus;
        this.lastBackupNo = orig.lastBackupNo;
        for (PlMatch match : orig.matches) {
        	matches.add(match.copyOf());
        }
    }



    @Override
    public int getChildCount() {
    	return matches.size();
    }

    @Override
    public List<DefaultHibernateObject> getChildren() {
    	List<DefaultHibernateObject> children = new ArrayList<DefaultHibernateObject>(matches);
    	Collections.sort(children);
    	return children;
    }

    // Property accessors
    public String getFolderName() {
        return this.folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
        fireChangeEvent(new ChangeEvent(this));
    }
    public String getFolderDesc() {
        return this.folderDesc;
    }

    public void setFolderDesc(String folderDesc) {
        this.folderDesc = folderDesc;
    }
    public String getFolderStatus() {
        return this.folderStatus;
    }

    public void setFolderStatus(String folderStatus) {
        this.folderStatus = folderStatus;
    }
    public Long getLastBackupNo() {
        return this.lastBackupNo;
    }

    public void setLastBackupNo(Long lastBackupNo) {
        this.lastBackupNo = lastBackupNo;
    }

	public Set<PlMatch> getMatches() {
		return matches;
	}

	public void setMatches(Set<PlMatch> matches) {
		this.matches = matches;
	}

	@Override
	public String toString() {
		return folderName;
	}

	public int compareTo(Object obj) {
		if (equals(obj)) return 0;
		final PlFolder other = (PlFolder) obj;
		return folderName.compareTo(other.getFolderName());
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((folderName == null) ? 0 : folderName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final PlFolder other = (PlFolder) obj;
		if (folderName == null) {
			if (other.folderName != null)
				return false;
		} else if (!folderName.equals(other.folderName))
			return false;
		return true;
	}

}


