package anomaly;
/*
 * creates a .dot file to be used for visualization 
 * 
 * 
 */
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;

import com.microsoft.z3.Expr;
import com.microsoft.z3.Model;

import z3.DeclaredObjects;

public class AnomalyVisualizer {

	Map<Expr, ArrayList<Expr>> WWPairs;
	Map<Expr, ArrayList<Expr>> WRPairs;
	Map<Expr, ArrayList<Expr>> RWPairs;
	Map<Expr, ArrayList<Expr>> visPairs;
	Map<Expr, Expr> cycle;
	Model model;
	DeclaredObjects objs;
	Map<Expr, ArrayList<Expr>> parentChildPairs;

	public AnomalyVisualizer(Map<Expr, ArrayList<Expr>> wWPairs, Map<Expr, ArrayList<Expr>> wRPairs,
			Map<Expr, ArrayList<Expr>> rWPairs, Map<Expr, ArrayList<Expr>> visPairs, Map<Expr, Expr> cycle, Model model,
			DeclaredObjects objs, Map<Expr, ArrayList<Expr>> parentChildPairs) {
		this.WRPairs = wRPairs;
		this.WWPairs = wWPairs;
		this.visPairs = visPairs;
		this.RWPairs = rWPairs;
		this.cycle = cycle;
		this.model = model;
		this.objs = objs;
		this.parentChildPairs = parentChildPairs;
	}

	public void createGraph() {
		File file = new File("anomalies/anomaly.dot");
		FileWriter writer = null;
		PrintWriter printer;
		String node_style = "node[ color=darkgoldenrod4, fontcolor=darkgoldenrod4, fontsize=10, fontname=\"Helvetica\"]";
		String edge_style = "\nedge[fontsize=12, fontname=\"Helvetica\"]";
		String graph_style = "\nrankdir=RL\n" + "style=filled\n" + "fontname=\"Helvetica\"\n"
				+ "fontcolor=darkgoldenrod4\n" + "color=cornsilk1\n style=\"rounded,filled\"\n" + "fontsize=10\n";
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
}
