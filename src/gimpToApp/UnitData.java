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
import soot.grimp.internal.GAssignStmt;
import soot.grimp.internal.GInvokeStmt;
import ir.expression.Expression;
import ir.statement.*;

public class UnitData {
	public int absIter;
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

	}

	public void addMapUTSE(Unit u, Map<Value, Expression> map) {
		this.unitToSetToExp.put(u, map);
	}

	public void printMapUTSE() {
		for (Unit x : this.unitToSetToExp.keySet()) {
			if (this.unitToSetToExp.get(x) != null)
				System.out.println(" -> " + this.unitToSetToExp.get(x));
		}
		System.out.println("======");
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
			GAssignStmt stmt = (GAssignStmt) u;
			Value rOP = stmt.getRightOp();
			if (rOP.getClass().getSimpleName().equals("GInterfaceInvokeExpr")) {
				Value value = rOP.getUseBoxes().get(rOP.getUseBoxes().size() - 1).getValue();
				if (valueToInvokations.get(value) == null)
					valueToInvokations.put(value, new ArrayList<Unit>());
				this.valueToInvokations.get(value).add(u);
			}
		}
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
