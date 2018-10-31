import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;

import com.microsoft.z3.Expr;

import anomaly.Anomaly;
import exceptions.UnknownUnitException;
import gimpToApp.GimpToApp;
import gimpToApp.GimpToAppOne;
import ir.*;
import ir.schema.Table;
import java_cup.non_terminal;
import soot.Body;
import soot.BodyTransformer;
import soot.PhaseOptions;
import soot.Scene;
import soot.coffi.constant_element_value;
import soot.jimple.JimpleBody;
import soot.util.cfgcmd.CFGIntermediateRep;
import sql.DDLParser;
import utils.CycleEdge;
import utils.Digraph;
import z3.ConstantArgs;
import z3.Z3Driver;

public class Transformer extends BodyTransformer {
	static String _RT_PATH = "/Library/Java/JavaVirtualMachines/jdk1.8.0_77.jdk/Contents/Home/jre/lib/rt.jar:";
	static String _JCE_PATH = "/Library/Java/JavaVirtualMachines/jdk1.8.0_77.jdk/Contents/Home/jre/lib/jce.jar";
	private static final String irOptionName = "ir";
	private CFGIntermediateRep ir;
	static ArrayList<Body> bodies;

	protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
		if (bodies == null)
			bodies = new ArrayList<Body>();
		ir = CFGIntermediateRep.getIR(PhaseOptions.getString(options, irOptionName));
		Body body = ir.getBody((JimpleBody) b);
		if (!body.getMethod().isConstructor()) {
			// System.out.println(body.getUnits().iterator());
			bodies.add(body);
		}
	}

	////// MAIN
	public static void main(String[] args) {
		Initializer init = new Initializer();
		String[] soot_args = init.initialize();
		soot.Main.main(soot_args);
		GimpToApp gta = null;
		Application app = null;

		/*
		 * extract tables from ddl file
		 */
		DDLParser ddlp = new DDLParser();
		ArrayList<Table> tables = ddlp.parse();
		System.out.print("=============================\n===	SCHEMA \n");
		for (Table t : tables)
			t.printTable();
		System.out.println();

		/*
		 * generate the intermediate representation
		 */
		gta = new GimpToAppOne(Scene.v(), bodies, tables);
		try {
			app = ((GimpToAppOne) gta).transform();
		} catch (UnknownUnitException e) {
			e.printStackTrace();
		}

		app.printApp();

		/*
		 * generate the anomaly given the IR
		 */
		Z3Driver zdr = new Z3Driver(app, tables, false);
		Anomaly anml = zdr.analyze();
		if (anml != null)
			anml.announce();
		// now we can visualize the anomaly or
		// generate concrete execution plans, etc.
		trimModel(anml);
		Z3Driver zdrCore = new Z3Driver(app, tables, true);
		Anomaly coreAnml = zdrCore.analyze();
		if (coreAnml != null)
			coreAnml.announce();

	}

	private static void trimModel(Anomaly anml) {
		System.out.println(getFullCycle(anml).cycleToString());
		ConstantArgs._MAX_TXN_INSTANCES = 2;
		ConstantArgs._DEP_CYCLE_LENGTH = 3;
	}

	private static Digraph getFullCycle(Anomaly anml) {
		Expr head = anml.cycle.keySet().stream().filter(o -> o.toString().contains("O!val!0")).findFirst().get();
		Expr n1 = head, n2 = null;
		Digraph headNode = new Digraph(n1, true);
		Digraph pointer = headNode;

		for (int i = 0; i < anml.cycle.size(); i++) {
			n2 = anml.cycle.get(n1);
			// take care of the gaps in the cycle
			if (n2 == null) {
				String next = "O!val!" + String.valueOf(i + 1);
				n2 = anml.cycle.keySet().stream().filter(o -> o.toString().contains(next)).findFirst().get();
			}
			CycleEdge currentEdge = getEdgeType(anml, n1, n2);
			Digraph nextNode = new Digraph(n2, false);
			pointer.edge = currentEdge;
			pointer.node = n1;
			pointer.push(nextNode);
			// get ready for the next iteration
			pointer = nextNode;
			n1 = n2;

		}
		// connect last and first
		pointer.nextNode=headNode;
		pointer.edge = getEdgeType(anml,pointer.node,headNode.node);
		return pointer;
	}

	private static CycleEdge getEdgeType(Anomaly anml, Expr x, Expr y) {
		CycleEdge currentEdge = null;
		if ((anml.RWPairs.get(x) != null) && anml.RWPairs.get(x).contains(y))
			currentEdge = CycleEdge.RW;
		else if ((anml.WRPairs.get(x) != null) && anml.WRPairs.get(x).contains(y))
			currentEdge = CycleEdge.WR;
		else if ((anml.WWPairs.get(x) != null) && anml.WWPairs.get(x).contains(y))
			currentEdge = CycleEdge.WW;
		else
			currentEdge = CycleEdge.SB;
		return currentEdge;
	}

}
