package gimpToApp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import exceptions.ColumnDoesNotExist;
import exceptions.UnknownUnitException;
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
	Body body;
	ArrayList<Table> tables;
	// auxiliary value, used to mark the last visited Prep statement to create the
	// mapping from prepStmt to queries
	Unit lastPreparedStatementUnit;
	// auxiliary value, used to mark the last visited Prep statement to create the
	// mapping from prepStmt to execStmt
	Unit valueIsExecuteLastPrepStmt;
	// a mapping to mark the ""latest"" relation between rowSet Values and the
	// expression they represet
	// it is updated when traversing the body to find its value for each unit in the
	// body
	Map<Value, Expression> map = null;
	List<Unit> unitsWithNextCall;

	public UnitHandler(Body body, ArrayList<Table> tables) {
		data = new UnitData();
		this.body = body;
		this.tables = tables;
		this.unitsWithNextCall = new ArrayList<>();
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
			// take care of the mapping from rowSetValues to rowExps

		}
	}

	public void extractStatements() throws UnknownUnitException {
		// extract and create queries with holes
		for (Unit u : body.getUnits())
			if (data.isExecute(u)) {
				Query query = extractQuery(data.getExecuteValue(u), u);
				this.data.addQuery(lastPreparedStatementUnit, query);
			}
		// add the expressions originating from queries and patch the queries (must
		// be done together)
		Value LastRowSet = null;
		int iter = 0;
		for (Unit u : body.getUnits()) {
			// the program logic not affecting queries is abstracted
			if (data.getQueries().containsKey(u)) {
				patchQuery(u);
				updateExpressions(u);
			}
			if (data.getExecuteValue(u) != null) {
				// update the map
				try {
					LastRowSet = ((GAssignStmt) u).getLeftOp();
				} catch (ClassCastException e) {

				}
				// a list of units which call .next() on this rowSerVar
				unitsWithNextCall = data.getInvokeListFromVal(LastRowSet);
				iter = 0;
				map = new HashMap<Value, Expression>();
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

		// At this point data.exps already includes some statements (assignments).
		// now add additional invoke statements
		for (Unit u : data.getQueries().keySet())
			this.data.addStmt(new InvokeStmt(data.getQueryFromUnit(u)));
	}

	// will replace the queries with holes with patched ones
	// might need to recursive track variable outside of the scope of this iteration
	private void patchQuery(Unit u) {
		Query q = data.getQueryFromUnit(u);
		Value value = ((GAssignStmt) u).getLeftOp();
		List<Unit> executeUnits = data.getInvokeListFromVal(value);
		if (executeUnits != null)
			for (Unit eu : executeUnits) {
				InvokeExpr ieu = ((GInvokeStmt) eu).getInvokeExpr();
				if (ieu.getArgCount() != 0) {
					try {
						q.patch(Integer.parseInt(ieu.getArg(0).toString()), valueToExpression(u, ieu.getArg(1)));
					} catch (NumberFormatException | UnknownUnitException | ColumnDoesNotExist e) {
						e.printStackTrace();
					}

				}
			}

	}

	// A new rowSetVar is added to the expressions
	private void updateExpressions(Unit u) throws UnknownUnitException {
		Query q = data.getQueryFromUnit(u);
		if (q.getKind() == Kind.SELECT) {
			GAssignStmt assgnmnt = (GAssignStmt) data.getExecFromPrep(u);
			RowSetVarExp newExp = new RowSetVarExp(assgnmnt.getLeftOp().toString(), q.getTable(), q.getWhClause());
			this.data.addExp(assgnmnt.getLeftOp(), newExp);
		}
	}

	// Open Ended --- I'll add more handler upon occurence
	private Expression valueToExpression(Unit callerU, Value v) throws UnknownUnitException, ColumnDoesNotExist {
		switch (v.getClass().getSimpleName()) {
		case "GAddExpr":
			GAddExpr gae = (GAddExpr) v;
			return new BinOpExp(BinOp.PLUS, valueToExpression(callerU, gae.getOp1()),
					valueToExpression(callerU, gae.getOp2()));
		case "JimpleLocal":
			if (data.getExp(v) != null)
				return data.getExp(v);
			else
				return valueToExpression(data.getDefinedAt(v), ((GAssignStmt) data.getDefinedAt(v)).getRightOp());

		case "IntConstant":
			IntConstant ic = (IntConstant) v;
			return new ConstValExp(ic.value);
		case "LongConstant":
			LongConstant lc = (LongConstant) v;
			return new ConstValExp(lc.value);
		case "StringConstant":
			StringConstant sc = (StringConstant) v;
			return new ConstValExp(sc.value);
		case "GInterfaceInvokeExpr":
			GInterfaceInvokeExpr iie = (GInterfaceInvokeExpr) v;
			String mName = iie.getMethod().getName();
			if (mName.equals("getInt")) {
				RowVarExp rSet = (RowVarExp) data.getUTSEs().get(callerU).get(iie.getBase());
				return projectRow(rSet, iie.getArgs());
			} else if (mName.equals("getString"))
				System.out.println("===TAKE CARE OF ME");
			else if (mName.equals("getLong")) {
				RowVarExp rSet = (RowVarExp) data.getUTSEs().get(callerU).get(iie.getBase());
				return projectRow(rSet, iie.getArgs());
			}
			return new UnknownExp(mName, -1);
		default:
			return new UnknownExp("??214", -1);
		}
	}

	// given a rowVarExpression returns a new expression projecting a column
	private ProjVarExp projectRow(RowVarExp rVar, List<Value> args) throws ColumnDoesNotExist {
		assert (args.size() == 1) : "Case not handled : UnitHandler.java.projectRow";
		Value v = args.get(0);
		if (v.getType().toString().equals("java.lang.String"))
			return new ProjVarExp(rVar.getName(), rVar.getTable().getColumn(v.toString().replaceAll("\"", "")), rVar);
		else if (v.getType().toString().equals("int"))
			return new ProjVarExp(rVar.getName(), rVar.getTable().getColumn(Integer.parseInt(v.toString())), rVar);

		throw new ColumnDoesNotExist("");
	}

	// given a unit (which contains an executeQuery/executeUpdate statement) it
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
	 * Initial Handlers ------ this.data is only allowed to be updated by the
	 * following handlers
	 * 
	 */
	private void invokeInitHandler(Unit u) throws UnknownUnitException {
		GInvokeStmt expr = (GInvokeStmt) u;
		if (expr.getUseBoxes().get(expr.getUseBoxes().size() - 1).getValue().getClass().getSimpleName()
				.equals("GInterfaceInvokeExpr")) {
			Value v1 = expr.getUseBoxes().get(expr.getUseBoxes().size() - 1).getValue();
			Value value = v1.getUseBoxes().get(v1.getUseBoxes().size() - 1).getValue();
			this.data.addValToInvoke(value, u);
		}
		Value value = expr.getUseBoxes().get(expr.getUseBoxes().size() - 1).getValue();
		if (valueIsExecute(value, u)) {
			this.data.addExecuteUnit(u, value);

		}
	}

	private void assignInitHandler(Unit u) throws UnknownUnitException {
		GAssignStmt stmt = (GAssignStmt) u;
		Value rOP = stmt.getRightOp();
		Value lOP = stmt.getLeftOp();
		if (!this.data.isDefinedAtExists(lOP))
			this.data.addDefinedAt(lOP, u);
		if (valueIsExecute(rOP, u)) {
			this.data.addExecuteUnit(u, rOP);
			data.addPrepToExec(valueIsExecuteLastPrepStmt, u);
		}
	}

	// Be careful that this implementation is not complete -> for example calling
	// executeQuery inside a for or condition is not handled yet
	private boolean valueIsExecute(Value v, Unit u) throws UnknownUnitException {
		switch (v.getClass().getSimpleName()) {
		case "GStaticInvokeExpr":
			break;
		case "GVirtualInvokeExpr":
			// not interesting for this function; contains string builders which will be
			// considered else where
			break;
		case "GInstanceFieldRef":
			// not interesting for this function;
			// r0 instances fields
			break;
		case "GInterfaceInvokeExpr": // invokation on local object
			GInterfaceInvokeExpr expr = (GInterfaceInvokeExpr) v;
			String fName = expr.getMethod().getName();
			if (fName.equals("prepareStatement"))
				valueIsExecuteLastPrepStmt = u; // this can be done better in the future by NOT mapping them based
												// on just their closeness
			return (fName.equals("executeQuery") || fName.equals("executeUpdate"));
		case "GNewInvokeExpr":
			// new object creation -- string creation is interesting for this version -> but
			// not for this function yet
			break;
		case "StaticFieldRef": // not interesting -- e.g. print stream
			break;
		case "GSpecialInvokeExpr": // intra procedure calls -- not interesting for now
			break;
		case "IntConstant":
			break;
		case "JimpleLocal":
			break;

		// default:
		// throw new UnknownUnitException("Unknown Jimple/Grimp unit class: " +
		// v.getClass().getSimpleName());

		}
		return false;
	}

}
