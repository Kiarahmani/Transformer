package gimpToApp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import soot.Local;
import soot.Unit;
import soot.Value;
import ir.expression.ParamValExp;
import ir.statement.*;

public class UnitData {
	// eventual data to be returned
	private List<Statement> stmts;
	// initially crafted
	private Map<Unit, Value> executeUnits;
	// initially crafted
	private Map<Value, Unit> definedAt;
	// initially extracted and used in analysis and eventually returned
	private Map<Local, Value> params;

	public UnitData() {
		stmts = new ArrayList<Statement>();
		executeUnits = new HashMap<Unit, Value>();
		definedAt = new HashMap<Value, Unit>();
		params = new HashMap<Local, Value>();
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
