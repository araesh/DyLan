/**
 * 
 */
package qmul.ds.formula;

import org.apache.jena.rdf.model.Resource;

/**
 * @author angus, arash
 *
 */
public class RDFVariable extends Variable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3542878573997978134L;
	//Name Space for RDF Variables
	
	protected static final String VAR_NAMESPACE = "http://wallscope.co.uk/ontology/var/";
	
	
	public RDFVariable(String var) {		
		
		super.name = VAR_NAMESPACE + var;
	}
	
	public RDFVariable(Resource var)
	{
		if (var.getURI().startsWith(VAR_NAMESPACE))
			super.name = var.getURI();
		else
			throw new IllegalArgumentException("Doesn't seem to be a variable resource:"+var.getURI());
	}
	
	public String toString()
	{
		return getName();
	}
	
	
	public String getName()
	{
	
		String prefixed=super.getName();
		return prefixed.substring(VAR_NAMESPACE.length(), prefixed.length());
		
	}

	public String getFullName() {
		return this.name;
	}
	
	
	
	
}
