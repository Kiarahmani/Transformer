package gimpToApp;

import java.util.ArrayList;

import ir.Application;
import ir.Transaction;
import ir.schema.Table;
import ir.statement.InvokeStmt;
import soot.Body;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.grimp.internal.GAssignStmt;
import soot.grimp.internal.GInterfaceInvokeExpr;
import soot.grimp.internal.GInvokeStmt;
import soot.jimple.InvokeExpr;

public class GimpToApp {
	private Scene v;
	ArrayList<Body> bodies;
	ArrayList<Table> tables;

	public GimpToApp(Scene v2, ArrayList<Body> bodies, ArrayList<Table> tables) {
		this.v = v;
		this.tables = tables;
		this.bodies = bodies;
	}

	/*
	 * 
	 * pretty printing the Gimp bodies for developement and debug
	 * 
	 */
	public void printGimpBody(Body b) {
		int _line_length = 120;
		System.out.println("\n\n" + String.format("%0" + _line_length + "d", 0).replace("0", "="));
		System.out.println("Txn: " + b.getMethod());
		System.out.println(String.format("%0" + _line_length + "d", 0).replace("0", "="));
		int iter = 1;
		for (Unit u : b.getUnits()) {
			System.out.print("(" + iter + ")\n");
			System.out.println(" ╰──" + u.getClass());
			System.out.println(" ╰──" + u);
			System.out.println(String.format("%0" + _line_length + "d", 0).replace("0", "-"));
			iter++;
		}
	}

	protected boolean isGInvokeStmt(Unit u) {
		return u.getClass().getSimpleName().equals("GInvokeStmt");
	}

	protected boolean isGAssignStmt(Unit u) {
		return u.getClass().getSimpleName().equals("GAssignStmt");
	}

	protected boolean isSqlExecute(SootMethod method) {
		return method.getName().equals("executeQuery") || method.getName().equals("executeUpdate")
				|| method.getName().equals("execute");
	}

	/*
	 * 
	 * 
	 * The followings are TEMP MUST BE GENERALIZED
	 * 
	 * 
	 * 
	 * 
	 */
	protected boolean containsSqlStmt(Unit u) {
		// TODO Auto-generated method stub
		SootMethod method = extractMethod(u);
		return (method != null) && method.getName().equals("prepareStatement");
	}

	protected SootMethod extractMethod(Unit u) {
		SootMethod method = null;
		if (isGInvokeStmt(u)) {
			InvokeExpr expr = ((GInvokeStmt) u).getInvokeExpr();
			method = expr.getMethod();
		} else if (isGAssignStmt(u)) {
			GAssignStmt currStmt = (GAssignStmt) u;
			if (currStmt.getRightOp().getClass().getSimpleName().equals("GInterfaceInvokeExpr"))
				method = ((GInterfaceInvokeExpr) currStmt.getRightOp()).getMethod();
		} else {
			System.err.println("unknown/unexpected unit class name");
		}
		return method;
	}

	protected boolean isDefinedHere(Value v, Unit u) {
		for (ValueBox x : u.getDefBoxes()) {
			if (x.getValue() == v)
				return true;
		}
		return false;
	}

	protected Value extractMethodBase(Unit u) {
		Value val = null;
		if (isGInvokeStmt(u)) {
			// not doing anything... TODO
		} else if (isGAssignStmt(u)) {
			GAssignStmt currStmt = (GAssignStmt) u;
			if (currStmt.getRightOp().getClass().getSimpleName().equals("GInterfaceInvokeExpr"))
				val = ((GInterfaceInvokeExpr) currStmt.getRightOp()).getBase();
		} else {
			System.err.println("unknown/unexpected unit class name");
		}
		return val;
	}

}
