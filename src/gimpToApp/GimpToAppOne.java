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

public class GimpToAppOne extends GimpToApp {

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
		List<GValue> gValueList = new ArrayList<GValue>();
		System.out.println("\n---\nExtracting a transaction from Gimp body (" + name + ")");
		for (Unit u : b.getUnits()) {
			 System.out.println("UNIT: "+u+"\n--");
			GValueExtractor gve = new GValueExtractor(u);
			if (gve.lhs != null&&gve.rhs != null) {
				gve.lhs.print();
				System.out.println("===");
			}
			if (gve.rhs != null)
				gve.rhs.print();
			// gValueList.add();
			System.out.println(String.format("%0" + 120 + "d", 0).replace("0", "-"));

		}
		// super.printGimpBody(b);
		return txn;
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
