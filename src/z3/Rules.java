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
import ir.expression.vars.RowSetVarExp;
import ir.schema.Column;
import ir.schema.Table;
import ir.statement.InvokeStmt;
import ir.statement.Query;
import ir.statement.Statement;
import net.sf.jsqlparser.statement.insert.Insert;
import ir.statement.Query.Kind;

public class Rules {

	Context ctx;
	Application app;
	DeclaredObjects objs;
	Expr o1, o2, o3;
	Z3Util z3Util;
	ArrayList<Table> tables;

	public Rules(Context ctx, DeclaredObjects objs, Application app, ArrayList<Table> tables) {
		this.app = app;
		this.ctx = ctx;
		this.objs = objs;
		this.z3Util = new Z3Util(ctx, objs);
		this.tables = tables;
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
					String tableName = q1.getTable().getName();
					Sort rowSort = objs.getSort(tableName);
					Expr rowVar = ctx.mkFreshConst("r", rowSort);
					Table table = tables.stream().filter(t -> t.getName().equals(tableName)).findAny().get();
					BoolExpr pathCond1 = (BoolExpr) z3Util.irCondToZ3Expr(txn1.getName(), vt1, rowVar,
							o1.getPathCond());
					BoolExpr pathCond2 = (BoolExpr) z3Util.irCondToZ3Expr(txn2.getName(), vt2, rowVar,
							o2.getPathCond());

					FuncDecl funcConf = objs.getfuncs(
							((InvokeStmt) o1).getType() + "_" + ((InvokeStmt) o2).getType() + "_conflict_rows");
					BoolExpr rowConflictCond = ctx.mkEq(ctx.mkApp(funcConf, vo1, vo2), rowVar);
					//
					if (q1.getKind() == Kind.SELECT && q2.getKind() == Kind.UPDATE) {
						BoolExpr whereClause1 = (BoolExpr) z3Util.irCondToZ3Expr(txn1.getName(), vt1, rowVar,
								q1.getWhClause());
						BoolExpr whereClause2 = (BoolExpr) z3Util.irCondToZ3Expr(txn2.getName(), vt2, rowVar,
								q2.getWhClause());
						BoolExpr aliveCond = (BoolExpr) ctx.mkApp(objs.getfuncs("IsAlive_" + tableName), rowVar, vo2);
						BoolExpr rwOnTableCond = (BoolExpr) ctx.mkApp(objs.getfuncs("RW_O_" + tableName), rowVar, vo1,
								vo2);
						Expr body = ctx.mkAnd(rowConflictCond, otypeCond1, otypeCond2, whereClause1, whereClause2,
								pathCond1, pathCond2, aliveCond, rwOnTableCond);
						BoolExpr rowExistsCond = ctx.mkExists(new Expr[] { rowVar }, body, 1, null, null, null, null);
						result.add(rowExistsCond);
						//
					} else if (q1.getKind() == Kind.SELECT && q2.getKind() == Kind.INSERT) {
						String lhsVarName = ((RowSetVarExp) q1.getsVar()).getName();
						BoolExpr whereClause1 = (BoolExpr) z3Util.irCondToZ3Expr(txn1.getName(), vt1, rowVar,
								q1.getWhClause());
						BoolExpr aliveCond = ctx
								.mkNot((BoolExpr) ctx.mkApp(objs.getfuncs("IsAlive_" + tableName), rowVar, vo1));
						BoolExpr notNullCond = ctx.mkNot(
								(BoolExpr) ctx.mkApp(objs.getfuncs(txn1.getName() + "_" + lhsVarName), vt1, rowVar));
						BoolExpr rwAliveOnTableCond = (BoolExpr) ctx.mkApp(objs.getfuncs("RW_Alive_" + tableName),
								rowVar, vo1, vo2);
						BoolExpr[] insertedRowConds = new BoolExpr[table.getColumns().size()];
						int iter = 0;
						for (Column c : table.getColumns())
							insertedRowConds[iter] = ctx.mkEq(
									ctx.mkApp(objs.getfuncs(tableName + "_PROJ_" + c.getName()), rowVar),
									z3Util.irCondToZ3Expr(txn2.getName(), rowVar, vt2, q2.getI_values().get(iter++)));

						BoolExpr allInsertedRowCond = ctx.mkAnd(insertedRowConds);
						Expr body = ctx.mkAnd(rowConflictCond,otypeCond1, otypeCond2, whereClause1, pathCond1, pathCond2,
								allInsertedRowCond, aliveCond, notNullCond, rwAliveOnTableCond);

						BoolExpr rowExistsCond = ctx.mkExists(new Expr[] { rowVar }, body, 1, null, null, null, null);
						result.add(rowExistsCond);
						//
					} else if (q1.getKind() == Kind.SELECT && q2.getKind() == Kind.DELETE) {
						String lhsVarName = ((RowSetVarExp) q1.getsVar()).getName();
						BoolExpr whereClause2 = (BoolExpr) z3Util.irCondToZ3Expr(txn2.getName(), vt2, rowVar,
								q2.getWhClause());
						BoolExpr whereClause1 = (BoolExpr) z3Util.irCondToZ3Expr(txn1.getName(), vt1, rowVar,
								q1.getWhClause());
						BoolExpr aliveCond = (BoolExpr) ctx.mkApp(objs.getfuncs("IsAlive_" + tableName), rowVar, vo2);
						BoolExpr notNullCond = (BoolExpr) ctx
								.mkApp(objs.getfuncs(txn1.getName() + "_" + lhsVarName + "_isNull"), vo1);
						BoolExpr rwAliveOnTableCond = (BoolExpr) ctx.mkApp(objs.getfuncs("RW_Alive_" + tableName),
								rowVar, vo1, vo2);

						Expr body = ctx.mkAnd(rowConflictCond, otypeCond1, otypeCond2, whereClause1, whereClause2,
								pathCond1, pathCond2, aliveCond, notNullCond, rwAliveOnTableCond);
						BoolExpr rowExistsCond = ctx.mkExists(new Expr[] { rowVar }, body, 1, null, null, null, null);
						result.add(rowExistsCond);
						//
						// XXX I DONT THINK THERE IS A DEPENDENCY HERE
					} else if (q1.getKind() == Kind.UPDATE && q2.getKind() == Kind.DELETE) {
						/*
						 * BoolExpr whereClause2 = (BoolExpr) z3Util.irCondToZ3Expr(txn2.getName(), vt2,
						 * rowVar, q2.getWhClause()); BoolExpr whereClause1 = (BoolExpr)
						 * z3Util.irCondToZ3Expr(txn1.getName(), vt1, rowVar, q1.getWhClause());
						 * BoolExpr aliveCond2 = (BoolExpr) ctx.mkApp(objs.getfuncs("IsAlive_" +
						 * tableName), rowVar, vo2); BoolExpr aliveCond1 = ctx .mkNot((BoolExpr)
						 * ctx.mkApp(objs.getfuncs("IsAlive_" + tableName), rowVar, vo1)); BoolExpr
						 * rwAliveOnTableCond = (BoolExpr) ctx.mkApp(objs.getfuncs("RW_Alive_" +
						 * tableName), rowVar, vo1, vo2); Expr body = ctx.mkAnd(otypeCond1, otypeCond2,
						 * whereClause1, whereClause2, pathCond1, pathCond2, aliveCond1, aliveCond2,
						 * rwAliveOnTableCond); BoolExpr rowExistsCond = ctx.mkExists(new Expr[] {
						 * rowVar }, body, 1, null, null, null, null); result.add(rowExistsCond);
						 */
					} else if (q1.getKind() == Kind.UPDATE && q2.getKind() == Kind.INSERT) {
						BoolExpr whereClause1 = (BoolExpr) z3Util.irCondToZ3Expr(txn1.getName(), vt1, rowVar,
								q1.getWhClause());
						BoolExpr aliveCond1 = ctx
								.mkNot((BoolExpr) ctx.mkApp(objs.getfuncs("IsAlive_" + tableName), rowVar, vo1));
						BoolExpr rwAliveOnTableCond = (BoolExpr) ctx.mkApp(objs.getfuncs("RW_Alive_" + tableName),
								rowVar, vo1, vo2);
						BoolExpr[] insertedRowConds = new BoolExpr[table.getColumns().size()];
						int iter = 0;
						for (Column c : table.getColumns())
							insertedRowConds[iter] = ctx.mkEq(
									ctx.mkApp(objs.getfuncs(tableName + "_PROJ_" + c.getName()), rowVar),
									z3Util.irCondToZ3Expr(txn2.getName(), rowVar, vt2, q2.getI_values().get(iter++)));
						BoolExpr allInsertedRowCond = ctx.mkAnd(insertedRowConds);
						Expr body = ctx.mkAnd(rowConflictCond,otypeCond1, otypeCond2, whereClause1, pathCond1, pathCond2, aliveCond1,
								allInsertedRowCond, rwAliveOnTableCond);
						BoolExpr rowExistsCond = ctx.mkExists(new Expr[] { rowVar }, body, 1, null, null, null, null);
						result.add(rowExistsCond);
					} else if (q1.getKind() == Kind.DELETE && q2.getKind() == Kind.INSERT) {
						BoolExpr whereClause1 = (BoolExpr) z3Util.irCondToZ3Expr(txn1.getName(), vt1, rowVar,
								q1.getWhClause());
						BoolExpr aliveCond1 = ctx
								.mkNot((BoolExpr) ctx.mkApp(objs.getfuncs("IsAlive_" + tableName), rowVar, vo1));
						BoolExpr rwAliveOnTableCond = (BoolExpr) ctx.mkApp(objs.getfuncs("RW_Alive_" + tableName),
								rowVar, vo1, vo2);
						BoolExpr[] insertedRowConds = new BoolExpr[table.getColumns().size()];
						int iter = 0;
						for (Column c : table.getColumns())
							insertedRowConds[iter] = ctx.mkEq(
									ctx.mkApp(objs.getfuncs(tableName + "_PROJ_" + c.getName()), rowVar),
									z3Util.irCondToZ3Expr(txn2.getName(), rowVar, vt2, q2.getI_values().get(iter++)));
						BoolExpr allInsertedRowCond = ctx.mkAnd(insertedRowConds);
						Expr body = ctx.mkAnd(rowConflictCond,rowConflictCond, otypeCond1, otypeCond2, whereClause1, pathCond1,
								pathCond2, aliveCond1, allInsertedRowCond, rwAliveOnTableCond);
						BoolExpr rowExistsCond = ctx.mkExists(new Expr[] { rowVar }, body, 1, null, null, null, null);
						result.add(rowExistsCond);
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
	public List<BoolExpr> return_conditions_wr_then(FuncDecl t1, FuncDecl t2, Expr vo1, Expr vo2, Expr vt1, Expr vt2)
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
					String tableName = q1.getTable().getName();
					Table table = tables.stream().filter(t -> t.getName().equals(tableName)).findAny().get();
					Sort rowSort = objs.getSort(tableName);
					Expr rowVar = ctx.mkFreshConst("r", rowSort);
					BoolExpr pathCond1 = (BoolExpr) z3Util.irCondToZ3Expr(txn1.getName(), vt1, rowVar,
							o1.getPathCond());
					BoolExpr pathCond2 = (BoolExpr) z3Util.irCondToZ3Expr(txn2.getName(), vt2, rowVar,
							o2.getPathCond());
					FuncDecl funcConf = objs.getfuncs(
							((InvokeStmt) o1).getType() + "_" + ((InvokeStmt) o2).getType() + "_conflict_rows");
					BoolExpr rowConflictCond = ctx.mkEq(ctx.mkApp(funcConf, vo1, vo2), rowVar);
					//
					if (q1.getKind() == Kind.UPDATE && q2.getKind() == Kind.SELECT) {
						String lhsVarName = ((RowSetVarExp) q2.getsVar()).getName();
						BoolExpr whereClause1 = (BoolExpr) z3Util.irCondToZ3Expr(txn1.getName(), vt1, rowVar,
								q1.getWhClause());
						BoolExpr whereClause2 = (BoolExpr) z3Util.irCondToZ3Expr(txn2.getName(), vt2, rowVar,
								q2.getWhClause());
						BoolExpr wrOnTableCond = (BoolExpr) ctx.mkApp(objs.getfuncs("WR_O_" + tableName), rowVar, vo1,
								vo2);
						BoolExpr aliveCond = (BoolExpr) ctx.mkApp(objs.getfuncs("IsAlive_" + tableName), rowVar, vo1);
						BoolExpr notNullCond = (BoolExpr) ctx
								.mkApp(objs.getfuncs(txn2.getName() + "_" + lhsVarName + "_isNull"), vo2);
						Expr body = ctx.mkAnd(rowConflictCond,otypeCond1, otypeCond2, whereClause1, whereClause2, pathCond1, pathCond2,
								aliveCond, notNullCond, wrOnTableCond);
						BoolExpr rowExistsCond = ctx.mkExists(new Expr[] { rowVar }, body, 1, null, null, null, null);
						result.add(rowExistsCond);
						//
					} else if (q1.getKind() == Kind.INSERT && q2.getKind() == Kind.SELECT) {
						String lhsVarName = ((RowSetVarExp) q2.getsVar()).getName();
						BoolExpr whereClause2 = (BoolExpr) z3Util.irCondToZ3Expr(txn2.getName(), vt2, rowVar,
								q2.getWhClause());
						BoolExpr aliveCond = (BoolExpr) ctx.mkApp(objs.getfuncs("IsAlive_" + tableName), rowVar, vo2);
						BoolExpr notNullCond = (BoolExpr) ctx
								.mkApp(objs.getfuncs(txn2.getName() + "_" + lhsVarName + "_isNull"), vo2);
						BoolExpr wrAliveOnTableCond = (BoolExpr) ctx.mkApp(objs.getfuncs("WR_Alive_" + tableName),
								rowVar, vo1, vo2);
						BoolExpr[] insertedRowConds = new BoolExpr[table.getColumns().size()];
						int iter = 0;
						for (Column c : table.getColumns())
							insertedRowConds[iter] = ctx.mkEq(
									ctx.mkApp(objs.getfuncs(tableName + "_PROJ_" + c.getName()), rowVar),
									z3Util.irCondToZ3Expr(txn1.getName(), rowVar, vt1, q1.getI_values().get(iter++)));

						BoolExpr allInsertedRowCond = ctx.mkAnd(insertedRowConds);
						Expr body = ctx.mkAnd(rowConflictCond,otypeCond1, otypeCond2, whereClause2, pathCond1, pathCond2,
								allInsertedRowCond, aliveCond, notNullCond, wrAliveOnTableCond);
						BoolExpr rowExistsCond = ctx.mkExists(new Expr[] { rowVar }, body, 1, null, null, null, null);
						result.add(rowExistsCond);
						//
					} else if (q1.getKind() == Kind.DELETE && q2.getKind() == Kind.SELECT) {
						String lhsVarName = ((RowSetVarExp) q2.getsVar()).getName();
						BoolExpr whereClause1 = (BoolExpr) z3Util.irCondToZ3Expr(txn1.getName(), vt1, rowVar,
								q1.getWhClause());
						BoolExpr whereClause2 = (BoolExpr) z3Util.irCondToZ3Expr(txn2.getName(), vt2, rowVar,
								q2.getWhClause());
						BoolExpr aliveCond1 = (BoolExpr) ctx.mkApp(objs.getfuncs("IsAlive_" + tableName), rowVar, vo1);
						BoolExpr aliveCond2 = ctx
								.mkNot((BoolExpr) ctx.mkApp(objs.getfuncs("IsAlive_" + tableName), rowVar, vo2));

						BoolExpr notNullCond = ctx.mkNot(
								(BoolExpr) ctx.mkApp(objs.getfuncs(txn2.getName() + "_" + lhsVarName), vt2, rowVar));

						BoolExpr wrAliveOnTableCond = (BoolExpr) ctx.mkApp(objs.getfuncs("WR_Alive_" + tableName),
								rowVar, vo1, vo2);

						Expr body = ctx.mkAnd(rowConflictCond,otypeCond1, otypeCond2, whereClause1, whereClause2, pathCond1, pathCond2,
								aliveCond1, aliveCond2, notNullCond, wrAliveOnTableCond);

						BoolExpr rowExistsCond = ctx.mkExists(new Expr[] { rowVar }, body, 1, null, null, null, null);
						result.add(rowExistsCond);
					}
					//
					else if (q1.getKind() == Kind.DELETE && q2.getKind() == Kind.UPDATE) {
						BoolExpr whereClause1 = (BoolExpr) z3Util.irCondToZ3Expr(txn1.getName(), vt1, rowVar,
								q1.getWhClause());
						BoolExpr whereClause2 = (BoolExpr) z3Util.irCondToZ3Expr(txn2.getName(), vt2, rowVar,
								q2.getWhClause());
						BoolExpr aliveCond1 = (BoolExpr) ctx.mkApp(objs.getfuncs("IsAlive_" + tableName), rowVar, vo1);
						BoolExpr aliveCond2 = ctx
								.mkNot((BoolExpr) ctx.mkApp(objs.getfuncs("IsAlive_" + tableName), rowVar, vo2));

						BoolExpr wrAliveOnTableCond = (BoolExpr) ctx.mkApp(objs.getfuncs("WR_Alive_" + tableName),
								rowVar, vo1, vo2);
						Expr body = ctx.mkAnd(rowConflictCond,otypeCond1, otypeCond2, whereClause1, whereClause2, pathCond1, pathCond2,
								aliveCond1, aliveCond2, wrAliveOnTableCond);
						BoolExpr rowExistsCond = ctx.mkExists(new Expr[] { rowVar }, body, 1, null, null, null, null);
						result.add(rowExistsCond);
						//
					} else if (q1.getKind() == Kind.INSERT && q2.getKind() == Kind.UPDATE) {
						BoolExpr whereClause2 = (BoolExpr) z3Util.irCondToZ3Expr(txn2.getName(), vt2, rowVar,
								q2.getWhClause());
						BoolExpr aliveCond = (BoolExpr) ctx.mkApp(objs.getfuncs("IsAlive_" + tableName), rowVar, vo2);
						BoolExpr wrAliveOnTableCond = (BoolExpr) ctx.mkApp(objs.getfuncs("WR_Alive_" + tableName),
								rowVar, vo1, vo2);
						BoolExpr[] insertedRowConds = new BoolExpr[table.getColumns().size()];
						int iter = 0;
						for (Column c : table.getColumns())
							insertedRowConds[iter] = ctx.mkEq(
									ctx.mkApp(objs.getfuncs(tableName + "_PROJ_" + c.getName()), rowVar),
									z3Util.irCondToZ3Expr(txn1.getName(), rowVar, vt1, q1.getI_values().get(iter++)));

						BoolExpr allInsertedRowCond = ctx.mkAnd(insertedRowConds);
						Expr body = ctx.mkAnd(rowConflictCond,otypeCond1, otypeCond2, whereClause2, pathCond1, pathCond2,
								allInsertedRowCond, aliveCond, wrAliveOnTableCond);
						BoolExpr rowExistsCond = ctx.mkExists(new Expr[] { rowVar }, body, 1, null, null, null, null);
						result.add(rowExistsCond);
					} else if (q1.getKind() == Kind.INSERT && q2.getKind() == Kind.DELETE) {
						BoolExpr whereClause2 = (BoolExpr) z3Util.irCondToZ3Expr(txn2.getName(), vt2, rowVar,
								q2.getWhClause());
						BoolExpr aliveCond = (BoolExpr) ctx.mkApp(objs.getfuncs("IsAlive_" + tableName), rowVar, vo2);
						BoolExpr wrAliveOnTableCond = (BoolExpr) ctx.mkApp(objs.getfuncs("WR_Alive_" + tableName),
								rowVar, vo1, vo2);
						BoolExpr[] insertedRowConds = new BoolExpr[table.getColumns().size()];
						int iter = 0;
						for (Column c : table.getColumns())
							insertedRowConds[iter] = ctx.mkEq(
									ctx.mkApp(objs.getfuncs(tableName + "_PROJ_" + c.getName()), rowVar),
									z3Util.irCondToZ3Expr(txn1.getName(), rowVar, vt1, q1.getI_values().get(iter++)));

						BoolExpr allInsertedRowCond = ctx.mkAnd(insertedRowConds);
						Expr body = ctx.mkAnd(rowConflictCond, otypeCond1, otypeCond2, whereClause2, pathCond1,
								pathCond2, allInsertedRowCond, aliveCond, wrAliveOnTableCond);
						BoolExpr rowExistsCond = ctx.mkExists(new Expr[] { rowVar }, body, 1, null, null, null, null);
						result.add(rowExistsCond);
					}
				}
			}
		return result;
	}

	public List<BoolExpr> return_conditions_ww_then(FuncDecl t1, FuncDecl t2, Expr vo1, Expr vo2, Expr vt1, Expr vt2)
			throws UnexoectedOrUnhandledConditionalExpression {
		Transaction txn1 = app.getTxnByName(t1.getName().toString());
		Transaction txn2 = app.getTxnByName(t2.getName().toString());
		List<BoolExpr> result = new ArrayList<BoolExpr>();
		for (Statement o1 : txn1.getStmts())
			for (Statement o2 : txn2.getStmts()) {
				// generate constarints shared beween cases:
				BoolExpr otypeCond1 = ctx.mkEq(ctx.mkApp(objs.getfuncs("otype"), vo1),
						ctx.mkApp(objs.getConstructor("OType", ((InvokeStmt) o1).getType().toString())));
				BoolExpr otypeCond2 = ctx.mkEq(ctx.mkApp(objs.getfuncs("otype"), vo2),
						ctx.mkApp(objs.getConstructor("OType", ((InvokeStmt) o2).getType().toString())));
				Query q1 = ((InvokeStmt) o1).getQuery();
				Query q2 = ((InvokeStmt) o2).getQuery();

				// add the conditions if there is a common table between statements
				if (q1.getTable().equals(q2.getTable())) {
					String tableName = q1.getTable().getName();
					Sort rowSort = objs.getSort(tableName);
					Expr rowVar = ctx.mkFreshConst("r", rowSort);
					BoolExpr pathCond1 = (BoolExpr) z3Util.irCondToZ3Expr(txn1.getName(), vt1, rowVar,
							o1.getPathCond());
					BoolExpr pathCond2 = (BoolExpr) z3Util.irCondToZ3Expr(txn2.getName(), vt2, rowVar,
							o2.getPathCond());
					FuncDecl funcConf = objs.getfuncs(
							((InvokeStmt) o1).getType() + "_" + ((InvokeStmt) o2).getType() + "_conflict_rows");
					BoolExpr rowConflictCond = ctx.mkEq(ctx.mkApp(funcConf, vo1, vo2), rowVar);
					//
					if (q1.getKind() == Kind.UPDATE && q2.getKind() == Kind.UPDATE) {
						BoolExpr whereClause1 = (BoolExpr) z3Util.irCondToZ3Expr(txn1.getName(), vt1, rowVar,
								q1.getWhClause());
						BoolExpr whereClause2 = (BoolExpr) z3Util.irCondToZ3Expr(txn2.getName(), vt2, rowVar,
								q2.getWhClause());
						BoolExpr wwOnTableCond = (BoolExpr) ctx.mkApp(objs.getfuncs("WW_O_" + tableName), rowVar, vo1,
								vo2);
						BoolExpr aliveCond1 = (BoolExpr) ctx.mkApp(objs.getfuncs("IsAlive_" + tableName), rowVar, vo1);
						BoolExpr aliveCond2 = (BoolExpr) ctx.mkApp(objs.getfuncs("IsAlive_" + tableName), rowVar, vo2);
						Expr body = ctx.mkAnd(rowConflictCond, otypeCond1, otypeCond2, whereClause1, whereClause2,
								pathCond1, pathCond2, aliveCond1, aliveCond2, wwOnTableCond);
						BoolExpr rowExistsCond = ctx.mkExists(new Expr[] { rowVar }, body, 1, null, null, null, null);
						result.add(rowExistsCond);
					}
				}
			}
		return result;
	}
}
