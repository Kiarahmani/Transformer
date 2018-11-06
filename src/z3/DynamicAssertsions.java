package z3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.Quantifier;
import com.microsoft.z3.Sort;

import exceptions.UnexoectedOrUnhandledConditionalExpression;
import ir.Application;
import ir.Transaction;
import ir.expression.Expression;
import ir.expression.vars.PrimitiveVarExp;
import ir.expression.vars.RowSetVarExp;
import ir.expression.vars.RowVarExp;
import ir.expression.vars.VarExp;
import ir.schema.Column;
import ir.schema.Table;
import ir.statement.InvokeStmt;
import ir.statement.Query;
import ir.statement.Query.Kind;
import ir.statement.Statement;

public class DynamicAssertsions {
	Context ctx;
	Application app;
	DeclaredObjects objs;
	Expr o1, o2, o3;
	Z3Util z3Util;

	public DynamicAssertsions(Context ctx, DeclaredObjects objs, Application app) {
		this.app = app;
		this.ctx = ctx;
		this.objs = objs;
		this.z3Util = new Z3Util(ctx, objs);
		o1 = ctx.mkFreshConst("o", objs.getSort("O"));
		o2 = ctx.mkFreshConst("o", objs.getSort("O"));
		o3 = ctx.mkFreshConst("o", objs.getSort("O"));
	}

	public Quantifier mk_oType_to_is_update(List<String> updateTypes) {
		BoolExpr[] ors = new BoolExpr[updateTypes.size()];
		int iter = 0;
		BoolExpr lhs = (BoolExpr) ctx.mkApp(objs.getfuncs("is_update"), o1);
		for (String s : updateTypes) {
			Expr exp = ctx.mkApp(objs.getfuncs("otype"), o1);
			ors[iter] = ctx.mkEq(exp, ctx.mkApp(objs.getConstructor("OType", s)));
			iter++;
		}
		BoolExpr body = ctx.mkImplies(lhs, ctx.mkOr(ors));
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

	public Quantifier mk_is_update_to_oType(List<String> updateTypes) {
		BoolExpr[] ors = new BoolExpr[updateTypes.size()];
		int iter = 0;
		BoolExpr rhs = (BoolExpr) ctx.mkApp(objs.getfuncs("is_update"), o1);
		for (String s : updateTypes) {
			Expr exp = ctx.mkApp(objs.getfuncs("otype"), o1);
			ors[iter] = ctx.mkEq(exp, ctx.mkApp(objs.getConstructor("OType", s)));
			iter++;
		}
		BoolExpr body = ctx.mkImplies(ctx.mkOr(ors), rhs);
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

	public BoolExpr op_types_to_parent_type(String name, String stmtName) {
		BoolExpr rhs = ctx.mkEq(ctx.mkApp(objs.getfuncs("ttype"), ctx.mkApp(objs.getfuncs("parent"), o1)),
				ctx.mkApp(objs.getConstructor("TType", name)));
		BoolExpr lhs = (BoolExpr) ctx.mkEq(ctx.mkApp(objs.getfuncs("otype"), o1),
				ctx.mkApp(objs.getConstructor("OType", stmtName)));
		BoolExpr body = ctx.mkImplies(lhs, rhs);
		Quantifier x = ctx.mkForall(new Expr[] { o1 }, body, 1, null, null, null, null);
		return x;
	}

	public BoolExpr otime_follows_po(String stmt, String stmt2) {
		FuncDecl otime = objs.getfuncs("otime");
		FuncDecl ar = objs.getfuncs("ar");
		FuncDecl parent = objs.getfuncs("parent");
		FuncDecl otype = objs.getfuncs("otype");
		BoolExpr lhs1 = ctx.mkEq(ctx.mkApp(parent, o1), ctx.mkApp(parent, o2));
		BoolExpr lhs2 = ctx.mkEq(ctx.mkApp(otype, o1), ctx.mkApp(objs.getConstructor("OType", stmt)));
		BoolExpr lhs3 = ctx.mkEq(ctx.mkApp(otype, o2), ctx.mkApp(objs.getConstructor("OType", stmt2)));
		BoolExpr lhs = ctx.mkAnd(lhs1, lhs2, lhs3);
		BoolExpr rhs = ctx.mkGt((ArithExpr) ctx.mkApp(otime, o2), (ArithExpr) ctx.mkApp(otime, o1));
		BoolExpr body1 = ctx.mkImplies(lhs, rhs);
		BoolExpr body2 = ctx.mkImplies(ctx.mkAnd((BoolExpr) ctx.mkApp(ar, o1, o2), ctx.mkNot(ctx.mkEq(o1, o2))),
				ctx.mkGe((ArithExpr) ctx.mkApp(otime, o2), (ArithExpr) ctx.mkApp(otime, o1)));

		BoolExpr body = ctx.mkAnd(body1, body2);
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

	public BoolExpr mk_limit_txn_instances(int limit) {
		Expr[] Ts = new Expr[limit + 1];
		for (int i = 0; i < limit + 1; i++)
			Ts[i] = ctx.mkFreshConst("t", objs.getSort("T"));
		Expr body = ctx.mkNot(ctx.mkDistinct(Ts));
		Quantifier x = ctx.mkForall(Ts, body, 1, null, null, null, null);
		return x;
	}

	public BoolExpr mk_pk_tables(Table t) {
		Expr r1 = ctx.mkFreshConst("r", objs.getSort(t.getName()));
		Expr r2 = ctx.mkFreshConst("r", objs.getSort(t.getName()));
		FuncDecl verFunc = objs.getfuncs(t.getName() + "_VERSION");
		BoolExpr lhs = ctx.mkTrue();
		for (Column c : t.getColumns())
			if (c.isPK()) {
				Expr proj1 = ctx.mkApp(objs.getfuncs(t.getName() + "_PROJ_" + c.getName()), r1,
						ctx.mkApp(verFunc, r1, o1));
				Expr proj2 = ctx.mkApp(objs.getfuncs(t.getName() + "_PROJ_" + c.getName()), r2,
						ctx.mkApp(verFunc, r2, o2));
				lhs = ctx.mkAnd(lhs, ctx.mkEq(proj1, proj2));
			}
		BoolExpr rhs = ctx.mkEq(r1, r2);
		BoolExpr body = ctx.mkImplies(lhs, rhs);
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2, r1, r2 }, body, 1, null, null, null, null);
		return x;
	}

	public BoolExpr mk_svar_props(String txnName, String ValueName, String table, Expression whClause) {
		Expr rsort = ctx.mkFreshConst("r", objs.getSort(table));
		Expr tsort = ctx.mkFreshConst("t", objs.getSort("T"));
		BoolExpr rowBelongsToSet = (BoolExpr) ctx.mkApp(objs.getfuncs(txnName + "_" + ValueName), tsort, rsort);
		Quantifier x = null;
		try {
			x = ctx.mkForall(new Expr[] { o1, tsort, rsort },
					ctx.mkImplies(rowBelongsToSet,
							(BoolExpr) z3Util.irCondToZ3Expr(txnName, tsort, rsort, o1, whClause)),
					1, null, null, null, null);
		} catch (UnexoectedOrUnhandledConditionalExpression e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return x;
	}

	public BoolExpr mk_row_var_props(String txnName, String valueName, RowSetVarExp setVar) {
		Expr tsort = ctx.mkFreshConst("t", objs.getSort("T"));
		Quantifier x = null;
		String sVarName = txnName + "_" + setVar.getName();
		String rowVarName = txnName + "_" + valueName;
		x = ctx.mkForall(new Expr[] { tsort },
				ctx.mkApp(objs.getfuncs(sVarName), tsort, (ctx.mkApp(objs.getfuncs(rowVarName), tsort))), 1, null, null,
				null, null);
		return x;
	}

	public BoolExpr mk_row_var_loop_props(String txnName, String valueName, RowSetVarExp setVar) {
		Expr tsort = ctx.mkFreshConst("t", objs.getSort("T"));
		Expr isort = ctx.mkFreshConst("i", objs.getSort("Int"));
		Quantifier x = null;
		String sVarName = txnName + "_" + setVar.getName();
		String rowVarName = txnName + "_" + valueName;
		x = ctx.mkForall(new Expr[] { tsort, isort },
				ctx.mkApp(objs.getfuncs(sVarName), tsort, (ctx.mkApp(objs.getfuncs(rowVarName), tsort, isort))), 1,
				null, null, null, null);
		return x;
	}

	public BoolExpr mk_rw_then_deps(String tName) {
		Expr r1 = ctx.mkFreshConst("r", objs.getSort(tName));
		BoolExpr rhs = (BoolExpr) ctx.mkApp(objs.getfuncs("RW_O"), o1, o2);
		BoolExpr body1 = ctx.mkImplies((BoolExpr) ctx.mkApp(objs.getfuncs("RW_O_" + tName), r1, o1, o2), rhs);
		BoolExpr body2 = ctx.mkImplies((BoolExpr) ctx.mkApp(objs.getfuncs("RW_Alive_" + tName), r1, o1, o2), rhs);
		Expr body = ctx.mkAnd(body1, body2);
		Quantifier x = ctx.mkForall(new Expr[] { r1, o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

	public BoolExpr mk_wr_then_deps(String tName) {
		Expr r1 = ctx.mkFreshConst("r", objs.getSort(tName));
		BoolExpr rhs = (BoolExpr) ctx.mkApp(objs.getfuncs("WR_O"), o1, o2);
		BoolExpr body1 = ctx.mkImplies((BoolExpr) ctx.mkApp(objs.getfuncs("WR_O_" + tName), r1, o1, o2), rhs);
		// BoolExpr body2 = ctx.mkImplies((BoolExpr) ctx.mkApp(objs.getfuncs("WR_Alive_"
		// + tName), r1, o1, o2), rhs);
		Expr body = ctx.mkAnd(body1);
		Quantifier x = ctx.mkForall(new Expr[] { r1, o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

	public BoolExpr mk_ww_then_deps(String tName) {
		Expr r1 = ctx.mkFreshConst("r", objs.getSort(tName));
		BoolExpr rhs = (BoolExpr) ctx.mkApp(objs.getfuncs("WW_O"), o1, o2);
		BoolExpr body1 = ctx.mkImplies((BoolExpr) ctx.mkApp(objs.getfuncs("WW_O_" + tName), r1, o1, o2), rhs);
		BoolExpr body2 = ctx.mkImplies((BoolExpr) ctx.mkApp(objs.getfuncs("WW_Alive_" + tName), r1, o1, o2), rhs);
		Expr body = ctx.mkAnd(body1, body2);
		Quantifier x = ctx.mkForall(new Expr[] { r1, o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

	public BoolExpr mk_lww(String tName) {
		Expr r1 = ctx.mkFreshConst("r", objs.getSort(tName));
		BoolExpr rhs1 = (BoolExpr) ctx.mkApp(objs.getfuncs("WW_O_" + tName), r1, o2, o3);
		BoolExpr lhs1 = ctx.mkAnd(ctx.mkNot((BoolExpr) ctx.mkApp(objs.getfuncs("sibling"), o2, o3)),
				(BoolExpr) ctx.mkApp(objs.getfuncs("WR_O_" + tName), r1, o2, o1),
				(BoolExpr) ctx.mkApp(objs.getfuncs("RW_O_" + tName), r1, o1, o3));
		BoolExpr body1 = ctx.mkImplies(lhs1, rhs1);

		BoolExpr rhs2 = (BoolExpr) ctx.mkApp(objs.getfuncs("WW_Alive_" + tName), r1, o2, o3);
		BoolExpr lhs2 = ctx.mkAnd((BoolExpr) ctx.mkApp(objs.getfuncs("WR_Alive_" + tName), r1, o2, o1),
				(BoolExpr) ctx.mkApp(objs.getfuncs("RW_Alive_" + tName), r1, o1, o3));
		BoolExpr body2 = ctx.mkImplies(lhs2, rhs2);
		Expr body = ctx.mkAnd(body1, body2);
		Quantifier x = ctx.mkForall(new Expr[] { r1, o1, o2, o3 }, body, 1, null, null, null, null);
		return x;
	}

	public List<FuncDecl> mk_declare_lhs(String label, VarExp ve) {
		// PrimitiveVarExp
		try {
			PrimitiveVarExp pve = (PrimitiveVarExp) ve;
			return Arrays.asList(
					ctx.mkFuncDecl(label, new Sort[] { objs.getSort("T") }, objs.getSort(pve.getType().toZ3String())));
		} catch (ClassCastException e1) {
			// RowVarExp
			try {
				RowVarExp rve = (RowVarExp) ve;
				return Arrays.asList(ctx.mkFuncDecl(label, new Sort[] { objs.getSort("T") },
						objs.getSort(rve.getTable().getName())));
			} catch (ClassCastException e2) {
				// RowSetVarExp
				try {
					RowSetVarExp rsve = (RowSetVarExp) ve;
					FuncDecl varFunc = ctx.mkFuncDecl(label,
							new Sort[] { objs.getSort("T"), objs.getSort(rsve.getTable().getName()) },
							objs.getSort("Bool"));
					FuncDecl isNullFunc = ctx.mkFuncDecl(label + "_isNull", new Sort[] { objs.getSort("T") },
							objs.getSort("Bool"));

					return Arrays.asList(varFunc, isNullFunc);

				} catch (ClassCastException e3) {
					e3.printStackTrace();
				}

			}

		}
		return null;
	}

	public BoolExpr mk_assert_is_null(String label, VarExp ve) {
		// PrimitiveVarExp (no isNull prop)
		try {
			PrimitiveVarExp pve = (PrimitiveVarExp) ve;
			return null;
		} catch (ClassCastException e1) {
			// RowVarExp (no isNull prop)
			try {
				RowVarExp rve = (RowVarExp) ve;
				return null;
			} catch (ClassCastException e2) {
				// RowSetVarExp
				try {
					RowSetVarExp rsve = (RowSetVarExp) ve;
					Expr t1 = ctx.mkFreshConst("t", objs.getSort("T"));
					Expr r1 = ctx.mkFreshConst("r", objs.getSort(rsve.getTable().getName()));
					BoolExpr prop1 = ctx.mkNot((BoolExpr) ctx.mkApp(objs.getfuncs(label + "_isNull"), t1));
					BoolExpr prop2 = ctx.mkExists(new Expr[] { r1 }, ctx.mkApp(objs.getfuncs(label), t1, r1), 1, null,
							null, null, null);
					BoolExpr body1 = ctx.mkImplies(prop1, prop2);
					BoolExpr body2 = ctx.mkImplies(prop2, prop1);
					Expr body = ctx.mkAnd(body1, body2);
					Quantifier x = ctx.mkForall(new Expr[] { t1 }, body, 1, null, null, null, null);
					return x;

				} catch (ClassCastException e3) {
					e3.printStackTrace();
				}

			}

		}
		return null;
	}

	public List<BoolExpr> mk_versioning_props(ArrayList<Table> tables) {
		List<BoolExpr> result = new ArrayList<>();
		for (Table t : tables) {
			Expr r = ctx.mkFreshConst("r", objs.getSort(t.getName()));
			FuncDecl verFunc = objs.getfuncs(t.getName() + "_VERSION");
			BoolExpr lhs = (BoolExpr) ctx.mkApp(objs.getfuncs("RW_O_" + t.getName()), r, o1, o2);
			BoolExpr rhs = ctx.mkEq((ArithExpr) ctx.mkApp(verFunc, r, o2),
					ctx.mkAdd((ArithExpr) ctx.mkApp(verFunc, r, o1), ctx.mkInt(1)));
			Expr body = ctx.mkImplies(lhs, rhs);
			Quantifier x = ctx.mkForall(new Expr[] { r, o1, o2 }, body, 1, null, null, null, null);
			result.add(x);

			lhs = (BoolExpr) ctx.mkApp(objs.getfuncs("WW_O_" + t.getName()), r, o1, o2);
			rhs = ctx.mkEq((ArithExpr) ctx.mkApp(verFunc, r, o2),
					ctx.mkAdd((ArithExpr) ctx.mkApp(verFunc, r, o1), ctx.mkInt(1)));
			body = ctx.mkImplies(lhs, rhs);
			x = ctx.mkForall(new Expr[] { r, o1, o2 }, body, 1, null, null, null, null);
			result.add(x);

		}
		Expr r1 = ctx.mkFreshConst("r", objs.getSort("A"));
		Quantifier temp = ctx.mkForall(new Expr[] { r1, o1 },
				ctx.mkEq(ctx.mkApp(objs.getfuncs("A_VERSION"), r1, o1), ctx.mkInt(999)), 1, null, null, null, null);
		// result.add(temp);
		return result;
	}
}
