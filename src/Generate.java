import java.util.ArrayList;
import java.util.Map;
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

public class Generate extends BodyTransformer {
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

	private static void analyze(Body b) {
		int iter = 1;
		for (Unit u : b.getUnits()) {
			System.out.print("(" + iter + ")\n");
			System.out.println(" ╰──" + u.getClass());
			System.out.println(" ╰──" + u);
			System.out.println(
					"----------------------------------------------------------------------------------------------");
			iter++;
		}
	}

	////// MAIN
	public static void main(String[] args) {
		Initializer init = new Initializer();
		String[] soot_args = init.initialize();
		soot.Main.main(soot_args);
		// CallGraph cg = Scene.v().getCallGraph();
		// System.out.println(cg.size());
		//SootMethod src = Scene.v().getMainClass().getMethodByName("increment");
		// CallGraph ug = Scene.v().getCallGraph();
		SootField p = Scene.v().getSootClass("MySQLAccess").getFieldByName("p");
		
		
		

		for (Body b : bodies) {
			System.out.println(
					"\n\n================================================================================================\n");
			for (Local x : b.getLocals())
				//System.out.println(x.getName() + "::" + x.getType());
			System.out.println(
					"\n================================================================================================");
			// analyze(b);
			break;// temp - to limit the console output
		}
	}

}

















