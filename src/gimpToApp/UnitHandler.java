package gimpToApp;

import java.util.ArrayList;
import java.util.List;

import exceptions.UnknownUnitException;
import ir.expression.BinOpExp;
import ir.expression.Expression;
import ir.expression.vals.ConstValExp;
import ir.expression.vals.FieldAccessValExp;
import ir.expression.BinOpExp.BinOp;
import ir.expression.vars.RowSetVarExp;
import ir.expression.vars.UnknownExp;
import ir.schema.Table;
import ir.statement.InvokeStmt;
import ir.statement.Query;
import ir.statement.Query.Kind;
import ir.statement.SqlStmtType;
import ir.statement.Statement;
import soot.Body;
import soot.Local;
import soot.Unit;
import soot.UnitBox;
import soot.Value;
import soot.baf.toolkits.base.ExamplePeephole;
import soot.grimp.internal.GAddExpr;
import soot.grimp.internal.GAssignStmt;
import soot.grimp.internal.GInstanceFieldRef;
import soot.grimp.internal.GInterfaceInvokeExpr;
import soot.grimp.internal.GInvokeStmt;
import soot.grimp.internal.GNewInvokeExpr;
import soot.grimp.internal.GStaticInvokeExpr;
import soot.grimp.internal.GVirtualInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.LongConstant;
import soot.jimple.StringConstant;
import soot.jimple.internal.AbstractInvokeExpr;

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

	public UnitHandler(Body body, ArrayList<Table> tables) {
		data = new UnitData();
		this.body = body;
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

	public void extractStatements() throws UnknownUnitException {
		// extract and create queries with holes
		for (Unit u : body.getUnits())
			if (data.isExecute(u)) {
				Query query = extractQuery(data.getExecuteValue(u), u);
				this.data.addQuery(lastPreparedStatementUnit, query);
			}
		// add the expressions originating from queries and patching the queries (must
		// be done together)
		for (Unit u : body.getUnits())
			extractAssignments(u);

		// At this point data.exps already includes some statements (assignments).
		// now add additional invoke statements
		for (Unit u : data.getQueries().keySet()) {
			this.data.addStmt(new InvokeStmt(data.getQueryFromUnit(u)));
		}

	}

	private void extractAssignments(Unit u) throws UnknownUnitException {
		// the program logic not affecting queries is abstracted
		if (data.getQueries().containsKey(u)) {
			patchQuery(u);
			updateExpressions(u);
		}

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
						q.patch(Integer.parseInt(ieu.getArg(0).toString()), valueToExpression(ieu.getArg(1)));
					} catch (NumberFormatException | UnknownUnitException e) {
						e.printStackTrace();
					}

				}
			}

	}

	// X
	// Open Ended --- I'll add more handler upon occurence
	private Expression valueToExpression(Value v) throws UnknownUnitException {
		switch (v.getClass().getSimpleName()) {
		case "GAddExpr":
			GAddExpr gae = (GAddExpr) v;
			return new BinOpExp(BinOp.PLUS, valueToExpression(gae.getOp1()), valueToExpression(gae.getOp2()));
		case "JimpleLocal":
			return data.getExp(v);
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
			System.out.println("====" + iie.getArgs());
			return new UnknownExp("NEEDS TO BE DONE!", -1);

		// return data.getExp(iie.getBase());

		default:
			throw new UnknownUnitException("Unhandled Soot Value/Class: " + v + "/" + v.getClass().getSimpleName());
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

		//
		//
		//
		//
		//
		//

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
		if (valueIsExecute(value, u))
			this.data.addExecuteUnit(u, value);
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
				valueIsExecuteLastPrepStmt = u;
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
