import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import soot.Body;
import soot.G;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Targets;
import soot.toolkits.graph.DirectedGraph;
import soot.util.cfgcmd.CFGGraphType;
import soot.util.cfgcmd.CFGToDotGraph;
import soot.util.dot.DotGraph;

public class Printer {
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

	public void print_callGraph() {
		// analysis
		CallGraph ug = Scene.v().getCallGraph();
		SootMethod src = Scene.v().getMainClass().getMethodByName("main");
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

}
