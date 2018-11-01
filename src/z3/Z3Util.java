package z3;

import com.microsoft.z3.ArithExpr;
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
import ir.expression.UnOpExp.UnOp;

public class Z3Util {
	Context ctx;
	DeclaredObjects objs;
	private int loopCount = 0;

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
			return ctx.mkApp(projFunc, irCondToZ3Expr(txnName, txn, row, pv.getRVar()));
		case "RowSetVarExp":
			break;
		case "RowVarExp":
			RowVarExp ve = (RowVarExp) cond;
			FuncDecl rowFunc = objs.getfuncs(txnName + "_" + ve.getName());
			return ctx.mkApp(rowFunc, txn);
		case "RowVarLoopExp":
			RowVarLoopExp vle = (RowVarLoopExp) cond;
			FuncDecl loopVarFunc = objs.getfuncs(txnName + "_" + vle.getName());
			return ctx.mkApp(loopVarFunc, txn, ctx.mkInt(loopCount++));
		case "UnknownExp":
			break;
		case "BinOpExp":
			BinOpExp boe = (BinOpExp) cond; {
			switch (boe.op) {
			case EQ:
				return ctx.mkEq(irCondToZ3Expr(txnName, txn, row, boe.e1), irCondToZ3Expr(txnName, txn, row, boe.e2));
			case PLUS:
				return ctx.mkAdd((ArithExpr) irCondToZ3Expr(txnName, txn, row, boe.e1),
						(ArithExpr) irCondToZ3Expr(txnName, txn, row, boe.e2));
			case MINUS:
				return ctx.mkSub((ArithExpr) irCondToZ3Expr(txnName, txn, row, boe.e1),
						(ArithExpr) irCondToZ3Expr(txnName, txn, row, boe.e2));
			case MULT:
				return ctx.mkMul((ArithExpr) irCondToZ3Expr(txnName, txn, row, boe.e1),
						(ArithExpr) irCondToZ3Expr(txnName, txn, row, boe.e2));
			case DIV:
				return ctx.mkDiv((ArithExpr) irCondToZ3Expr(txnName, txn, row, boe.e1),
						(ArithExpr) irCondToZ3Expr(txnName, txn, row, boe.e2));
			case AND:
				return ctx.mkAnd((BoolExpr) irCondToZ3Expr(txnName, txn, row, boe.e1),
						(BoolExpr) irCondToZ3Expr(txnName, txn, row, boe.e2));
			case OR:
				return ctx.mkOr((BoolExpr) irCondToZ3Expr(txnName, txn, row, boe.e1),
						(BoolExpr) irCondToZ3Expr(txnName, txn, row, boe.e2));
			case XOR:
				return ctx.mkXor((BoolExpr) irCondToZ3Expr(txnName, txn, row, boe.e1),
						(BoolExpr) irCondToZ3Expr(txnName, txn, row, boe.e2));
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
			UnOpExp uoe = (UnOpExp) cond;
			if (uoe.equals(UnOp.NOT))
				return ctx.mkNot((BoolExpr) irCondToZ3Expr(txnName, txn, row, uoe.e));
			else
				break;
		}
		throw new UnexoectedOrUnhandledConditionalExpression(
				"--irCondToZ3Expr case not handled yet: " + cond.getClass().getSimpleName());
	}
}
