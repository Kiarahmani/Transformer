package z3;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.Quantifier;
import com.microsoft.z3.Sort;

import exceptions.UnexoectedOrUnhandledConditionalExpression;
import ir.Application;
import ir.Transaction;
import ir.statement.InvokeStmt;
import ir.statement.Query;
import ir.statement.Statement;
import ir.statement.Query.Kind;

public class Rules {

	Context ctx;
	Application app;
	DeclaredObjects objs;
	Expr o1, o2, o3;
	Z3Util z3Util;

	public Rules(Context ctx, DeclaredObjects objs, Application app) {
		this.app = app;
		this.ctx = ctx;
		this.objs = objs;
		this.z3Util = new Z3Util(ctx, objs);
		o1 = ctx.mkFreshConst("o", objs.getSort("O"));
		o2 = ctx.mkFreshConst("o", objs.getSort("O"));
		o3 = ctx.mkFreshConst("o", objs.getSort("O"));
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// RW THEN
	//
	////////////////////////
	public List<BoolExpr> return_conditions_rw_then(FuncDecl t1, FuncDecl t2, Expr vo1, Expr vo2, Expr vt1, Expr vt2)
			throws UnexoectedOrUnhandledConditionalExpression {
		Transaction txn1 = app.getTxnByName(t1.getName().toString());
		Transaction txn2 = app.getTxnByName(t2.getName().toString());
		List<BoolExpr> result = new ArrayList<BoolExpr>();
		// these have to be removed
		for (Statement o1 : txn1.getStmts())
			for (Statement o2 : txn2.getStmts()) {
				// generate constarints sharet beween cases:
				BoolExpr otypeCond1 = ctx.mkEq(ctx.mkApp(objs.getfuncs("otype"), vo1),
						ctx.mkApp(objs.getConstructor("OType", ((InvokeStmt) o1).getType().toString())));
				BoolExpr otypeCond2 = ctx.mkEq(ctx.mkApp(objs.getfuncs("otype"), vo2),
						ctx.mkApp(objs.getConstructor("OType", ((InvokeStmt) o2).getType().toString())));
				Query q1 = ((InvokeStmt) o1).getQuery();
				Query q2 = ((InvokeStmt) o2).getQuery();

				// add the conditions if there is a common table between statements
				if (q1.getTable().equals(q2.getTable())) {
					if (q1.getKind() == Kind.SELECT && q2.getKind() == Kind.UPDATE) {
						String tableName = q1.getTable().getName();
						Sort rowSort = objs.getSort(tableName);
						Expr rowVar = ctx.mkFreshConst("r", rowSort);
						BoolExpr whereClause1 = (BoolExpr) z3Util.irCondToZ3Expr(txn1.getName(), vt1, rowVar,
								q1.getWhClause());
						BoolExpr whereClause2 = (BoolExpr) z3Util.irCondToZ3Expr(txn2.getName(), vt2, rowVar,
								q2.getWhClause());
						BoolExpr pathCond1 = (BoolExpr) z3Util.irCondToZ3Expr(txn1.getName(), vt1, rowVar,
								o1.getPathCond());
						BoolExpr pathCond2 = (BoolExpr) z3Util.irCondToZ3Expr(txn2.getName(), vt2, rowVar,
								o2.getPathCond());
						BoolExpr aliveCond = (BoolExpr) ctx.mkApp(objs.getfuncs("IsAlive_" + tableName), rowVar, vo1);
						BoolExpr rwOnTableCond = (BoolExpr) ctx.mkApp(objs.getfuncs("RW_O_" + tableName), rowVar, vo1,
								vo2);
						Expr body = ctx.mkAnd(otypeCond1, otypeCond2, whereClause1, whereClause2, pathCond1, pathCond2,
								aliveCond, rwOnTableCond);
						// aliveCond,
						// rwOnTableCond);
						// outer most wrapper
						BoolExpr rowExistsCond = ctx.mkExists(new Expr[] { rowVar }, body, 1, null, null, null, null);
						result.add(rowExistsCond);
					} else if (q1.getKind() == Kind.SELECT && q2.getKind() == Kind.INSERT) {
						BoolExpr body = ctx.mkAnd(otypeCond1, otypeCond2);
						result.add(body);
					} else if (q1.getKind() == Kind.SELECT && q2.getKind() == Kind.DELETE) {
						BoolExpr body = ctx.mkAnd(otypeCond1, otypeCond2);
						result.add(body);
					} else if (q1.getKind() == Kind.UPDATE && q2.getKind() == Kind.DELETE) {
						BoolExpr body = ctx.mkAnd(otypeCond1, otypeCond2);
						result.add(body);
					} else if (q1.getKind() == Kind.UPDATE && q2.getKind() == Kind.INSERT) {
						BoolExpr body = ctx.mkAnd(otypeCond1, otypeCond2);
						result.add(body);
					} else if (q1.getKind() == Kind.DELETE && q2.getKind() == Kind.INSERT) {
						BoolExpr body = ctx.mkAnd(otypeCond1, otypeCond2);
						result.add(body);
					}
				}
			}
		return result;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// WR THEN
	//
	////////////////////////
	public List<BoolExpr> return_conditions_wr_then(FuncDecl t1, FuncDecl t2, Expr vo1, Expr vo2, Expr vt1, Expr vt2) {
		Transaction txn1 = app.getTxnByName(t1.getName().toString());
		Transaction txn2 = app.getTxnByName(t2.getName().toString());
		List<BoolExpr> result = new ArrayList<BoolExpr>();
		// these have to be removed
		for (Statement o1 : txn1.getStmts())
			for (Statement o2 : txn2.getStmts()) {
				// generate constarints sharet beween cases:
				BoolExpr otype1 = ctx.mkEq(ctx.mkApp(objs.getfuncs("otype"), vo1),
						ctx.mkApp(objs.getConstructor("OType", ((InvokeStmt) o1).getType().toString())));
				BoolExpr otype2 = ctx.mkEq(ctx.mkApp(objs.getfuncs("otype"), vo2),
						ctx.mkApp(objs.getConstructor("OType", ((InvokeStmt) o2).getType().toString())));
				Query q1 = ((InvokeStmt) o1).getQuery();
				Query q2 = ((InvokeStmt) o2).getQuery();

				// add the conditions if there is a common table between statements
				if (q1.getTable().equals(q2.getTable())) {
					if (q1.getKind() == Kind.UPDATE && q2.getKind() == Kind.SELECT)
						result.add(ctx.mkAnd(otype1, otype2));
					else if (q1.getKind() == Kind.INSERT && q2.getKind() == Kind.SELECT)
						result.add(ctx.mkAnd(otype1, otype2));
					else if (q1.getKind() == Kind.DELETE && q2.getKind() == Kind.SELECT)
						result.add(ctx.mkAnd(otype1, otype2));
					else if (q1.getKind() == Kind.DELETE && q2.getKind() == Kind.UPDATE)
						result.add(ctx.mkAnd(otype1, otype2));
					else if (q1.getKind() == Kind.INSERT && q2.getKind() == Kind.UPDATE)
						result.add(ctx.mkAnd(otype1, otype2));
					else if (q1.getKind() == Kind.INSERT && q2.getKind() == Kind.DELETE)
						result.add(ctx.mkAnd(otype1, otype2));
				}
			}
		return result;
	}

}
