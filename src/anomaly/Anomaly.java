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
	Map<Expr, Expr> otypes;
	Map<Expr, Expr> ttypes;

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
		WWPairs = getWWPairs(functions.get("WW_O"));
		WRPairs = getWRPairs(functions.get("WR_O"));
		RWPairs = getRWPairs(functions.get("RW_O"));
		visPairs = getVisPairs(functions.get("vis"));
		cycle = getCycle(functions.get("D"));
		otypes = getOType(functions.get("otype"));
		ttypes = getTType(functions.get("ttype"));
		System.out.println("Parent-Child: " + parentChildPairs);
		System.out.println("OType: " + otypes);
		System.out.println("TType: " + ttypes);
		System.out.println("WW:  " + WWPairs);
		System.out.println("RW:  " + RWPairs);
		System.out.println("WR:  " + WRPairs);
		System.out.println("vis: " + visPairs);
		System.out.println("cyc: " + cycle);
		// System.out.println(model);
		System.out.println("-----------\n");
		AnomalyVisualizer av = new AnomalyVisualizer(WWPairs, WRPairs, RWPairs, visPairs, cycle, model, objs,
				parentChildPairs, otypes);
		av.createGraph();
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
			else if (f.getName().toString().contains("ttype"))
				result.put("ttype", f);
			else if (f.getName().toString().contains("otype")) {
				result.put("otype", f);
			}
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

	private Map<Expr, Expr> getOType(FuncDecl oType) {
		Expr[] Os = model.getSortUniverse(objs.getSort("O"));
		Map<Expr, Expr> result = new HashMap<>();
		Expr t;
		for (Expr o : Os) {
			t = model.eval(oType.apply(o), true);
			result.put(o, t);
		}
		return result;
	}

	private Map<Expr, Expr> getTType(FuncDecl ttype) {
		Expr[] Ts = model.getSortUniverse(objs.getSort("T"));
		Map<Expr, Expr> result = new HashMap<>();
		Expr tp;
		for (Expr t : Ts) {
			tp = model.eval(ttype.apply(t), true);
			result.put(t, tp);
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
