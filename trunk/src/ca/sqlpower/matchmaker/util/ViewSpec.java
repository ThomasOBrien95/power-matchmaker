package ca.sqlpower.matchmaker.util;

/**
 * The ViewSpec class specifies a SQL view object as a fully-qualified view name
 * with a set of three strings that can be concatenated together to form a
 * SELECT statement. It also provides methods for creating, replacing, and
 * dropping the view.
 */
public class ViewSpec extends SQLQuery {
    
	/** the view's name */
	private String name;
    
	/** The jdbc catalog containing the view */
	private String catalog;
    
	/** the jdbc schema containing the view */
	private String schema;
	
	public ViewSpec(){
		
	}
	
	public ViewSpec(String select, String from, String where) {
		super(select,from,where);
	}

	private void create(){
		throw new UnsupportedOperationException("Not yet implemented");
	}
	
	private void drop(){
        throw new UnsupportedOperationException("Not yet implemented");
	}
	
	private void replace(){
        throw new UnsupportedOperationException("Not yet implemented");
	}
	
	private void verifyQuery(){
        throw new UnsupportedOperationException("Not yet implemented");
	}

	public String getCatalog() {
		return catalog;
	}

	public void setCatalog(String catalog) {
		if (this.catalog != catalog) {
			this.catalog = catalog;
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		if (this.name != name) {
			this.name = name;
		}
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		if (this.schema != schema) {
			this.schema = schema;
		}
	}

	/**
	 * duplicate all the properties of the ViewSpec
	 * @return new ViewSpec instance with the same properties
	 */
	public ViewSpec duplicate() {
		ViewSpec spec = new ViewSpec();
		spec.setCatalog(getCatalog()==null?null:new String(getCatalog()));
		spec.setFrom(getFrom()==null?null:new String(getFrom()));
		spec.setName(getName()==null?null:new String(getName()));
		spec.setSchema(getSchema()==null?null:new String(getSchema()));
		spec.setSelect(getSelect()==null?null:new String(getSelect()));
		spec.setWhere(getWhere()==null?null:new String(getWhere()));
		return spec;
	}

}
