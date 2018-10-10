package anomaly;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.FuncInterp;
import com.microsoft.z3.FuncInterp.Entry;
import com.sun.org.apache.bcel.internal.generic.RETURN;
import com.microsoft.z3.Model;

import z3.DeclaredObjects;

public class Anomaly {
	private String name;
	private Model model;
	private Context ctx;
	DeclaredObjects objs;
	Map<Expr, ArrayList<Expr>> visPairs;
	Map<Expr, ArrayList<Expr>> WRPairs;
	Map<Expr, ArrayList<Expr>> WWPairs;
	Map<Expr, ArrayList<Expr>> RWPairs;
	Map<Expr, ArrayList<Expr>> parentChildPairs;
	Map<Expr, Expr> cycle;

	public Anomaly(Model model, Context ctx, DeclaredObjects objs) {
		this.model = model;
		this.ctx = ctx;
		this.objs = objs;
	}

	public void announce() {
		System.out.println("I'm an anomaly!");
		System.out.println("-----------\nModel: ");
		Map<String, FuncDecl> functions = getFunctions();
		parentChildPairs = getParentChild(functions.get("parent"));
		System.out.println("Parent-Child: " + parentChildPairs);
		WWPairs = getWWPairs(functions.get("WW_O"));
		WRPairs = getWRPairs(functions.get("WR_O"));
		RWPairs = getRWPairs(functions.get("RW_O"));
		visPairs = getVisPairs(functions.get("vis"));
		cycle = getCycle(functions.get("D"));

		System.out.println("WW:  " + WWPairs);
		System.out.println("RW:  " + RWPairs);
		System.out.println("WR:  " + WRPairs);
		System.out.println("vis: " + visPairs);
		System.out.println("cyc: " + cycle);
		// System.out.println(model);
		System.out.println("-----------\n");
		createGraph();
		ctx.close();
	}

	private Map<Expr, Expr> getCycle(FuncDecl x) {
		Expr[] Os = model.getSortUniverse(objs.getSort("O"));
		Map<Expr, Expr> result = new HashMap<>();
		for (Expr o : Os)
			for (Expr o1 : Os) {
				if (model.eval(x.apply(o, o1), true).toString().equals("true")) {
					result.put(o, o1);
				}
			}
		return result;
	}

	public void createGraph() {
		File file = new File("anomalies/anomaly.dot");
		FileWriter writer = null;
		PrintWriter printer;
		String node_style = "node[ color=darkgoldenrod4, fontcolor=darkgoldenrod4, fontsize=10, fontname=\"Helvetica\"]";
		String edge_style = "\nedge[fontsize=12, fontname=\"Helvetica\"]";
		String graph_style = "\nrankdir=RL\n" + "style=filled\n"+"fontname=\"Helvetica\"\n" + "fontcolor=darkgoldenrod4\n"
				+ "color=cornsilk1\n style=\"rounded,filled\"\n" + "fontsize=10\n";
		String bold_style = "concentrate=true, penwidth=2.0,weight=0.3, style=bold, arrowhead=normal, arrowtail=inv, arrowsize=0.9, color=red3, fontsize=11, fontcolor=red3";
		String normal_style = "concentrate=true, style=solid,weight=0.2, arrowhead=normal, arrowtail=inv, arrowsize=0.7, color=gray60, fontsize=10, fontcolor=gray60";
		String rw_edge_setting = "[label = \"rw\", " + normal_style + "]";
		String wr_edge_setting = "[label = \"wr\", " + normal_style + "]";
		String ww_edge_setting = "[label = \"ww\", " + normal_style + "]";
		String rwB_edge_setting = "[label = \"RW\"," + bold_style + "]";
		String wrB_edge_setting = "[label = \"WR\"," + bold_style + "]";
		String wwB_edge_setting = "[label = \"WW\"," + bold_style + "]";
		String vis_edge_setting = "[label = \"vis\",style=solid,weight=0.2, arrowhead=normal, arrowtail=inv, arrowsize=0.7, color=gray60, fontsize=10, fontcolor=gray60]";
		try {
			writer = new FileWriter(file, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		printer = new PrintWriter(writer);
		Expr[] Ts = model.getSortUniverse(objs.getSort("T"));

		printer.append("digraph {" + graph_style + node_style + edge_style);
		String ttype = "";
		int iter = 0;
		for (Expr t : Ts) {
			ttype = model.eval(objs.getfuncs("ttype").apply(Ts[1]), true).toString();
			printer.append("\nsubgraph cluster_" + iter + " {\n");
			printer.append("label=\" " + t.toString().replaceAll("!val!", "") + "\n(" + ttype + ")" + "\";\n");
			for (Expr o : parentChildPairs.get(t))
				printer.append(o.toString().replaceAll("!val!", "") + "; ");
			printer.append("}");
			iter++;
		}
		printer.append("\n\n");
		for (Expr t : Ts) {
			for (Expr o : parentChildPairs.get(t)) {

				// vis
				if (visPairs.get(o) != null)
					for (Expr o1 : visPairs.get(o))
						if (o1 != null)
							printer.append(o.toString().replaceAll("!val!", "") + " -> "
									+ o1.toString().replaceAll("!val!", "") + vis_edge_setting + ";\n");

				// WW
				if (WWPairs.get(o) != null)
					for (Expr o1 : WWPairs.get(o))
						if (o1 != null) {
							if (cycle.get(o) != null && cycle.get(o).toString().equals(o1.toString()))
								printer.append(o.toString().replaceAll("!val!", "") + " -> "
										+ o1.toString().replaceAll("!val!", "") + wwB_edge_setting + ";\n");
							else
								printer.append(o.toString().replaceAll("!val!", "") + " -> "
										+ o1.toString().replaceAll("!val!", "") + ww_edge_setting + ";\n");
						}
				// WR
				if (WRPairs.get(o) != null)
					for (Expr o1 : WRPairs.get(o))
						if (o1 != null)
							if (cycle.get(o) != null && cycle.get(o).toString().equals(o1.toString()))
								printer.append(o.toString().replaceAll("!val!", "") + " -> "
										+ o1.toString().replaceAll("!val!", "") + wrB_edge_setting + ";\n");
							else
								printer.append(o.toString().replaceAll("!val!", "") + " -> "
										+ o1.toString().replaceAll("!val!", "") + wr_edge_setting + ";\n");
				// RW
				if (RWPairs.get(o) != null)
					for (Expr o1 : RWPairs.get(o))
						if (o1 != null)
							if (cycle.get(o) != null && cycle.get(o).toString().equals(o1.toString()))
								printer.append(o.toString().replaceAll("!val!", "") + " -> "
										+ o1.toString().replaceAll("!val!", "") + rwB_edge_setting + ";\n");
							else
								printer.append(o.toString().replaceAll("!val!", "") + " -> "
										+ o1.toString().replaceAll("!val!", "") + rw_edge_setting + ";\n");
			}

		}

		printer.append("\n}");
		printer.flush();

	}

	public Map<String, FuncDecl> getFunctions() {
		Expr[] Os = model.getSortUniverse(objs.getSort("O"));
		Expr[] Ts = model.getSortUniverse(objs.getSort("T"));
		Map<String, FuncDecl> result = new HashMap<>();
		for (FuncDecl f : model.getFuncDecls()) {
			if (f.getName().toString().contains("parent"))
				result.put("parent", f);
			else if (f.getName().toString().contains("vis"))
				result.put("vis", f);
			else if (f.getName().toString().contains("WW_O"))
				result.put("WW_O", f);
			else if (f.getName().toString().contains("WR_O"))
				result.put("WR_O", f);
			else if (f.getName().toString().contains("RW_O"))
				result.put("RW_O", f);
			else if (f.getName().toString().contains("D"))
				result.put("D", f);
			else if (f.getName().toString().contains("X"))
				result.put("X", f);
		}
		return result;
	}

	private Map<Expr, ArrayList<Expr>> getVisPairs(FuncDecl vis) {
		Expr[] Os = model.getSortUniverse(objs.getSort("O"));
		Map<Expr, ArrayList<Expr>> result = new HashMap<>();
		ArrayList<Expr> relation;
		for (Expr o : Os)
			for (Expr o1 : Os) {
				if (model.eval(vis.apply(o, o1), true).toString().equals("true")) {
					relation = result.get(o);
					if (relation == null)
						relation = new ArrayList<Expr>();
					relation.add(o1);
					result.put(o, relation);
				}
			}
		return result;
	}

	private Map<Expr, ArrayList<Expr>> getWWPairs(FuncDecl ww) {
		Expr[] Os = model.getSortUniverse(objs.getSort("O"));
		Map<Expr, ArrayList<Expr>> result = new HashMap<>();
		ArrayList<Expr> relation;
		for (Expr o : Os)
			for (Expr o1 : Os) {
				if (model.eval(ww.apply(o, o1), true).toString().equals("true")) {
					relation = result.get(o);
					if (relation == null)
						relation = new ArrayList<Expr>();
					relation.add(o1);
					result.put(o, relation);
				}
			}
		return result;
	}

	private Map<Expr, ArrayList<Expr>> getRWPairs(FuncDecl rw) {
		Expr[] Os = model.getSortUniverse(objs.getSort("O"));
		Map<Expr, ArrayList<Expr>> result = new HashMap<>();
		ArrayList<Expr> relation;
		for (Expr o : Os)
			for (Expr o1 : Os) {
				if (model.eval(rw.apply(o, o1), true).toString().equals("true")) {
					relation = result.get(o);
					if (relation == null)
						relation = new ArrayList<Expr>();
					relation.add(o1);
					result.put(o, relation);
				}
			}
		return result;
	}

	private Map<Expr, ArrayList<Expr>> getWRPairs(FuncDecl wr) {
		Expr[] Os = model.getSortUniverse(objs.getSort("O"));
		Map<Expr, ArrayList<Expr>> result = new HashMap<>();
		ArrayList<Expr> relation;
		for (Expr o : Os)
			for (Expr o1 : Os) {
				if (model.eval(wr.apply(o, o1), true).toString().equals("true")) {
					relation = result.get(o);
					if (relation == null)
						relation = new ArrayList<Expr>();
					relation.add(o1);
					result.put(o, relation);
				}
			}
		return result;
	}

	private Map<Expr, ArrayList<Expr>> getParentChild(FuncDecl parent) {
		Expr[] Os = model.getSortUniverse(objs.getSort("O"));
		Map<Expr, ArrayList<Expr>> result = new HashMap<>();
		Expr t;
		ArrayList<Expr> child;
		for (Expr o : Os) {
			t = model.eval(parent.apply(o), true);
			child = result.get(t);
			if (child == null)
				child = new ArrayList<Expr>();
			child.add(o);
			result.put(t, child);
		}
		return result;
	}
}
