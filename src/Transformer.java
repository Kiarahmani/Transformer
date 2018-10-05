import java.util.ArrayList;
import java.util.Map;

import anomaly.Anomaly;
import ir.*;
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
		// extract tables from ddl file
		SQLParser sqlp = new SQLParser();
		ArrayList<Table> tables = sqlp.parse();
		// generate the intermediate representation
		GimpToApp gta = new GimpToApp(Scene.v(), bodies, tables);
		Application app = gta.transform(1);
		// generate the anomaly
		Z3Driver zdr = new Z3Driver();
		Anomaly anml = zdr.analyze(app);
		// now we can use the anomaly to create graphs or concrete execution plans
		System.out.println(anml);
	}

}
