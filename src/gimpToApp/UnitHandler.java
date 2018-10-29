package gimpToApp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import exceptions.ColumnDoesNotExist;
import exceptions.UnknownUnitException;
import gimpToApp.utils.QueryPatcher;
import gimpToApp.utils.ValueToExpression;
import ir.expression.Expression;
import ir.expression.UnOpExp;
import ir.expression.UnOpExp.UnOp;
import ir.expression.vals.ConstValExp;
import ir.expression.vals.NullExp;
import ir.expression.vars.RowSetVarExp;
import ir.expression.vars.RowVarExp;
import ir.expression.vars.RowVarLoopExp;
import ir.expression.vars.UnknownExp;
import ir.schema.Table;
import ir.statement.InvokeStmt;
import ir.statement.Query;
import ir.statement.Query.Kind;
import soot.Body;
import soot.Local;
import soot.Unit;
import soot.Value;
import soot.grimp.internal.GAddExpr;
import soot.grimp.internal.GAssignStmt;
import soot.grimp.internal.GEqExpr;
import soot.grimp.internal.GIfStmt;
import soot.grimp.internal.GInterfaceInvokeExpr;
import soot.grimp.internal.GInvokeStmt;
import soot.grimp.internal.GNeExpr;
import soot.grimp.internal.GNewInvokeExpr;
import soot.grimp.internal.GStaticInvokeExpr;
import soot.grimp.internal.GVirtualInvokeExpr;
import soot.jimple.StringConstant;
import soot.jimple.internal.JGotoStmt;
import soot.jimple.toolkits.infoflow.FakeJimpleLocal;
import z3.ConstantArgs;

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
			data.units.add(u);
			data.addCondToUnit(u, new ConstValExp(true));
		}
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
			case "JGotoStmt":
				JGotoStmt gs = (JGotoStmt) u;
				gotoInitHandler(null, u, gs.getUnitBoxes().get(0).getUnit());
				break;
			case "GThrowStmt": // TODO
				break;
			case "JReturnVoidStmt": // TODO
				break;
			case "GIfStmt": // TODO
				ifInitHandler(u);
				break;
			default:
				throw new UnknownUnitException("Unknown Jimple/Grimp unit class: " + u.getClass().getSimpleName());
			}
		}
	}

	// The outermost function wrapping anlysis
	// Has 4 main loops iterating over all units in the given body
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
		List<Value> LastRowSets = new ArrayList<>();
		Map<Value, List<Unit>> unitsWithNextCall = new HashMap<>();
		Map<Value, Expression> map = null;
		int iter = 0;
		// add LHS expressions with patchy queries
		for (Unit u : body.getUnits()) {
			// the program logic not affecting queries is abstracted
			if (data.getQueries().containsKey(u))
				updateExpressions(u);

			// find all .next() invokations on LHS of this (if exists)
			if (data.getExecuteValue(u) != null) { // if u is executeQ/U update the map
				try {
					Value LastRowSet = ((GAssignStmt) u).getLeftOp();
					LastRowSets.add(LastRowSet);
					// if (data.getInvokeListFromVal(LastRowSet) != null)
					// a list of units which call .next() on this rowSerVar
					for (Unit x : data.getInvokeListFromVal(LastRowSet)) {
						if (this.isValueMethodCall(unitToValue(x), "next")) {
							if (unitsWithNextCall.get(LastRowSet) == null)
								unitsWithNextCall.put(LastRowSet, new ArrayList<Unit>());
							unitsWithNextCall.get(LastRowSet).add(x);
						}
					}
				} catch (ClassCastException e) {
				}
				iter = 0;
			}
			// for all rowSets calls found so far, map them to the appropriate expression
			for (Value LastRowSet : LastRowSets) {
				int index = unitsWithNextCall.get(LastRowSet).indexOf(u);
				if (index != -1) {// if you are one of the .next() calls
					if (data.getLoopNo(u) == -1) {// if you are outside of loops
						RowSetVarExp oldRSVar = (RowSetVarExp) data.getExp(LastRowSet);
						String newRVarName = LastRowSet.toString() + "-next" + String.valueOf(++iter);
						RowVarExp newRVar = new RowVarExp(newRVarName, oldRSVar.getTable(), oldRSVar);
						data.addExp(new FakeJimpleLocal(newRVarName, null, null), newRVar);
						map = new HashMap<Value, Expression>();
						map.put(LastRowSet, newRVar);
						//
						Unit nextU = body.getUnits().getSuccOf(u);
						innerloop: for (int i = 0; i < body.getUnits().size(); i++) {
							// System.out.println(unitsWithNextCall);

							if (nextU == null)
								break innerloop;
							if (unitsWithNextCall.get(LastRowSet).indexOf(nextU) < index + 1) {
								data.addMapUTSE(nextU, map);
							} else
								break innerloop;
							nextU = body.getUnits().getSuccOf(nextU);
						}
					} else {// if you are inside of a loop

						RowSetVarExp oldRSVar = (RowSetVarExp) data.getExp(LastRowSet);
						String newRVarName = LastRowSet.toString() + "-loopVar" + String.valueOf(data.getLoopNo(u));
						RowVarLoopExp newRLVar = new RowVarLoopExp(newRVarName, oldRSVar.getTable(), oldRSVar);
						data.addExp(new FakeJimpleLocal(newRVarName, null, null), newRLVar);
						map = new HashMap<Value, Expression>();
						map.put(LastRowSet, newRLVar);
						for (Unit x : data.getAllUnitsFromLoop(data.getLoopNo(u))) {
							data.addMapUTSE(x, map);
						}
					}

				}
			}
		}
		// loop #3
		// patch the queries and the rSets
		for (Unit u : body.getUnits()) {
			// the program logic not affecting queries is abstracted
			if (data.getQueries().containsKey(u)) {
				queryPatcher.patchQuery(u, veTranslator, data);
			}
		}
		// loop #4
		// now add invoke statements containing patched queries
		for (Unit u : data.getQueries().keySet())
			this.data.addStmt(new InvokeStmt(data.getCondFromUnit(u), data.getQueryFromUnit(u)));

		// loop #5
		// here we patch up the not_null expressions previously created
		for (Value v : data.getExps().keySet())
			if (v.toString().contains("NotNull")) {
				NullExp notNullExp = (NullExp) data.getExp(v);
				RowSetVarExp rSetExp = (RowSetVarExp) data.getExp(notNullExp.rSet);
				notNullExp.updateRowSetExp(rSetExp);
			}
	}

	// A new rowSetVar is added to the expressions
	private void updateExpressions(Unit u) throws UnknownUnitException {
		Query q = data.getQueryFromUnit(u);
		if (q.getKind() == Kind.SELECT) {
			try {
				GAssignStmt assgnmnt = (GAssignStmt) data.getExecFromPrep(u);
				RowSetVarExp newExp = new RowSetVarExp(assgnmnt.getLeftOp().toString(), q.getTable(), q.getWhClause());
				this.data.addExp(assgnmnt.getLeftOp(), newExp);
				q.addStmt(newExp);
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
	 * 
	 * 
	 * 
	 * Initial Handlers
	 * 
	 * 
	 * 
	 * 
	 * 
	 */

	private void ifInitHandler(Unit u) {
		GIfStmt gis = (GIfStmt) u;
		this.data.addValToInvoke(u);

		try {
			// handling the if path
			Expression ifCond = veTranslator.valueToExpression(false, -1, ir.Type.BOOLEAN, u, gis.getCondition());
			Unit pointsTo = (gis.getUnitBoxes().get(0).getUnit());
			gotoInitHandler(new UnOpExp(UnOp.NOT, ifCond), u, pointsTo);
			// handling the else path
			Unit lastUnitInIf = body.getUnits().getPredOf(pointsTo);

			//
			// the second condition is to make sure if conditions used for loops are not
			// accepted here
			if (lastUnitInIf.getClass().getSimpleName().equals("JGotoStmt")) {
				Unit lastUnitpointsTo = ((JGotoStmt) lastUnitInIf).getUnitBoxes().get(0).getUnit();
				if (data.units.indexOf(lastUnitpointsTo) != data.units.indexOf(u)) // they are equal in loops
					gotoInitHandler(ifCond, lastUnitInIf, lastUnitpointsTo);
			}

		} catch (UnknownUnitException | ColumnDoesNotExist e) {
			e.printStackTrace();
		}
	}

	private void gotoInitHandler(Expression pathCond, Unit pointsFrom, Unit pointsTo) {

		int thisUnitNo = data.units.indexOf(pointsFrom);
		int pointsToUnitNo = data.units.indexOf(pointsTo);
		// HANDLING LOOPS
		// set the loop boundaries
		if (pointsToUnitNo != -1 && pointsToUnitNo < thisUnitNo) {
			for (int i = pointsToUnitNo; i <= thisUnitNo; i++) {
				data.addUnitToLoop(data.units.get(i), data.loopCount);
				// gather loop-locals and bookkeep them
				Value loopLocal;
				if (data.units.get(i).getClass().getSimpleName().equals("GAssignStmt")) {
					loopLocal = ((GAssignStmt) data.units.get(i)).getLeftOp();
					data.addLoopLocal(data.loopCount, loopLocal);
				}
			}
			data.loopCount++;
		}
		// HANDLING CONDITIONALS
		if (pathCond != null && (pointsToUnitNo == -1 || pointsToUnitNo > thisUnitNo)) {
			// if jumps to the end of the program
			if (pointsToUnitNo == -1) {
				for (int i = thisUnitNo; i < data.units.size(); i++) {
					// data.addCondToUnit(data.units.get(i), pathCond);
				}
			} else
				for (int i = thisUnitNo; i <= pointsToUnitNo; i++)
					data.addCondToUnit(data.units.get(i), pathCond);
		}
	}

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

	private Value unitToValue(Unit u) {
		try {
			// if it's an invokation
			GInvokeStmt expr = (GInvokeStmt) u;
			Value value = expr.getUseBoxes().get(expr.getUseBoxes().size() - 1).getValue();
			return value;
		} catch (ClassCastException e) {
			// if it's an assignment
			try {
				GAssignStmt stmt = (GAssignStmt) u;
				return stmt.getRightOp();
			} catch (ClassCastException e1) {
				try {
					GIfStmt is = (GIfStmt) u;
					return is.getCondition();
				} catch (ClassCastException e2) {
					e2.printStackTrace();
				}
				return null;
			}
		}
	}

	private boolean isValueMethodCall(Value v, String s) {
		try {
			GInterfaceInvokeExpr expr = (GInterfaceInvokeExpr) v;
			String fName = expr.getMethod().getName();
			return (fName.equals(s) || s.equals("ANY#FUNCTION"));
		} catch (ClassCastException e) {
			switch (v.getClass().getSimpleName()) {
			case "GAddExpr":
				GAddExpr ae = (GAddExpr) v;
				return isValueMethodCall(ae.getOp1(), s) || isValueMethodCall(ae.getOp2(), s);
			case "IntConstant":
				return false;
			case "JimpleLocal":
				return false;
			case "GEqExpr":
				GEqExpr ee = (GEqExpr) v;
				return isValueMethodCall(ee.getOp1(), s) || isValueMethodCall(ee.getOp2(), s);
			case "GNeExpr":
				GNeExpr ne = (GNeExpr) v;
				return isValueMethodCall(ne.getOp1(), s) || isValueMethodCall(ne.getOp2(), s);
			case "GStaticInvokeExpr":
				GStaticInvokeExpr sie = (GStaticInvokeExpr) v;
				return (sie.getMethod().getName().equals(s));
			case "GVirtualInvokeExpr":
				GVirtualInvokeExpr vie = (GVirtualInvokeExpr) v;
				return (vie.getMethod().getName().equals(s));
			case "GInstanceFieldRef":
				return false;
			case "GNewInvokeExpr":
				return false;
			default:
				if (ConstantArgs.DEBUG_MODE)
					System.err.println("case not handled isValueMethodCall: " + v.getClass().getSimpleName());
				break;
			}

			return false;
		}
	}

}
