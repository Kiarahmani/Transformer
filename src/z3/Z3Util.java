package z3;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.Sort;

import exceptions.UnexoectedOrUnhandledConditionalExpression;
import ir.expression.*;
import ir.expression.vars.*;
import ir.expression.vals.*;
import ir.expression.Expression;

public class Z3Util {
	Context ctx;
	DeclaredObjects objs;

	public Z3Util(Context ctx, DeclaredObjects objs) {
		this.ctx = ctx;
		this.objs = objs;
	}

	public Expr irCondToZ3Expr(String txnName, Expr txn, Expr row, Expression cond)
			throws UnexoectedOrUnhandledConditionalExpression {

		switch (cond.getClass().getSimpleName()) {
		case "ConstValExp":
			ConstValExp cve = (ConstValExp) cond; {
			switch (cve.getType()) {
			case INT:
				return ctx.mkInt(cve.getIVal());
			case BOOLEAN:
				return ctx.mkBool(cve.isBVal());
			case REAL:
				return ctx.mkReal((int) cve.getDVal());
			case STRING:
				return ctx.MkString(cve.getSVal());
			}

		}
		case "FieldAccessValExp":
			break;
		case "NullExp":
			break;
		case "ParamValExp":
			ParamValExp pave = (ParamValExp) cond;
			return ctx.mkApp(objs.getfuncs(txnName + "_PARAM_" + pave.getName()), txn);
		case "ProjValExp":
			ProjValExp pve = (ProjValExp) cond;
			return ctx.mkApp(objs.getfuncs(pve.table.getName() + "_PROJ_" + pve.column.name), row);
		case "PrimitiveVarExp":
			break;
		case "ProjVarExp":
			ProjVarExp pv = (ProjVarExp) cond;
			FuncDecl projFunc = objs.getfuncs(pv.getRVar().getTable().getName() + "_PROJ_" + pv.getColumn().toString());
			// XXXXXXXXXXXXX
			Sort rowSort = objs.getSort(pv.getRVar().getTable().getName());
			return ctx.mkApp(projFunc, irCondToZ3Expr(txnName, txn, row, pv.getRVar()));
		// not sure about what I am returning above -> requires handling RowVarExp case
		// first
		case "RowSetVarExp":
			break;
		case "RowVarExp":
			/////////////// CASE TO BE HANDLED
			RowVarExp ve = (RowVarExp) cond;
			System.out.println("=========" + ve.getName());
			break;
		case "RowVarLoopExp":
			break;
		case "UnknownExp":
			break;
		case "BinOpExp":
			BinOpExp boe = (BinOpExp) cond; {
			switch (boe.op) {
			case EQ:
				return ctx.mkEq(irCondToZ3Expr(txnName, txn, row, boe.e1), irCondToZ3Expr(txnName, txn, row, boe.e2));
			case PLUS:
				break;
			case MINUS:
				break;
			case MULT:
				break;
			case DIV:
				break;
			case AND:
				break;
			case OR:
				break;
			case XOR:
				break;
			case GEQ:
				break;
			case LEQ:
				break;
			case LT:
				break;
			case GT:
				break;
			default:
				break;
			}
		}
			break;
		case "UnOpExp":
			break;

		}
		throw new UnexoectedOrUnhandledConditionalExpression(
				"--irCondToZ3Expr case not handled yet: " + cond.getClass().getSimpleName());
	}
}
