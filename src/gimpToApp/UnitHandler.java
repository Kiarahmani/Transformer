package gimpToApp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import exceptions.ColumnDoesNotExist;
import exceptions.UnknownUnitException;
import gimpToApp.utils.QueryPatcher;
import gimpToApp.utils.ValueToExpression;
import ir.expression.BinOpExp;
import ir.expression.Expression;
import ir.expression.vals.ConstValExp;
import ir.expression.vals.ProjValExp;
import ir.expression.BinOpExp.BinOp;
import ir.expression.vars.ProjVarExp;
import ir.expression.vars.RowSetVarExp;
import ir.expression.vars.RowVarExp;
import ir.expression.vars.UnknownExp;
import ir.schema.Table;
import ir.statement.InvokeStmt;
import ir.statement.Query;
import ir.statement.Query.Kind;
import soot.Body;
import soot.Local;
import soot.Type;
import soot.Unit;
import soot.UnitPrinter;
import soot.Value;
import soot.ValueBox;
import soot.grimp.internal.GAddExpr;
import soot.grimp.internal.GAssignStmt;
import soot.grimp.internal.GInterfaceInvokeExpr;
import soot.grimp.internal.GInvokeStmt;
import soot.grimp.internal.GNewInvokeExpr;
import soot.grimp.internal.GStaticInvokeExpr;
import soot.grimp.internal.GVirtualInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.LongConstant;
import soot.jimple.StringConstant;
import soot.jimple.toolkits.infoflow.FakeJimpleLocal;
import soot.util.Switch;

public class UnitHandler {
	UnitData data;
	ValueToExpression veTranslator;
	QueryPatcher queryPatcher;
	Body body;
	ArrayList<Table> tables;
	// auxiliary value, used to mark the last visited Prep statement to create the
	// mapping from prepStmt to queries
	Unit lastPreparedStatementUnit;

	public UnitHandler(Body body, ArrayList<Table> tables) {
		data = new UnitData();
		this.body = body;
		this.veTranslator = new ValueToExpression(data);
		this.queryPatcher = new QueryPatcher();
		this.tables = tables;
	}

	public void extractParams() {
		int iter = 0;
		for (Local l : body.getParameterLocals()) {
			this.data.addParam(l, body.getParameterRefs().get(iter++));
		}
	}

	// initial iteration over all units to extract useful information for later
	// stages
	public void InitialAnalysis() throws UnknownUnitException {
		for (Unit u : body.getUnits()) {
			switch (u.getClass().getSimpleName()) {
			case "GIdentityStmt":
				break;
			case "GAssignStmt":
				assignInitHandler(u);
				break;
			case "GInvokeStmt":
				invokeInitHandler(u);
				break;
			case "JGotoStmt": // TODO
				break;
			case "GThrowStmt": // TODO
				break;
			case "JReturnVoidStmt": // TODO
				break;
			case "GIfStmt": // TODO
				break;
			default:
				throw new UnknownUnitException("Unknown Jimple/Grimp unit class: " + u.getClass().getSimpleName());
			}
		}
	}

	// The outermost function wrapping anlysis
	// Has 3 main loops iterating over all units in the given body
	public void extractStatements() throws UnknownUnitException {

		// loop #1
		// extract and create queries with holes
		for (Unit u : body.getUnits())
			if (data.isExecute(u)) {
				Query query = extractQuery(data.getExecuteValue(u), u);
				this.data.addQuery(lastPreparedStatementUnit, query);
			}
		// loop #2
		// helping datastructures
		Value LastRowSet = null;
		List<Unit> unitsWithNextCall = new ArrayList<>();
		Map<Value, Expression> map = null;
		int iter = 0;
		// add some expressions and patch the queries
		for (Unit u : body.getUnits()) {
			// the program logic not affecting queries is abstracted
			if (data.getQueries().containsKey(u)) {
				queryPatcher.patchQuery(u, veTranslator, data);
				updateExpressions(u);
			}
			if (data.getExecuteValue(u) != null) { // if u is executeQ/U
				// update the map
				try {
					LastRowSet = ((GAssignStmt) u).getLeftOp();
					// a list of units which call .next() on this rowSerVar
					unitsWithNextCall = data.getInvokeListFromVal(LastRowSet);
				} catch (ClassCastException e) {
				}
				iter = 0;
			}
			if (unitsWithNextCall.contains(u)) {
				RowSetVarExp oldRSVar = (RowSetVarExp) data.getExp(LastRowSet);
				String newRVarName = LastRowSet.toString() + "-next" + String.valueOf(++iter);
				RowVarExp newRVar = new RowVarExp(newRVarName, oldRSVar.getTable(), oldRSVar);
				data.addExp(new FakeJimpleLocal(newRVarName, null, null), newRVar);
				map = new HashMap<Value, Expression>();
				map.put(LastRowSet, newRVar);

			}
			data.addMapUTSE(u, map);

		}
		// loop #3
		// now add invoke statements containing patched queries
		for (Unit u : data.getQueries().keySet())
			this.data.addStmt(new InvokeStmt(data.getQueryFromUnit(u)));
	}

	// A new rowSetVar is added to the expressions
	private void updateExpressions(Unit u) throws UnknownUnitException {
		Query q = data.getQueryFromUnit(u);
		if (q.getKind() == Kind.SELECT) {
			try {
				GAssignStmt assgnmnt = (GAssignStmt) data.getExecFromPrep(u);
				RowSetVarExp newExp = new RowSetVarExp(assgnmnt.getLeftOp().toString(), q.getTable(), q.getWhClause());
				this.data.addExp(assgnmnt.getLeftOp(), newExp);
			} catch (ClassCastException e) {
				// where a select queries return value is discarded
				this.data.addExp(new FakeJimpleLocal("discarded", null, null), new UnknownExp("discarded", -2));
			}
		}
	}

	// Given a unit (which contains an executeQuery/executeUpdate statement) it
	// searches backward for its String statement;
	private Query extractQuery(Value v, Unit u) throws UnknownUnitException {
		switch (v.getClass().getSimpleName()) {
		case "GInterfaceInvokeExpr":
			GInterfaceInvokeExpr expr = (GInterfaceInvokeExpr) v;
			if (expr.getMethod().getName().equals("prepareStatement")) {
				lastPreparedStatementUnit = u;
				Value nextValue = expr.getArgBox(0).getValue();
				return extractQuery(nextValue, u);
			} else {
				Value caller = v.getUseBoxes().get(v.getUseBoxes().size() - 1).getValue();
				return extractQuery(caller, u);
			}
		case "JimpleLocal":
			GAssignStmt expr1 = (GAssignStmt) this.data.getDefinedAt(v);
			return extractQuery(expr1.getRightOp(), this.data.getDefinedAt(v));

		// WHERE THE QUERY IS GENERATED
		case "StringConstant":
			StringConstant expr2 = (StringConstant) v;
			Query query = new Query(expr2.toString(), data, tables);
			return query;

		case "GVirtualInvokeExpr":
			GVirtualInvokeExpr expr3 = (GVirtualInvokeExpr) v;
			Value nextVal = expr3.getBaseBox().getValue();
			return extractQuery(nextVal, this.data.getDefinedAt(nextVal));

		case "GStaticInvokeExpr":
			GStaticInvokeExpr expr5 = (GStaticInvokeExpr) v;
			return extractQuery(expr5.getArg(0), u);
		case "GNewInvokeExpr":
			GNewInvokeExpr expr6 = (GNewInvokeExpr) v;
			return extractQuery(expr6.getArg(0), u);
		default:
			throw new UnknownUnitException("Unknown Jimple/Grimp value class: " + v.getClass().getSimpleName());
		}
	}

	/*
	 * Initial Handlers
	 */
	private void invokeInitHandler(Unit u) throws UnknownUnitException {
		GInvokeStmt expr = (GInvokeStmt) u;
		this.data.addValToInvoke(u);
		Value value = expr.getUseBoxes().get(expr.getUseBoxes().size() - 1).getValue();
		if (isValueMethodCall(value, "executeQuery") || isValueMethodCall(value, "executeUpdate")) {
			this.data.addExecuteUnit(u, value);
			Unit valueIsExecuteLastPrepStmt = data.getDefinedAt(((GInterfaceInvokeExpr) value).getBase());
			data.addPrepToExec(valueIsExecuteLastPrepStmt, u);
		}
	}

	private void assignInitHandler(Unit u) throws UnknownUnitException {
		GAssignStmt stmt = (GAssignStmt) u;
		this.data.addValToInvoke(u);
		Value rOP = stmt.getRightOp();
		Value lOP = stmt.getLeftOp();
		if (!this.data.isDefinedAtExists(lOP))
			this.data.addDefinedAt(lOP, u);
		if (isValueMethodCall(rOP, "executeQuery") || isValueMethodCall(rOP, "executeUpdate")) {
			this.data.addExecuteUnit(u, rOP);
			Unit valueIsExecuteLastPrepStmt = data.getDefinedAt(((GInterfaceInvokeExpr) rOP).getBase());
			data.addPrepToExec(valueIsExecuteLastPrepStmt, u);
		}
	}

	private boolean isValueMethodCall(Value v, String s) {
		try {
			GInterfaceInvokeExpr expr = (GInterfaceInvokeExpr) v;
			String fName = expr.getMethod().getName();
			return (fName.equals(s) || s.equals("ANY#FUNCTION"));
		} catch (ClassCastException e) {
			return false;
		}
	}

}
