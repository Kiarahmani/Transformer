package gimpToApp;

import java.util.ArrayList;
import java.util.List;

import exceptions.UnknownUnitException;
import ir.Application;
import ir.Transaction;
import ir.schema.Table;
import ir.statement.Statement;
import soot.Body;
import soot.Scene;
import soot.Unit;
import soot.UnitPatchingChain;

public class GimpToAppOne extends GimpToApp {

	public GimpToAppOne(Scene v2, ArrayList<Body> bodies, ArrayList<Table> tables) {
		super(v2, bodies, tables);
	}

	public Application transform() throws UnknownUnitException {
		Application app = new Application();
		for (Body b : bodies) {
			Transaction txn = extractTxn(b);
			if (!txn.getName().contains("init"))
				app.addTxn(txn);
		}
		return app;
	}

	private Transaction extractTxn(Body b) throws UnknownUnitException {
		String name = b.getMethod().getName();
		Transaction txn = new Transaction(name);
		UnitHandler unitHandler = new UnitHandler(b);
		for (Unit u : b.getUnits())
			unitHandler.InitialAnalysis(u); // does nothing so far

		for (Unit u : b.getUnits())
			unitHandler.extractStatements(u);

		// craft the output transaction from the extracted data
		for (Statement s : unitHandler.data.getStmts())
			txn.addStmt(s);
		txn.setTypes();
		txn.printTxn();
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
