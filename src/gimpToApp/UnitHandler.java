package gimpToApp;

import java.util.ArrayList;

import exceptions.UnknownUnitException;
import ir.schema.Table;
import ir.statement.InvokeStmt;
import ir.statement.Query;
import ir.statement.SqlStmtType;
import ir.statement.Statement;
import soot.Body;
import soot.Local;
import soot.Unit;
import soot.UnitBox;
import soot.Value;
import soot.grimp.internal.GAssignStmt;
import soot.grimp.internal.GInstanceFieldRef;
import soot.grimp.internal.GInterfaceInvokeExpr;
import soot.grimp.internal.GInvokeStmt;
import soot.grimp.internal.GNewInvokeExpr;
import soot.grimp.internal.GStaticInvokeExpr;
import soot.grimp.internal.GVirtualInvokeExpr;
import soot.jimple.StringConstant;
import soot.jimple.internal.AbstractInvokeExpr;

public class UnitHandler {
	UnitData data;
	Body body;
	ArrayList<Table> tables;

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
	public void InitialAnalysis(Unit u) throws UnknownUnitException {
		// System.out.println("==="+u);
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

	// updates the UnitData (adds a statement to its list if one is extracable from
	// the given unit)
	public void extractStatements(Unit u) throws UnknownUnitException {
		// System.out.println(String.format("%0" + 120 + "d", 0).replace("0", "-"));
		if (data.isExecute(u)) {
			Query query = extractQuery(data.getExecuteValue(u), u);
			Statement stmt = new InvokeStmt(query);
			this.data.addStmt(stmt);
		}

	}

	// given a unit (which contains an executeQuery/executeUpdate statement) it
	// searches backward for its statement;
	private Query extractQuery(Value v, Unit u) throws UnknownUnitException {
		switch (v.getClass().getSimpleName()) {
		case "GInterfaceInvokeExpr":
			GInterfaceInvokeExpr expr = (GInterfaceInvokeExpr) v;
			if (expr.getMethod().getName().equals("prepareStatement")) {
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
			Query query = new Query(expr2.toString(),data,tables);
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
			System.out.println("==="+expr6);
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
		Value value = expr.getUseBoxes().get(expr.getUseBoxes().size() - 1).getValue();
		if (valueIsExecute(value))
			this.data.addExecuteUnit(u, value);
	}

	private void assignInitHandler(Unit u) throws UnknownUnitException {
		GAssignStmt stmt = (GAssignStmt) u;
		Value rOP = stmt.getRightOp();
		Value lOP = stmt.getLeftOp();
		if (!this.data.isDefinedAtExists(lOP))
			this.data.addDefinedAt(lOP, u);
		if (valueIsExecute(rOP))
			this.data.addExecuteUnit(u, rOP);
	}

	// Be careful that this implementation is not complete -> for example calling
	// executeQuery inside a for or condition is not handled yet
	private boolean valueIsExecute(Value v) throws UnknownUnitException {
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
