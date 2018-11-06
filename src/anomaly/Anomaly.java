package anomaly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.IntNum;
import com.microsoft.z3.Model;

import ir.Application;
import ir.Transaction;
import ir.schema.Table;
import utils.Tuple;
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
	public Map<Tuple<Expr, Expr>, Tuple<Expr, Integer>> conflictingRow;
	public Map<Expr, Expr> otimes;
	public Map<Expr, Expr> opart;
	public List<Expr> isUpdate;
	private Application app;
	private boolean isCore;

	public List<Expr> Ts;
	ArrayList<Table> tables;

	public Anomaly(Model model, Context ctx, DeclaredObjects objs, ArrayList<Table> tables, Application app,
			boolean isCore) {
		this.model = model;
		this.ctx = ctx;
		this.objs = objs;
		this.isCore = isCore;
		this.tables = tables;
		this.app = app;
	}

	public void announce(boolean isCore) {
		if (!isCore)
			System.out.println("\n\n-------------\n--- Model --- ");
		else
			System.out.println("\n\n------------------\n--- Core Model --- ");

		Map<String, FuncDecl> functions = getFunctions();
		conflictingRow = new HashMap<>();
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

		// announce the non-core model
		if (!isCore) {
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
			System.out.println("------------------");

			//printAllVersions();

			System.out.println("--- TXN Params --- ");
			for (Expr t : Ts) {
				System.out.print(t.toString().replaceAll("!val!", "") + ": ");
				Expr ttype = model.eval(objs.getfuncs("ttype").apply(t), true);
				System.out.print(ttype + "(");
				Transaction txn = app.getTxnByName(ttype.toString());
				String delim = "";
				for (String pm : txn.getParams().keySet()) {
					System.out.print(delim + pm + "=");
					System.out.print(model.eval(ctx.mkApp(objs.getfuncs(ttype.toString() + "_PARAM_" + pm), t), true));

					delim = ", ";
				}
				System.out.println(")");
			}

			AnomalyVisualizer av = new AnomalyVisualizer(WWPairs, WRPairs, RWPairs, visPairs, cycle, model, objs,
					parentChildPairs, otypes, opart, conflictingRow);
			av.createGraph("anomaly.dot");
			// visualize records
			RecordsVisualizer rv = new RecordsVisualizer(ctx, model, objs, tables, conflictingRow);
			rv.createGraph("records.dot");

		}
		// announce core model
		else {
			List<Set<Expr>> coreOpSets = getCoreOps();
			Map<Expr, Expr> coreDep = new HashMap<>();
			System.out.println("Core Os:     " + coreOpSets);
			// find connections between core operations
			for (int i = 0; i < coreOpSets.size(); i++)
				for (int j = 0; j < coreOpSets.size(); j++)
					if (i != j) {
						Set<Expr> set1 = coreOpSets.get(i);
						Set<Expr> set2 = coreOpSets.get(j);
						for (Expr o1 : set1)
							for (Expr o2 : set2)
								if (areConnected(o1, o2))
									coreDep.put(o1, o2);
					}
			CoreAnomalyVisualizer av = new CoreAnomalyVisualizer(WWPairs, WRPairs, RWPairs, visPairs, cycle, model,
					objs, parentChildPairs, otypes, opart, coreDep, coreOpSets);
			av.createGraph("anomaly_core.dot");
			System.out.println("Core Edges:  " + coreDep);
		}
	}

	private void printAllVersions() {
		System.out.println("\n\n\n===========================");
		Expr[] Os = model.getSortUniverse(objs.getSort("O"));
		for (Table t : tables) {
			Expr[] Rs = model.getSortUniverse(objs.getSort(t.getName()));
			FuncDecl verFunc = objs.getfuncs(t.getName() + "_VERSION");
			for (Expr r1 : Rs) {
				if (conflictingRow.values().stream().map(tuple -> tuple.x).collect(Collectors.toList()).contains(r1))
					System.out.println("\n===" + r1);
				for (Expr o : Os)
					System.out.print("(" + o.toString().replaceAll("!val!", "") + ","
							+ model.eval(verFunc.apply(r1, o), true) + ")");

			}
			System.out.println();
		}
		System.out.println("===========================\n\n\n");

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
		for (Expr o1 : Os)
			for (Expr o2 : Os) {
				if (model.eval(x.apply(o1, o2), true).toString().equals("true")) {
					String o1Type = model.eval(ctx.mkApp(objs.getfuncs("otype"), o1), true).toString();
					String o2Type = model.eval(ctx.mkApp(objs.getfuncs("otype"), o2), true).toString();
					FuncDecl func = objs.getfuncs(o1Type.substring(1, o1Type.length() - 1) + "_"
							+ o2Type.substring(1, o2Type.length() - 1) + "_conflict_rows");
					Expr row = model.eval(ctx.mkApp(func, o1, o2), true);
					String tableName = row.getSort().toString();
					IntNum version = (IntNum) model.eval(ctx.mkApp(objs.getfuncs(tableName + "_VERSION"), row, o1),
							true);
					result.put(o1, o2);
					conflictingRow.put(new Tuple<Expr, Expr>(o1, o2), new Tuple<Expr, Integer>(row, version.getInt()));
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

	private List<Set<Expr>> getCoreOps() {
		ArrayList<Set<Expr>> result = new ArrayList<>();
		Set<Expr> newSet = null;
		List<Expr> Os = Arrays.asList(model.getSortUniverse(objs.getSort("O")));
		for (Expr o1 : Os) {
			for (Expr o2 : Os) {
				if (areSib(o1, o2))
					// check if set already exists
					if (setIsCreated(result, o1) == null) {
						newSet = new HashSet<>();
						newSet.add(o1);
						newSet.add(o2);
						result.add(newSet);
					} else {
						newSet = setIsCreated(result, o1);
						newSet.add(o1);
						newSet.add(o2);
					}
			}
		}
		return result.stream().filter(s -> s.size() > 1).collect(Collectors.toList());
	}

	private boolean areConnected(Expr o1, Expr o2) {
		Expr next = this.cycle.get(o1);
		if (next == null)
			return false;
		else if (next.equals(o2))
			return true;
		else
			return areConnected(next, o2);
	}

	private Set<Expr> setIsCreated(ArrayList<Set<Expr>> sets, Expr o1) {
		for (Set<Expr> set : sets)
			if (set.contains(o1))
				return set;
		return null;
	}

	private boolean areSib(Expr o1, Expr o2) {
		boolean result = false;
		for (ArrayList<Expr> sb : this.parentChildPairs.values())
			if (sb.contains(o1) && sb.contains(o2)) {
				result = true;
				break;
			}
		return result;
	}

	private void drawLine() {
		// System.out.println("--------------------------------------");
	}
}
