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
		long start = System.currentTimeMillis();
		/*
		 * extract tables from ddl file
		 */
		DDLParser ddlp = new DDLParser();
		ArrayList<Table> tables = ddlp.parse();
		System.out.print("=============================\n===	SCHEMA \n");
		for (Table t : tables)
			t.printTable();
		System.out.println();
		long endTables = System.currentTimeMillis();
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
		long endApp = System.currentTimeMillis();

		/*
		 * generate the anomaly given the IR
		 */
		Z3Driver zdr = new Z3Driver(app, tables, false);
		Anomaly anml = zdr.analyze();
		if (anml != null) {
			anml.announce(false);
			anml.announce(true);
			System.out.println("\n\n");
		}


		long endZ3 = System.currentTimeMillis();

		// print stats
		System.out.println("\n\n\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`");
		System.out.println("~Tables extracted in: " + (endTables - start) + " ms");
		System.out.println("~App analyzed in:     " + (endApp - endTables) + " ms");
		System.out.println("~Model Generated in:  " + (endZ3 - endTables) + " ms");
		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`");

	}
}
