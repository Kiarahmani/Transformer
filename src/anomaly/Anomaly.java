package anomaly;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.FuncInterp;
import com.microsoft.z3.FuncInterp.Entry;
import com.sun.org.apache.bcel.internal.generic.RETURN;
import com.microsoft.z3.Model;
import com.microsoft.z3.Sort;

import z3.DeclaredObjects;

public class Anomaly {
	private String name;
	private Model model;
	private Context ctx;
	DeclaredObjects objs;
	public Map<Expr, ArrayList<Expr>> visPairs;
	public Map<Expr, ArrayList<Expr>> WRPairs;
	public Map<Expr, ArrayList<Expr>> WWPairs;
	public Map<Expr, ArrayList<Expr>> RWPairs;
	public Map<Expr, ArrayList<Expr>> parentChildPairs;
	public Map<Expr, Expr> cycle;
	public Map<Expr, Expr> otypes;
	public Map<Expr, Expr> ttypes;
	public Map<Expr, Expr> otimes;
	public Map<Expr, Expr> opart;
	public List<Expr> isUpdate;
	private boolean isCore;

	public List<Expr> Ts;

	public Anomaly(Model model, Context ctx, DeclaredObjects objs, boolean isCore) {
		this.model = model;
		this.ctx = ctx;
		this.objs = objs;
		this.isCore = isCore;
	}

	public void announce() {
		if (!isCore)
			System.out.println("\n\n-------------\n--- Model --- ");
		else
			System.out.println("\n\n------------------\n--- Core Model --- ");
		Map<String, FuncDecl> functions = getFunctions();
		parentChildPairs = getParentChild(functions.get("parent"));
		WWPairs = getWWPairs(functions.get("WW_O"));
		WRPairs = getWRPairs(functions.get("WR_O"));
		RWPairs = getRWPairs(functions.get("RW_O"));
		visPairs = getVisPairs(functions.get("vis"));
		cycle = getCycle(functions.get("D"));
		otypes = getOType(functions.get("otype"));
		otimes = getOTime(functions.get("otime"));
		opart = getOPart(functions.get("opart"));
		ttypes = getTType(functions.get("ttype"));
		isUpdate = getIsUpdate(functions.get("is_update"));
		this.Ts = Arrays.asList(model.getSortUniverse(objs.getSort("T")));

		System.out.println("{T}:       " + Ts);
		drawLine();
		System.out.println("ttype:     " + ttypes);
		drawLine();
		System.out.println("{O}:       " + Arrays.asList(model.getSortUniverse(objs.getSort("O"))));
		drawLine();
		System.out.println("Prnt-Chld: " + parentChildPairs);
		drawLine();
		System.out.println("otype:     " + otypes);
		drawLine();
		System.out.println("is_update: " + isUpdate);
		drawLine();
		System.out.println("WW:        " + WWPairs);
		drawLine();
		System.out.println("RW:        " + RWPairs);
		drawLine();
		System.out.println("WR:        " + WRPairs);
		drawLine();
		System.out.println("vis:       " + visPairs);
		drawLine();
		System.out.println("cyc:       " + cycle);
		drawLine();
		System.out.println("otime:     " + otimes);
		drawLine();
		System.out.println("opart:     " + opart);
		drawLine();
		// System.out.println(model);
		System.out.println("-------------\n");
		AnomalyVisualizer av = new AnomalyVisualizer(WWPairs, WRPairs, RWPairs, visPairs, cycle, model, objs,
				parentChildPairs, otypes, opart);
		if (isCore)
			av.createGraph("anomaly_core.dot");
		else
			av.createGraph("anomaly.dot");

		if (isCore)
			ctx.close();
	}

	private List<Expr> getIsUpdate(FuncDecl isUpdate) {
		Expr[] Os = model.getSortUniverse(objs.getSort("O"));
		List<Expr> result = new ArrayList<>();

		for (Expr o : Os) {
			if (model.eval(isUpdate.apply(o), true).toString().equals("true"))
				result.add(o);
		}
		return result;
	}

	private Map<Expr, Expr> getCycle(FuncDecl x) {
		Expr[] Os = model.getSortUniverse(objs.getSort("O"));
		Map<Expr, Expr> result = new LinkedHashMap<>();
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
			else if (f.getName().toString().equals("vis"))
				result.put("vis", f);
			else if (f.getName().toString().equals("WW_O"))
				result.put("WW_O", f);
			else if (f.getName().toString().equals("WR_O"))
				result.put("WR_O", f);
			else if (f.getName().toString().equals("RW_O"))
				result.put("RW_O", f);
			else if (f.getName().toString().equals("D"))
				result.put("D", f);
			else if (f.getName().toString().equals("X"))
				result.put("X", f);
			else if (f.getName().toString().equals("ttype"))
				result.put("ttype", f);
			else if (f.getName().toString().equals("is_update"))
				result.put("is_update", f);
			else if (f.getName().toString().equals("otime"))
				result.put("otime", f);
			else if (f.getName().toString().equals("opart"))
				result.put("opart", f);
			else if (f.getName().toString().equals("otype")) {
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

	private Map<Expr, Expr> getOTime(FuncDecl otimes) {
		Expr[] Os = model.getSortUniverse(objs.getSort("O"));
		Map<Expr, Expr> result = new HashMap<>();
		Expr t;
		if (otimes != null)
			for (Expr o : Os) {
				t = model.eval(otimes.apply(o), true);
				result.put(o, t);
			}
		return result;
	}

	private Map<Expr, Expr> getOPart(FuncDecl opart) {
		Expr[] Os = model.getSortUniverse(objs.getSort("O"));
		Map<Expr, Expr> result = new HashMap<>();
		Expr t;
		if (opart != null)
			for (Expr o : Os) {
				t = model.eval(opart.apply(o), true);
				result.put(o, t);
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

	private void drawLine() {
		// System.out.println("--------------------------------------");
	}
}
