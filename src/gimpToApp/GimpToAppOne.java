package gimpToApp;

import java.util.ArrayList;

import exceptions.UnknownUnitException;
import ir.Application;
import ir.Transaction;
import ir.schema.Table;
import ir.statement.InvokeStmt;
import ir.statement.Statement;
import soot.Body;
import soot.Local;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.UnitBox;
import soot.Value;
import soot.ValueBox;
import soot.grimp.internal.GAssignStmt;
import soot.grimp.internal.GInterfaceInvokeExpr;
import soot.grimp.internal.GInvokeStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.JAssignStmt;

public class GimpToAppOne extends GimpToApp {
	Statement sqlStmt;

	public GimpToAppOne(Scene v2, ArrayList<Body> bodies, ArrayList<Table> tables) {
		super(v2, bodies, tables);
	}

	public Application transform() throws UnknownUnitException {
		Application app = new Application();
		for (Body b : bodies) {
			app.addTxn(extractTxn(b));
			break;
		}
		return app;
	}

	private Transaction extractTxn(Body b) throws UnknownUnitException {
		String name = b.getMethod().getName();
		Transaction txn = new Transaction(name);
		System.out.println("\n---\nExtracting a transaction from Gimp body (" + name + ")");
		for (Unit u : b.getUnits()) {
			if (updateLocals(u, b)) {
				System.out.println(String.format("%0" + 150 + "d", 0).replace("0", "-"));
				txn.addStmt(sqlStmt);
			}
		}
		// super.printGimpBody(b);
		return txn;
	}

	// updates the local data-structures and tells if we are ready to extract a
	// stetement or not
	private boolean updateLocals(Unit u, Body b) throws UnknownUnitException {
		switch (u.getClass().getSimpleName()) {
		case "GIdentityStmt":
			return identityStmtHandler(u, b);
		case "GAssignStmt":
			return assignStmtHandler(u, b);
		case "GInvokeStmt":
			return invokeStmtHandler(u, b);
		case "JGotoStmt":
			return gotoStmtHandler(u, b);
		case "GThrowStmt":
			return throwStmtHandler(u, b);
		case "JReturnVoidStmt":
			return returnVoidStmtHandler(u, b);
		case "GIfStmt":
			return ifStmtHandler(u, b);
		default:
			throw new UnknownUnitException(
					"Unknown/Unimplemented Jimple/Grimp unit class: " + u.getClass().getSimpleName());
		}
	}

	/*
	 * Grimp unit handlers
	 */

	private boolean ifStmtHandler(Unit u, Body b) {
		return false;
	}

	private boolean returnVoidStmtHandler(Unit u, Body b) {
		return false;
	}

	private boolean throwStmtHandler(Unit u, Body b) {
		return false;
	}

	private boolean gotoStmtHandler(Unit u, Body b) {
		return false;
	}

	private boolean identityStmtHandler(Unit u, Body b) {
		return false;
	}

	private boolean invokeStmtHandler(Unit u, Body b) {
		// first check if there is an SQL statment that we can extract
		sqlStmt = null;
		sqlStmt = extractSQLInvokation(u, b);
		return (sqlStmt != null);
	}

	private boolean assignStmtHandler(Unit u, Body b) {
		// first check if there is an SQL statment that we can extract
		sqlStmt = null;
		sqlStmt = extractSQLInvokation(u, b);
		return (sqlStmt != null);
	}

	/*
	 * 
	 * 
	 * 
	 */

	public InvokeStmt extractSQLInvokation(Unit u, Body b) {
		// determine if this unit is Query/Update execution
		SootMethod method = extractMethod(u);
		if (method != null && isSqlExecute(method)) {
			System.out.println("EXECUTE QUERY LOCATED:");
			System.out.println("UNIT: " + u);
			findPreparedStatement(null, u, b);
		}
		return new InvokeStmt();
	}

	private Object findPreparedStatement(Value v, Unit u, Body b) {
		if (isGAssignStmt(u)) {
			if (v == null)
				v = extractMethodBase(u);
			if (!(isDefinedHere(v, u) && containsSqlStmt(u))) {
				Unit prev = b.getUnits().getPredOf(u);
				System.out.println(".");
				findPreparedStatement(v, prev, b);
			} else {
				System.out.println("FOUND IT: " + u);
			}
		}
		if (isGInvokeStmt(u)) {
			Unit prev = b.getUnits().getPredOf(u);
			System.out.println(".");
			findPreparedStatement(v, prev, b);
		}
		return null;
	}

}

/*
 * 
 * 
 * 
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 * 
 */
