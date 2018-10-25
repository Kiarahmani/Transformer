package gimpToApp.utils;

import java.util.List;

import exceptions.ColumnDoesNotExist;
import exceptions.UnknownUnitException;
import gimpToApp.UnitData;
import ir.statement.Query;
import soot.Unit;
import soot.Value;
import soot.grimp.internal.GAssignStmt;
import soot.grimp.internal.GInvokeStmt;
import soot.jimple.InvokeExpr;

public class QueryPatcher {
	// will replace the queries with holes with patched ones
	// might need to recursive track variable outside of the scope of this iteration
	public void patchQuery(Unit u, ValueToExpression veTranslator, UnitData data) {
		Query q = data.getQueryFromUnit(u);
		Value value = ((GAssignStmt) u).getLeftOp();
		List<Unit> executeUnits = data.getInvokeListFromVal(value);
		if (executeUnits != null)
			for (Unit eu : executeUnits) {
				try {
					// if it is an invoke, e.g. setInt
					InvokeExpr ieu = ((GInvokeStmt) eu).getInvokeExpr();
					if (ieu.getArgCount() != 0) {
						try {
							q.patch(Integer.parseInt(ieu.getArg(0).toString()),
									veTranslator.valueToExpression(u, ieu.getArg(1)));
						} catch (NumberFormatException | UnknownUnitException | ColumnDoesNotExist e) {
							e.printStackTrace();
						}

					}
				} catch (ClassCastException e) {

				}
			}
	}
}
