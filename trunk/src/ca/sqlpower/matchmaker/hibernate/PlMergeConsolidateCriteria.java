package ca.sqlpower.matchmaker.hibernate;
// Generated Sep 18, 2006 4:34:38 PM by Hibernate Tools 3.2.0.beta7


import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.beanutils.BeanUtils;

/**
 * PlMergeConsolidateCriteria generated by hbm2java, but maintained by hand
 */
public class PlMergeConsolidateCriteria  implements java.io.Serializable {

    // Fields

     private PlMergeConsolidateCriteriaId id;
     private PlMatch plMatch;
     private String columnFormat;
     private BigDecimal columnLength;
     private boolean canUpdateActionInd;
     private String actionType;
     private Date lastUpdateDate;
     private String lastUpdateUser;
     private String lastUpdateOsUser;

     // Constructors

    /** default constructor */
    public PlMergeConsolidateCriteria() {
    }

	/** minimal constructor */
    public PlMergeConsolidateCriteria(PlMergeConsolidateCriteriaId id, PlMatch plMatch) {
        this.id = id;
        this.plMatch = plMatch;
    }
    /** full constructor */
    public PlMergeConsolidateCriteria(PlMergeConsolidateCriteriaId id, PlMatch plMatch, String columnFormat, BigDecimal columnLength, boolean canUpdateActionInd, String actionType, Date lastUpdateDate, String lastUpdateUser, String lastUpdateOsUser) {
       this.id = id;
       this.plMatch = plMatch;
       this.columnFormat = columnFormat;
       this.columnLength = columnLength;
       this.canUpdateActionInd = canUpdateActionInd;
       this.actionType = actionType;
       this.lastUpdateDate = lastUpdateDate;
       this.lastUpdateUser = lastUpdateUser;
       this.lastUpdateOsUser = lastUpdateOsUser;
    }

    public PlMergeConsolidateCriteria copyOf() {
    	try {
    		PlMergeConsolidateCriteria copy = (PlMergeConsolidateCriteria) BeanUtils.cloneBean(this);

    		// Now copy the non-trivial parts
    		copy.id = id.copyOf();
    		return copy;
    	} catch (Exception e) {
    		throw new RuntimeException("Could not copy");
    	}
	}

	// Property accessors
    public PlMergeConsolidateCriteriaId getId() {
        return this.id;
    }

    public void setId(PlMergeConsolidateCriteriaId id) {
        this.id = id;
    }
    public PlMatch getPlMatch() {
        return this.plMatch;
    }

    public void setPlMatch(PlMatch plMatch) {
        this.plMatch = plMatch;
    }
    public String getColumnFormat() {
        return this.columnFormat;
    }

    public void setColumnFormat(String columnFormat) {
        this.columnFormat = columnFormat;
    }
    public BigDecimal getColumnLength() {
        return this.columnLength;
    }

    public void setColumnLength(BigDecimal columnLength) {
        this.columnLength = columnLength;
    }
    public boolean isCanUpdateActionInd() {
        return this.canUpdateActionInd;
    }

    public void setCanUpdateActionInd(boolean canUpdateActionInd) {
        this.canUpdateActionInd = canUpdateActionInd;
    }
    public String getActionType() {
        return this.actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }
    public Date getLastUpdateDate() {
        return this.lastUpdateDate;
    }

    public void setLastUpdateDate(Date lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }
    public String getLastUpdateUser() {
        return this.lastUpdateUser;
    }

    public void setLastUpdateUser(String lastUpdateUser) {
        this.lastUpdateUser = lastUpdateUser;
    }
    public String getLastUpdateOsUser() {
        return this.lastUpdateOsUser;
    }

    public void setLastUpdateOsUser(String lastUpdateOsUser) {
        this.lastUpdateOsUser = lastUpdateOsUser;
    }



    /* for xml parser, overwrite all method that don't take String parameter,
	 * also create id when set matchId or groupId
	 *
	 */
	public void setMatchId(String id) {
		if ( this.id == null ) {
			this.id = new PlMergeConsolidateCriteriaId();
		}
		this.id.setMatchId(id);
	}
	public void setTableCatalog(String id) {
		if ( this.id == null ) {
			this.id = new PlMergeConsolidateCriteriaId();
		}
		this.id.setCatalog(id);
	}
	public void setTableOwner(String id) {
		if ( this.id == null ) {
			this.id = new PlMergeConsolidateCriteriaId();
		}
		this.id.setOwner(id);
	}
	public void setTableName(String id) {
		if ( this.id == null ) {
			this.id = new PlMergeConsolidateCriteriaId();
		}
		this.id.setTableName(id);
	}
	public void setColumnName(String id) {
		if ( this.id == null ) {
			this.id = new PlMergeConsolidateCriteriaId();
		}
		this.id.setColumnName(id);
	}
	public void setLastUpdateDate(String val) throws ParseException {
		if ( val != null && val.length()>0 && !val.equalsIgnoreCase("null")) {
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
			setLastUpdateDate(df.parse(val));
		}
	}
	public void setCanUpdateActionInd(String val) {
		if ( val != null && val.length()>0 && !val.equalsIgnoreCase("null")) {
			setCanUpdateActionInd(val.charAt(0) == 'y' || val.charAt(0) == 'Y');
		}
	}
	public void setColumnLength(String val) {
		if ( val != null && val.length()>0 && !val.equalsIgnoreCase("null")) {
			setColumnLength(BigDecimal.valueOf(Long.valueOf(val)));
		}
	}


}


