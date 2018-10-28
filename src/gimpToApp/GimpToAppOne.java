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
import z3.ConstantArgs;

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

		if (ConstantArgs.DEBUG_MODE)
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
				// Also add it the unit data
				unitHandler.data.addExp(l, exp);
			} catch (SqlTypeNotFoundException e) {
				e.printStackTrace();
			}
		}

		unitHandler.InitialAnalysis();
		unitHandler.extractStatements();
		// craft the output transaction from the extracted data
		for (Statement s : unitHandler.data.getStmts())
			txn.addStmt(s);
		txn.setTypes();
		printExpressions(unitHandler);
		return txn;

	}

	// just a helping function for dev phase
	private void printExpressions(UnitHandler unitHandler) {
		if (ConstantArgs.DEBUG_MODE) {
			System.out.println("===== LOOPS");
			for (Unit x : unitHandler.data.units)
				if (unitHandler.data.getLoopNo(x) == -1)
					System.out.println("" + unitHandler.data.units.indexOf(x));
				else
					System.out.println(
							"__" + unitHandler.data.units.indexOf(x) + "(" + unitHandler.data.getLoopNo(x) + ")");
		}

		System.out.println("=============================");
		System.out.println("===	VARIABLES");
		for (Value x : unitHandler.data.getExps().keySet()) {
			System.out.println(x + " := " + unitHandler.data.getExps().get(x));
		}
		System.out.println("=============================");
	}

}
