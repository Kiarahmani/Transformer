import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import anomaly.Anomaly;
import exceptions.UnknownUnitException;
import gimpToApp.GimpToApp;
import gimpToApp.GimpToAppOne;
import gimpToApp.GimpToAppTwo;
import ir.*;
import ir.schema.Table;
import soot.Body;
import soot.BodyTransformer;
import soot.Local;
import soot.PhaseOptions;
import soot.Scene;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.UnitBox;
import soot.ValueBox;
import soot.jimple.JimpleBody;
import soot.jimple.internal.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.util.cfgcmd.CFGIntermediateRep;
import sql.DDLParser;
import sun.net.www.content.audio.x_aiff;
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
		// System.out.print("\nSchema Extracted:");
		// for (Table t : tables)
		// t.printTable();

		/*
		 * generate the intermediate representation
		 */
		int absLvl = 1;
		switch (absLvl) {
		case 1:
			// most abstract version (no data value or flow is extracted)
			gta = new GimpToAppOne(Scene.v(), bodies, tables);
			try {
				app = ((GimpToAppOne) gta).transform();
			} catch (UnknownUnitException e) {
				e.printStackTrace();
			}
			break;
		case 2:
			gta = new GimpToAppTwo(Scene.v(), bodies, tables);
			app = ((GimpToAppTwo) gta).transform();
			break;
		default:
			System.err.println("ERROR: Unknown abstraction level requested");
			break;
		}
		// app.printApp();

		/*
		 * generate the anomaly given the IR
		 */
		Z3Driver zdr = new Z3Driver(app, tables);
		Anomaly anml = zdr.analyze();
		// now we can visualize the anomaly or
		// generate concrete execution plans, etc.
		if (anml != null)
			anml.announce();

	}

}