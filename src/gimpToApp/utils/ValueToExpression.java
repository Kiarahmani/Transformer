package gimpToApp.utils;

import java.util.List;

import exceptions.ColumnDoesNotExist;
import exceptions.UnknownUnitException;
import gimpToApp.UnitData;
import ir.Type;
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
import soot.grimp.internal.GLeExpr;
import soot.grimp.internal.GLtExpr;
import soot.grimp.internal.GMulExpr;
import soot.jimple.IntConstant;
import soot.jimple.LongConstant;
import soot.jimple.StringConstant;
import soot.jimple.toolkits.infoflow.FakeJimpleLocal;

public class ValueToExpression {

	static UnitData data;

	public ValueToExpression(UnitData data) {
		this.data = data;
	}

	// Open Ended --- I'll add more handler upon occurence
	public Expression valueToExpression(Type tp, Unit callerU, Value v)
			throws UnknownUnitException, ColumnDoesNotExist {
		switch (v.getClass().getSimpleName()) {
		case "GAddExpr":
			GAddExpr gae = (GAddExpr) v;
			return new BinOpExp(BinOp.PLUS, valueToExpression(tp, callerU, gae.getOp1()),
					valueToExpression(tp, callerU, gae.getOp2()));
		case "GMulExpr":
			GMulExpr gme = (GMulExpr) v;
			return new BinOpExp(BinOp.MULT, valueToExpression(tp, callerU, gme.getOp1()),
					valueToExpression(tp, callerU, gme.getOp2()));
		case "GLeExpr":
			GLeExpr gle = (GLeExpr) v;
			return new BinOpExp(BinOp.LEQ, valueToExpression(Type.REAL, callerU, gle.getOp1()),
					valueToExpression(Type.REAL, callerU, gle.getOp2()));
		case "GLtExpr":
			GLtExpr glt = (GLtExpr) v;
			return new BinOpExp(BinOp.LEQ, valueToExpression(Type.REAL, callerU, glt.getOp1()),
					valueToExpression(Type.REAL, callerU, glt.getOp2()));
		case "JimpleLocal":
			if (data.getExp(v) != null)
				return data.getExp(v);
			else
				return valueToExpression(tp, data.getDefinedAt(v), ((GAssignStmt) data.getDefinedAt(v)).getRightOp());
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
			Expression result;
			if (mName.equals("getInt")) {
				RowVarExp rSet = (RowVarExp) data.getUTSEs().get(callerU).get(iie.getBase());
				result = projectRow(rSet, iie.getArgs());
				data.addExp(new FakeJimpleLocal(rSet.getName() + "_proj", null, null), result);
				return result;
			} else if (mName.equals("getString")) {
				RowVarExp rSet = (RowVarExp) data.getUTSEs().get(callerU).get(iie.getBase());
				result = projectRow(rSet, iie.getArgs());
				data.addExp(new FakeJimpleLocal(rSet.getName() + "_proj", null, null), result);
				return result;
			} else if (mName.equals("getLong")) {
				RowVarExp rSet = (RowVarExp) data.getUTSEs().get(callerU).get(iie.getBase());
				result = projectRow(rSet, iie.getArgs());
				data.addExp(new FakeJimpleLocal(rSet.getName() + "_proj", null, null), result);
				return result;
			}

		default:
			String resName = "Abs-" + tp + "#" + (data.absIter++);
			System.err
					.println(v.getClass().getSimpleName() + " - Unhandled case - will abstract to: " + resName + "\n");
			Expression defResult = new UnknownExp(resName, -1);
			data.addExp(new FakeJimpleLocal(resName, null, null), defResult);
			return defResult;
		}
	}

	/*
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
