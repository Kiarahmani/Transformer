package gimpToApp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import soot.Local;
import soot.Unit;
import soot.Value;
import soot.grimp.internal.ExprBox;
import soot.grimp.internal.GAssignStmt;
import soot.grimp.internal.GEqExpr;
import soot.grimp.internal.GIfStmt;
import soot.grimp.internal.GInterfaceInvokeExpr;
import soot.grimp.internal.GInvokeStmt;
import soot.grimp.internal.GNeExpr;
import z3.ConstantArgs;
import ir.expression.Expression;
import ir.statement.*;

public class UnitData {
	// a mapping from units to the loops number they belong to
	// -1 is reserved for units outside of all loops
	private Map<Unit, Integer> unitToLoop;

	public int absIter;
	// just a list of units for internal analysis
	public List<Unit> units;
	// eventual data to be returned
	private List<Statement> stmts;
	// holds the units which contain an execution of queries
	Map<Unit, Value> executeUnits;
	// initially crafted
	private Map<Value, Unit> definedAt;
	// initially extracted and used in analysis and eventually returned
	private Map<Local, Value> params;
	// crafted to hold program logics affecting the queries or path conditions
	private Map<Value, Expression> exps;
	private Map<Unit, Query> queries;
	// holds a mapping from units with preparedStatements to units where they are
	// executed
	private Map<Unit, Unit> prepareToExecute;
	// holds a mapping from values to all units where a function is called by them.
	// e.g. (V:r0)->(U:r0.function)
	private Map<Value, List<Unit>> valueToInvokations;

	// mapping from all units to a mapping from units to expression
	// it is used to take care of multiple .next() calls (first unit represent's
	// body units)
	private Map<Unit, Map<Value, Expression>> unitToSetToExp;

	public int loopCount;

	public UnitData() {
		this.absIter = 0;
		stmts = new ArrayList<Statement>();
		executeUnits = new HashMap<Unit, Value>();
		definedAt = new HashMap<Value, Unit>();
		params = new HashMap<Local, Value>();
		exps = new HashMap<Value, Expression>();
		queries = new LinkedHashMap<Unit, Query>();
		prepareToExecute = new HashMap<Unit, Unit>();
		valueToInvokations = new HashMap<>();
		unitToSetToExp = new HashMap<>();
		this.units = new ArrayList<>();
		this.unitToLoop = new HashMap<>();
		this.loopCount = 0;

	}

	public void addUnitNonLoop(Unit u) {
		this.unitToLoop.put(u, -1);
	}

	public void addUnitToLoop(Unit u, int l) {
		this.unitToLoop.put(u, l);
	}

	public int getLoopNo(Unit u) {
		if (this.unitToLoop.get(u) == null)
			return -1;
		else
			return this.unitToLoop.get(u);
	}

	public Map<Unit, Integer> getAllLoops() {
		return this.unitToLoop;
	}

	public List<Unit> getAllUnitsFromLoop(int l) {
		List<Unit> result = new ArrayList<>();
		for (Unit x : this.unitToLoop.keySet())
			if (this.unitToLoop.get(x) == l)
				result.add(x);
		return result;

	}

	public void addMapUTSE(Unit u, Map<Value, Expression> map) {
		this.unitToSetToExp.put(u, map);
	}

	public void printMapUTSE() {
		for (Unit x : this.unitToSetToExp.keySet()) {
			if (this.unitToSetToExp.get(x) != null)
				System.out.println(" -> " + this.unitToSetToExp.get(x));
		}
	}

	public Map<Unit, Map<Value, Expression>> getUTSEs() {
		return this.unitToSetToExp;
	}

	// extract the value contained in the given unit and if it is an invokation
	// add it to valueToInvokations
	public void addValToInvoke(Unit u) {
		try {
			GInvokeStmt expr = (GInvokeStmt) u;
			if (expr.getUseBoxes().get(expr.getUseBoxes().size() - 1).getValue().getClass().getSimpleName()
					.equals("GInterfaceInvokeExpr")) {
				Value v1 = expr.getUseBoxes().get(expr.getUseBoxes().size() - 1).getValue();
				Value value = v1.getUseBoxes().get(v1.getUseBoxes().size() - 1).getValue();
				if (valueToInvokations.get(value) == null)
					valueToInvokations.put(value, new ArrayList<Unit>());
				this.valueToInvokations.get(value).add(u);
			}
		} catch (ClassCastException e) {
			try {
				GAssignStmt stmt = (GAssignStmt) u;
				Value rOP = stmt.getRightOp();
				if (rOP.getClass().getSimpleName().equals("GInterfaceInvokeExpr")) {
					Value value = rOP.getUseBoxes().get(rOP.getUseBoxes().size() - 1).getValue();
					if (valueToInvokations.get(value) == null)
						valueToInvokations.put(value, new ArrayList<Unit>());
					this.valueToInvokations.get(value).add(u);
				}
			} catch (ClassCastException e1) {
				try {
					GIfStmt gis = (GIfStmt) u;
					for (Value value : extractInvokedValues(gis.getCondition())) {
						if (valueToInvokations.get(value) == null)
							valueToInvokations.put(value, new ArrayList<Unit>());
						this.valueToInvokations.get(value).add(u);
					}
				} catch (ClassCastException e2) {
					e2.printStackTrace();
				}
			}
		}
	}

	// recursively anlyze the value and returns all values that are invoked inside
	// e.g. r0.F1 + r2.F2 must return: [r0,r2]
	public List<Value> extractInvokedValues(Value v) {
		List<Value> result = new ArrayList<Value>();
		switch (v.getClass().getSimpleName()) {
		case "GNeExpr":
			GNeExpr ne = (GNeExpr) v;
			result.addAll(extractInvokedValues(ne.getOp1()));
			result.addAll(extractInvokedValues(ne.getOp2()));
			return result;
			
		case "GEqExpr":
			GEqExpr ee = (GEqExpr) v;
			result.addAll(extractInvokedValues(ee.getOp1()));
			result.addAll(extractInvokedValues(ee.getOp2()));
			return result;

		case "GInterfaceInvokeExpr":
			GInterfaceInvokeExpr iie = (GInterfaceInvokeExpr) v;
			result.add(iie.getBase());
			return result;

		case "IntConstant":
			return result;

		case "JimpleLocal":
			result.add(v);
			return result;
		}
		if (ConstantArgs.DEBUG_MODE)
			System.err.println("---- UnitDAta.java.extractInvokedValues: value extraction case not handled yet: "
					+ v.getClass().getSimpleName());
		return result;
	}

	public List<Unit> getInvokeListFromVal(Value v) {
		return this.valueToInvokations.get(v);
	}

	public Map<Value, List<Unit>> getValueToInvokeMap() {
		return this.valueToInvokations;
	}

	public void addPrepToExec(Unit up, Unit ue) {
		this.prepareToExecute.put(up, ue);
	}

	public Map<Unit, Unit> getPrepToExecMap() {
		return this.prepareToExecute;
	}

	public Unit getExecFromPrep(Unit up) {
		return this.prepareToExecute.get(up);
	}

	public Map<Unit, Query> getQueries() {
		return this.queries;
	}

	public Query getQueryFromUnit(Unit u) {
		return this.queries.get(u);
	}

	public void addQuery(Unit u, Query q) {
		this.queries.put(u, q);
	}

	public Map<Value, Unit> getdefinedAts() {
		return this.definedAt;
	}

	public Expression getExp(Value v) {
		return this.exps.get(v);
	}

	public Map<Value, Expression> getExps() {
		return this.exps;
	}

	public void addExp(Value v, Expression exp) {
		this.exps.put(v, exp);
	}

	public void addParam(Local l, Value v) {
		this.params.put(l, v);
	}

	public boolean isDefinedAtExists(Value v) {
		return this.definedAt.keySet().contains(v);
	}

	public void addDefinedAt(Value v, Unit u) {
		this.definedAt.put(v, u);
	}

	public Map<Local, Value> getParams() {
		return this.params;
	}

	public Unit getDefinedAt(Value v) {
		return this.definedAt.get(v);
	}

	public void printDefinedAt() {
		for (Value v : this.definedAt.keySet())
			System.out.println(v + " := " + this.definedAt.get(v));
	}

	public List<Statement> getStmts() {
		return stmts;
	}

	public void addStmt(Statement s) {
		this.stmts.add(s);
	}

	public void addExecuteUnit(Unit u, Value v) {
		this.executeUnits.put(u, v);
	}

	public boolean isExecute(Unit u) {
		return (this.executeUnits.get(u) != null);
	}

	public Value getExecuteValue(Unit u) {
		return this.executeUnits.get(u);
	}

}
