package gimpToApp.utils;

import java.util.List;

import exceptions.ColumnDoesNotExist;
import exceptions.UnknownUnitException;
import gimpToApp.UnitData;
import ir.expression.BinOpExp;
import ir.expression.Expression;
import ir.expression.BinOpExp.BinOp;
import ir.expression.vals.ConstValExp;
import ir.expression.vars.ProjVarExp;
import ir.expression.vars.RowVarExp;
import ir.expression.vars.UnknownExp;
import soot.Unit;
import soot.Value;
import soot.grimp.internal.GAddExpr;
import soot.grimp.internal.GAssignStmt;
import soot.grimp.internal.GInterfaceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.LongConstant;
import soot.jimple.StringConstant;

public class ValueToExpression {

	static UnitData data;

	public ValueToExpression(UnitData data) {
		this.data = data;
	}

	// Open Ended --- I'll add more handler upon occurence
	public Expression valueToExpression(Unit callerU, Value v) throws UnknownUnitException, ColumnDoesNotExist {
		switch (v.getClass().getSimpleName()) {
		case "GAddExpr":
			GAddExpr gae = (GAddExpr) v;
			return new BinOpExp(BinOp.PLUS, valueToExpression(callerU, gae.getOp1()),
					valueToExpression(callerU, gae.getOp2()));
		case "JimpleLocal":
			if (data.getExp(v) != null)
				return data.getExp(v);
			else
				return valueToExpression(data.getDefinedAt(v), ((GAssignStmt) data.getDefinedAt(v)).getRightOp());
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
			String mName = iie.getMethod().getName();
			if (mName.equals("getInt")) {
				System.out.println("===" + data.getUTSEs().get(callerU));
				RowVarExp rSet = (RowVarExp) data.getUTSEs().get(callerU).get(iie.getBase());
				return projectRow(rSet, iie.getArgs());
			} else if (mName.equals("getString"))
				System.out.println("===TAKE CARE OF ME");
			else if (mName.equals("getLong")) {
				RowVarExp rSet = (RowVarExp) data.getUTSEs().get(callerU).get(iie.getBase());
				return projectRow(rSet, iie.getArgs());
			}
			return new UnknownExp(mName, -1);
		default:
			return new UnknownExp("??214", -1);
		}
	}

	/*
	 * 
	 * 
	 * 
	 *
	 * given a rowVarExpression returns a new expression projecting a column
	 */
	private ProjVarExp projectRow(RowVarExp rVar, List<Value> args) throws ColumnDoesNotExist {
		assert (args.size() == 1) : "Case not handled : UnitHandler.java.projectRow";
		Value v = args.get(0);
		if (v.getType().toString().equals("java.lang.String"))
			return new ProjVarExp(rVar.getName(), rVar.getTable().getColumn(v.toString().replaceAll("\"", "")), rVar);
		else if (v.getType().toString().equals("int"))
			return new ProjVarExp(rVar.getName(), rVar.getTable().getColumn(Integer.parseInt(v.toString())), rVar);

		throw new ColumnDoesNotExist("");
	}

}
