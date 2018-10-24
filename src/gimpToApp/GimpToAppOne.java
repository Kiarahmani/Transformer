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
		// Parameter extraction
		unitHandler.extractParams();
		for (Local l : unitHandler.data.getParams().keySet()) {
			Type t = Type.INT; // just to instanciate it, needed for calling the typing function
			Value v = unitHandler.data.getParams().get(l);
			try {
				ParamValExp exp = (ParamValExp) new ParamValExp(l.toString(), t.fromJavaTypes(v));
				txn.addParam(l.toString(), exp);
				// Also add it the unit data for future reference
				unitHandler.data.addExp(l, exp);

			} catch (SqlTypeNotFoundException e) {
				e.printStackTrace();
			}
		}

		unitHandler.InitialAnalysis();
		unitHandler.extractStatements();
		// at this point the unitHandler.data must include all the extracted statements,
		// some of which are actually vars and vals (wrapped in trivial
		// assignmentStatments)

		// craft the output transaction from the extracted data
		for (Statement s : unitHandler.data.getStmts())
			txn.addStmt(s);
		txn.setTypes();

		System.out.println("====================================");
		System.out.println("===	ALL EXPRESSIONS");
		for (Value x : unitHandler.data.getExps().keySet()) {
			System.out.println(x + " := " + unitHandler.data.getExps().get(x));
		}
		System.out.println("====================================");
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
