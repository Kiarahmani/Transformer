package gimpToApp;

import java.util.ArrayList;
import java.util.List;

import exceptions.SqlTypeNotFoundException;
import exceptions.UnknownUnitException;
import ir.Application;
import ir.Transaction;
import ir.Type;
import ir.expression.Expression;
import ir.expression.vals.ParamValExp;
import ir.expression.vars.PrimitiveVarExp;
import ir.expression.vars.RowSetVarExp;
import ir.expression.vars.RowVarExp;
import ir.expression.vars.VarExp;
import ir.schema.Table;
import ir.statement.AssignmentStmt;
import ir.statement.Statement;
import soot.Body;
import soot.Local;
import soot.Scene;
import soot.Unit;
import soot.UnitPatchingChain;
import soot.Value;
import soot.ValueBox;

public class GimpToAppOne extends GimpToApp {

	public GimpToAppOne(Scene v2, ArrayList<Body> bodies, ArrayList<Table> tables) {
		super(v2, bodies, tables);
	}

	public Application transform() throws UnknownUnitException {
		Application app = new Application();
		for (Body b : bodies) {
			Transaction txn = extractTxn(b);
			if (txn != null && !txn.getName().contains("init"))
				app.addTxn(txn);
		}
		return app;
	}

	private Transaction extractTxn(Body b) throws UnknownUnitException {

		super.printGimpBody(b);
		String name = b.getMethod().getName();
		Transaction txn = new Transaction(name);
		UnitHandler unitHandler = new UnitHandler(b, super.tables);

		// INTERNAL ANALYSIS
		// extraction jobs
		unitHandler.extractParams();
		for (Unit u : b.getUnits())
			unitHandler.InitialAnalysis(u); // does nothing so far

		for (Unit u : b.getUnits())
			unitHandler.extractStatements(u);

		// OUTPUT GENERATION
		// add the parameters to the output transaction
		for (Local l : unitHandler.data.getParams().keySet()) {
			Type t = Type.INT;
			Value v = unitHandler.data.getParams().get(l);
			try {
				txn.addParam(l.toString(), new ParamValExp(l.toString(), t.fromJavaTypes(v)));
			} catch (SqlTypeNotFoundException e) {
				e.printStackTrace();
			}
		}
		// craft the output transaction from the extracted data
		for (Statement s : unitHandler.data.getStmts())
			txn.addStmt(s);

		// XXXXXXXXXX
		// test an assignment statement
//		Statement testStmt = new AssignmentStmt(new RowSetVarExp("x", tables.get(0)), new Expression());
//		txn.addStmt(testStmt);
//		testStmt = new AssignmentStmt(new RowVarExp("y", tables.get(2)), new Expression());
//		txn.addStmt(testStmt);
//		testStmt = new AssignmentStmt(new PrimitiveVarExp("z", Type.REAL), new Expression());
//		txn.addStmt(testStmt);
		/*
		 * XXXXXXXXXX
		 */
		txn.setTypes();
		return txn;

	}

	private void printUnit(Unit u, int iter) {
		System.out.print("(" + iter + ")\n");
		System.out.println(" ╰──" + u.getClass());
		System.out.println(" ╰──" + u);
		System.out.println(String.format("%0" + 120 + "d", 0).replace("0", "-"));
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
