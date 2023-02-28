package qmul.ds.formula.rdf;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Selector;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.ResourceUtils;

import qmul.ds.Context;
import qmul.ds.formula.Formula;
import qmul.ds.formula.Variable;
import qmul.ds.formula.ttr.TTRRecordType;
import qmul.ds.tree.Tree;

/**
 * 
 * @author Arash, Angus
 * 
 *         RDFGraphs are specified in turtle.
 * 
 *         Variables are designated using the prefix 'var', e.g. var:x, var:e,
 *         etc
 * 
 *         Variables don't have DS types, therefore the following conventions
 *         should be followed, otherwise some funcionality might not work:
 * 
 *         Variables of ds type e: x1, x2, x3, ... Variables of ds type es: e1, e2,
 *         e3, ....
 * 
 * 
 *         DS-RDF specific predicates, labels etc. use the prefix 'dsrdf', e.g.
 *         dsrdf:Head
 * 
 *         Best practice is to use available properties, predicates, etc. from
 *         an existing ontology, e.g. schema.org
 * 
 *
 */
public class RDFGraph extends RDFFormula {

	private static final long serialVersionUID = 1L;
	protected Model rdfModel;

	/**
	 * Assuming that RDFGraph is specified in Turtle, and that the spec is enclosed
	 * in {}
	 */
	public static final String RDFGraphPattern = "\\{((.|\\R)+)\\}";

	public static final String DSRDF_NAMESPACE = "http://dsrdf.com/ontology/";
	public static final String DSRDF_EVENT = "@prefix event: <"+ DSRDF_NAMESPACE + "event/> .";
	public static final String DSRDF_PREFIX = "@prefix dsrdf: <" + DSRDF_NAMESPACE + "> .";
	public static final String VAR_PREFIX = "@prefix var: <" + RDFVariable.VAR_NAMESPACE + "> .";
	

	public static final String DEFAULT_RDF_PREFIX = "@prefix not: <http://dsrdf.com/ontology/!> ." + 
	"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ."
			+ "@prefix schema: <http://schema.org/> ." + DSRDF_PREFIX + VAR_PREFIX + DSRDF_EVENT;

	public RDFGraph(Model rdfm) {
		this.rdfModel = rdfm;

		storeVariables(rdfm);
	}

	public RDFGraph() {
		this.rdfModel = ModelFactory.createDefaultModel();
	}

	/**
	 * Create an RDFGraph object from Turtle specification. Assumes that the Turtle
	 * specification is enclosed in {...}
	 * 
	 * @param turtle
	 * @param prefixes
	 */
	public RDFGraph(String turtle, String prefixes) {

		Model turtleModel = ModelFactory.createDefaultModel();

		Pattern p = Pattern.compile(RDFGraphPattern);
		Matcher m = p.matcher(turtle);

		if (m.matches()) {
			String combined = (prefixes == null || prefixes.isEmpty()) ? m.group(1) : prefixes + "\n" + m.group(1);
			this.rdfModel = turtleModel.read(new ByteArrayInputStream(combined.getBytes()), null, "TTL");
			storeVariables(turtleModel);
		} else {
			throw new IllegalArgumentException("Turtle RDF formula should be enclosed in {}");
		}

	}

	public RDFGraph(String turtle) {
		this(turtle, DEFAULT_RDF_PREFIX);
	}

	public RDFGraph(RDFGraph rdf) {
		this.rdfModel = ModelFactory.createDefaultModel().add(rdf.rdfModel);
		storeVariables(this.rdfModel);
	}

	@Override
	public RDFGraph substitute(Formula f1, Formula f2) {
		RDFGraph newGraph = new RDFGraph(this);

		if (!(f1 instanceof RDFVariable && f2 instanceof RDFVariable)) {
			throw new IllegalArgumentException("Only RDFVariable substitutions are supported");
		}
		RDFVariable v1 = (RDFVariable) f1;
		RDFVariable v2 = (RDFVariable) f2;

		ResourceUtils.renameResource(newGraph.findResource(v1.getFullName()), v2.getFullName());

		newGraph.variables.remove(v1);

		newGraph.variables.add(v2);

		return newGraph;
	}

	public Resource findResource(String nodeID) {
		Resource node = this.rdfModel.getResource(nodeID);
		return node;
	}

	public void storeVariables(Model m) {
		StmtIterator mIter = m.listStatements();
		while (mIter.hasNext()) {
			Statement stmt = mIter.nextStatement();

			Resource subj = stmt.getSubject();
			RDFNode obj = stmt.getObject();

			Resource objR = obj.isResource() ? obj.asResource() : null;

			String subjS = subj.toString();

			if (subjS.startsWith(RDFVariable.VAR_NAMESPACE)) {
				getVariables()
						.add(new RDFVariable(subjS.substring(RDFVariable.VAR_NAMESPACE.length(), subjS.length())));

			}

			if (objR != null && objR.toString().startsWith(RDFVariable.VAR_NAMESPACE)) {
				String objRS = objR.toString();
				getVariables().add(new RDFVariable(
						objRS.substring(RDFVariable.VAR_NAMESPACE.length(), objRS.length()).toString()));
			}

			// System.out.println("subj is: "+subj);
			// System.out.println("obj is: "+objR);

			// getVariables().add(new RDFVariable(stmt.getSubject().toString()));
			// getVariables().add(new RDFVariable(stmt.getObject().toString()));
		}
	}

	/**
	 * 
	 * @param graph
	 * @return
	 */
	public RDFGraph union(RDFGraph graph) {
		return new RDFGraph(this.rdfModel.union(graph.getModel()));
	}

	/**
	 * Returns conjunction of this, with g. This is the union of the two graphs,
	 * with the head node coming from the left hand side argument, i.e. this
	 * RDFGraph.
	 * 
	 * Assumes that the heads of the two graphs are OF THE SAME DS TYPE. This is
	 * true in DS Link-Evaluation action that uses this method.
	 */
	public RDFGraph conjoin(Formula g) {
		System.out.println("conjoining :" + this);
		System.out.println("with :" + g);
		if (g instanceof RDFGraph) {
			RDFGraph rdf = (RDFGraph) g;

			RDFVariable argHead = rdf.getHead();
			RDFVariable headThis = this.getHead();

			// Heads of the same DS type should collapse, so:
			// first substitute head of g, with head of this
			// If not of same DS type, then remove head of the argument (g) - head comes
			// from left hand side conjunct (this RDFGraph)

			RDFGraph argHeadSubst;

			argHeadSubst = (headThis != null && argHead != null && argHead.getDSType().equals(headThis.getDSType()))
					? rdf.substitute(argHead, headThis)
					: rdf.removeHead();

			return this.union(argHeadSubst);
		} else {
			throw new UnsupportedOperationException("Can only conjoin with RDFGraph");

		}

	}

	public List<RDFVariable> getHeads() {
		List<RDFVariable> heads = new ArrayList<RDFVariable>();
		Resource mainHead;
		Resource mainHeadType = this.rdfModel.getResource(DSRDF_NAMESPACE + "Head");
		Selector headSelector = new SimpleSelector(null, org.apache.jena.vocabulary.RDF.type, mainHeadType);
		StmtIterator headIter = this.rdfModel.listStatements(headSelector);
		while (headIter.hasNext()) {

			Statement headStmt = headIter.nextStatement();
			mainHead = headStmt.getSubject();
			heads.add(new RDFVariable(mainHead));
		}
		return heads;
	}

	public RDFVariable getHead() {

		Resource mainHead;
		Resource mainHeadType = this.rdfModel.getResource(DSRDF_NAMESPACE + "Head");
		Selector headSelector = new SimpleSelector(null, org.apache.jena.vocabulary.RDF.type, mainHeadType);
		StmtIterator headIter = this.rdfModel.listStatements(headSelector);
		if (!headIter.hasNext()) {
			return null;
		} else {
			Statement headStmt = headIter.nextStatement();
			mainHead = headStmt.getSubject();
		}
		return new RDFVariable(mainHead);
	}

	public RDFGraph removeHead() {
		System.out.println("Removing head of:" + this);

		RDFGraph g = new RDFGraph(this);

		Resource mainHeadType = g.rdfModel.getResource(DSRDF_NAMESPACE + "Head");
		Selector headSelector = new SimpleSelector(null, org.apache.jena.vocabulary.RDF.type, mainHeadType);
		StmtIterator headIter = g.rdfModel.listStatements(headSelector);
		Statement headStmt;
		if (!headIter.hasNext()) {
			// No head. Returning this intact.
			return g;
		} else {
			headStmt = headIter.nextStatement();
			// mainHead = headStmt.getSubject();
		}

		g.rdfModel.remove(headStmt);

		return g;

	}

	@Override
	public int toUniqueInt() {
		// TODO
		return 0;
	}

	@Override
	public boolean subsumesMapped(Formula other, HashMap<Variable, Variable> map) {
		// TODO
		return false;
	}

	protected String removePrefix(String turtle) {

		String[] lines = turtle.split("\n");
		String result = "";
		for (String line : lines) {
			if (line.startsWith("@prefix"))
				continue;
			result += line + "\n";

		}
		return result;

	}

	public String toString() {
		StringWriter writer = new StringWriter();

		RDFDataMgr.write(writer, this.rdfModel, Lang.TURTLE);

		return "{" + removePrefix(writer.toString()) + "}";
	}

	public String toUnicodeString() {
		String plain = toString();
		String[] lines = plain.split("\n");
		String result = "{";
		for (int i = 1; i < lines.length - 1; i++) {
			result += lines[i];
			result += TTRRecordType.TTR_FIELD_SEPARATOR;
			result += TTRRecordType.TTR_LINE_BREAK;
		}

		result += "}";
		return result;
	}

	public Model getModel() {
		// TODO
		return rdfModel;
	}

	public int hashCode() {
		// TODO Currently just toString().hashCode()
		return rdfModel.hashCode();

	}

	public boolean equals(Object other) {
		if (!(other instanceof RDFGraph))
			return false;

		RDFGraph o = (RDFGraph) other;
		return this.rdfModel.equals(o.rdfModel);

	}

	@Override
	public RDFFormula clone() {
		return new RDFGraph(this);
	}

	public boolean hasVariable(RDFVariable v) {
		return variables.contains(v);
	}

	public RDFGraph freshenVars(Context c) {
		Set<RDFVariable> done = new HashSet<RDFVariable>();
		RDFGraph fresh = new RDFGraph(this);
		StmtIterator iter = fresh.rdfModel.listStatements();

		while (iter.hasNext()) {
			Statement stmt = iter.next();
			Resource subj = stmt.getSubject();

			// Assuming every variable is the subject of some statement
			// might turn out to be a bad assumption
			// String subjS = subj.toString();

			if (RDFVariable.isRDFVariable(subj)) {
				RDFVariable var = new RDFVariable(subj);
				if (done.contains(var))
					continue;

				if (var.getName().startsWith(Tree.ENTITY_VARIABLE_ROOT)) {
					RDFVariable newVar = new RDFVariable(c.getFreshEntityVariable());
					while (hasVariable(newVar))
						newVar = new RDFVariable(c.getFreshEntityVariable());

					fresh = fresh.substitute(var, newVar);

				} else if (var.getName().startsWith(Tree.EVENT_VARIABLE_ROOT)) {
					RDFVariable newVar = new RDFVariable(c.getFreshEventVariable());
					while (hasVariable(newVar))
						newVar = new RDFVariable(c.getFreshEventVariable());

					fresh = fresh.substitute(var, newVar);

				} else if (var.getName().startsWith(Formula.RDF_LAMBDA_VARIABLE_ROOT))
					continue;
				else {
					throw new IllegalArgumentException("Illegal Variable:" + var);
				}

				done.add(var);

			}
		}

		return fresh;

	}

	public RDFGraph freshenVars(Tree t) {
		Set<RDFVariable> done = new HashSet<RDFVariable>();
		RDFGraph fresh = new RDFGraph(this);
		StmtIterator iter = fresh.rdfModel.listStatements();

		while (iter.hasNext()) {
			Statement stmt = iter.next();
			Resource subj = stmt.getSubject();

			// Assuming every variable is the rdf subject of some rdf statement
			// might turn out to be a bad assumption

			if (RDFVariable.isRDFVariable(subj)) {
				RDFVariable var = new RDFVariable(subj);
				if (done.contains(var))
					continue;

				if (var.getName().startsWith(Tree.ENTITY_VARIABLE_ROOT)) {
					RDFVariable newVar = new RDFVariable(t.getFreshEntityVariable());
					while (hasVariable(newVar))
						newVar = new RDFVariable(t.getFreshEntityVariable());

					fresh = fresh.substitute(var, newVar);

				} else if (var.getName().startsWith(Tree.EVENT_VARIABLE_ROOT)) {
					RDFVariable newVar = new RDFVariable(t.getFreshEventVariable());
					while (hasVariable(newVar))
						newVar = new RDFVariable(t.getFreshEventVariable());

					fresh = fresh.substitute(var, newVar);

				} else if (var.getName().startsWith(Formula.RDF_LAMBDA_VARIABLE_ROOT))
					continue;
				else
					throw new IllegalStateException("Found illegal RDF variable:" + var.getName());

				done.add(var);

			}
		}

		return fresh;

	}

	public Dimension getDimensionsWhenDrawn(Graphics2D g2) {
		FontMetrics metrics = g2.getFontMetrics();

		String text = toString();
		int lineD = 1;
		int height = g2.getFontMetrics().getHeight();
		int maxW = 0;
		String[] lines = text.split("\n");
		for (String line : lines) {

			if (metrics.stringWidth(line) > maxW)
				maxW = metrics.stringWidth(line);

			height += g2.getFontMetrics().getHeight() + lineD;
		}

		return new Dimension(maxW, height);

	}

	public Dimension draw(Graphics2D g2, float x, float y) {
		FontMetrics metrics = g2.getFontMetrics();

		String text = toString();
		int lineD = 1;
		int height = g2.getFontMetrics().getHeight();
		int maxW = 0;
		String[] lines = text.split("\n");
		for (String line : lines) {
			g2.drawString(line, x + 2, y + height + 2);
			height += g2.getFontMetrics().getHeight() + lineD;
			if (metrics.stringWidth(line) > maxW)
				maxW = metrics.stringWidth(line);
		}

		return new Dimension(maxW, height);
	}

	public static void main(String args[]) {

		String putS = "{var:e rdfs:label \"put\"@en;"
				+ "event:e1 var:e1;"
				+ "event:e2 var:e2;"
				+ "event:e3 var:e3;"
				+ "event:e4 var:e4 ."
				+ "var:e1 a dsrdf:Event;"
				+ "dsrdf:has_location var:x24;"
				+ "dsrdf:theme var:x51 ."
				+ "var:e2 a dsrdf:Event;"
				+ "dsrdf:agent var:x2;"
				+ "dsrdf:cause var:e3 ."
				+ "var:e3 a dsrdf:Process;"
				+ "dsrdf:theme var:x51;"
				+ "dsrdf:not_has_location var:x24;"
				+ " dsrdf:motion var:x56 ."
				+ "var:e4 a dsrdf:Event;"
				+ "}";
		
		RDFGraph putG = new RDFGraph(putS);
		
		System.out.println(putG);

		

		// System.out.println(runG.betaReduce(janeGraph));
		// System.out.println("before collapse:\n"+ janeGraph);

		// RDFGraph janeFuture = janeGraph.substitute(new RDFVariable("y"), new
		// RDFVariable("x"));

		// System.out.println("after collapse:\n"+ janeGraph.collapseHeads());

		// System.out.println(janeGraph.removeHead());

//		System.out.println("Head is: "+janeGraph.getHead());
//		
//		RDFFormula subst = janeGraph.substitute(new RDFVariable("x"), new RDFVariable("x1"));
//		
//		RDFDataMgr.write(System.out, janeGraph.getModel(), Lang.TURTLE); //write graph
//		
//		System.out.println(subst);
//		
//		System.out.println("Variables are:");
//		System.out.println(subst.getVariables());

	}

}
