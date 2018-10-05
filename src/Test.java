import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import soot.MethodOrMethodContext;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.jimple.Jimple;
import soot.jimple.toolkits.callgraph.CHATransformer;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.jimple.toolkits.callgraph.Targets;

public class Test {
	static String _RT_PATH = "/Library/Java/JavaVirtualMachines/jdk1.8.0_77.jdk/Contents/Home/jre/lib/rt.jar:";
	static String _JCE_PATH = "/Library/Java/JavaVirtualMachines/jdk1.8.0_77.jdk/Contents/Home/jre/lib/jce.jar";

	private static String visit(CallGraph cg, SootMethod method, String old_string) {
		String output = "";
		Map<String, Boolean> visited = new HashMap<>();
		visited.put(method.getSignature(), true);
		// iterate over unvisited children
		Iterator<MethodOrMethodContext> ctargets = new Targets(cg.edgesOutOf(method));
		while (ctargets.hasNext()) {
			SootMethod child = (SootMethod) ctargets.next();
			if (!child.isJavaLibraryMethod()) {
				if (!visited.containsKey(child.getSignature()))
					output += method.getName() + " -> " + child.getName() + ";\n" + visit(cg, child, "");
			}
		}
		return old_string + output;

	}

	public static void main(String[] args) {
		System.out.println(">>>> Starting Analysis");
		// input classes
		List<String> argsList = new ArrayList<String>(Arrays.asList(args));
		argsList.addAll(Arrays
				.asList(new String[] { "-cp", "/Users/Kiarash/dev/eclipse_workspace/Soot/bin:" + _RT_PATH + _JCE_PATH,
						"-w", "-main-class", "CallGraphs", // main-class
						"CallGraphs" }));

		// analysis
		PackManager.v().getPack("wjtp").add(new Transform("wjtp.myTrans", new SceneTransformer() {
			@Override
			protected void internalTransform(String phaseName, Map options) {
				CallGraph ug = null;
				CHATransformer.v().transform();
				SootClass a = Scene.v().getSootClass("A");
				SootMethod src = Scene.v().getMainClass().getMethodByName("main");
				ug = Scene.v().getCallGraph();
				
				System.out.println(Scene.v().hasMainClass());
			
				String output = visit(ug, src, "");
				PrintWriter writer;
				try {
					writer = new PrintWriter("output.dot");
					writer.println("digraph callgraph");
					writer.println("{");
					writer.println(output);
					writer.println("}");
					writer.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		}));

		args = argsList.toArray(new String[0]);
		soot.Main.main(args);
		System.out.println(">>>> Analysis Finished");
	}
}
