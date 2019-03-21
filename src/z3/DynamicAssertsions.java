package z3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.Quantifier;
import com.microsoft.z3.Sort;

import anomaly.Anomaly;
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
import utils.Tuple;

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

	public BoolExpr mk_pk_tables_identical(Table t) {
		Expr r = ctx.mkFreshConst("r", objs.getSort(t.getName()));
		Expr v1 = ctx.mkFreshConst("v", objs.getSort("BitVec"));
		Expr v2 = ctx.mkFreshConst("v", objs.getSort("BitVec"));
		FuncDecl verFunc = objs.getfuncs(t.getName() + "_VERSION");
		BoolExpr rhs = ctx.mkTrue();
		for (Column c : t.getColumns())
			if (c.isPK()) {
				Expr proj1 = ctx.mkApp(objs.getfuncs(t.getName() + "_PROJ_" + c.getName()), r, v1);
				Expr proj2 = ctx.mkApp(objs.getfuncs(t.getName() + "_PROJ_" + c.getName()), r, v2);
				rhs = ctx.mkAnd(rhs, ctx.mkEq(proj1, proj2));
			}
		// BoolExpr body = ctx.mkImplies(lhs, rhs);
		Quantifier x = ctx.mkForall(new Expr[] { r, v1, v2 }, rhs, 1, null, null, null, null);
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
		Expr isort = ctx.mkFreshConst("i", objs.getSort("LoopBitVec"));
		Quantifier x = null;
		String sVarName = txnName + "_" + setVar.getName();
		String rowVarName = txnName + "_" + valueName;
		x = ctx.mkForall(new Expr[] { tsort, isort },
				ctx.mkApp(objs.getfuncs(sVarName), tsort, (ctx.mkApp(objs.getfuncs(rowVarName), tsort, isort))), 1,
				null, null, null, null);
		return x;
	}

	public BoolExpr _mk_no_incom_edge_then_NOT_init_ver(String tName) {
		Expr o = ctx.mkFreshConst("o", objs.getSort("O"));
		Expr o1 = ctx.mkFreshConst("o", objs.getSort("O"));
		Expr r = ctx.mkFreshConst("r", objs.getSort(tName));

		FuncDecl is_update_func = objs.getfuncs("is_update");
		FuncDecl rw_func = objs.getfuncs("RW_O_" + tName);
		FuncDecl wr_func = objs.getfuncs("WR_O_" + tName);
		FuncDecl ww_func = objs.getfuncs("WW_O_" + tName);

		BoolExpr lhs1 = (BoolExpr) ctx.mkApp(is_update_func, o);
		BoolExpr lhs21 = (BoolExpr) ctx.mkApp(rw_func, r, o1, o);
		BoolExpr lhs22 = (BoolExpr) ctx.mkApp(wr_func, r, o1, o);
		BoolExpr lhs23 = (BoolExpr) ctx.mkApp(ww_func, r, o1, o);
		BoolExpr lhs2 = ctx.mkForall(new Expr[] { o1 }, ctx.mkNot(ctx.mkOr(lhs21, lhs22, lhs23)), 1, null, null, null,
				null);

		BoolExpr lhs = ctx.mkAnd(lhs1, lhs2);
		FuncDecl init_ver_func = objs.getfuncs(tName + "_INITIAL_V");
		FuncDecl ver_func = objs.getfuncs(tName + "_VERSION");
		BoolExpr rhs = (BoolExpr) ctx.mkNot(ctx.mkEq(ctx.mkApp(init_ver_func, r), ctx.mkApp(ver_func, r, o)));
		Expr body = ctx.mkImplies(lhs, rhs);
		Quantifier x = ctx.mkForall(new Expr[] { r, o }, body, 1, null, null, null, null);
		return x;
	}

	public BoolExpr _mk_no_incom_edge_then_init_ver(String tName) {
		Expr o = ctx.mkFreshConst("o", objs.getSort("O"));
		Expr o1 = ctx.mkFreshConst("o", objs.getSort("O"));
		Expr r = ctx.mkFreshConst("r", objs.getSort(tName));

		FuncDecl is_update_func = objs.getfuncs("is_update");
		FuncDecl rw_func = objs.getfuncs("RW_O_" + tName);
		FuncDecl wr_func = objs.getfuncs("WR_O_" + tName);
		FuncDecl ww_func = objs.getfuncs("WW_O_" + tName);

		BoolExpr lhs1 = ctx.mkNot((BoolExpr) ctx.mkApp(is_update_func, o));
		BoolExpr lhs21 = (BoolExpr) ctx.mkApp(rw_func, r, o1, o);
		BoolExpr lhs22 = (BoolExpr) ctx.mkApp(wr_func, r, o1, o);
		BoolExpr lhs23 = (BoolExpr) ctx.mkApp(ww_func, r, o1, o);
		BoolExpr lhs2 = ctx.mkForall(new Expr[] { o1 }, ctx.mkNot(ctx.mkOr(lhs21, lhs22, lhs23)), 1, null, null, null,
				null);

		BoolExpr lhs = ctx.mkAnd(lhs1, lhs2);
		FuncDecl init_ver_func = objs.getfuncs(tName + "_INITIAL_V");
		FuncDecl ver_func = objs.getfuncs(tName + "_VERSION");
		BoolExpr rhs = (BoolExpr) ctx.mkEq(ctx.mkApp(init_ver_func, r), ctx.mkApp(ver_func, r, o));
		Expr body = ctx.mkImplies(lhs, rhs);
		Quantifier x = ctx.mkForall(new Expr[] { r, o }, body, 1, null, null, null, null);
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

	/*
	 * 
	 * 
	 * 
	 */

	public List<BoolExpr> mk_versioning_props(ArrayList<Table> tables) {
		List<BoolExpr> result = new ArrayList<>();
		for (Table t : tables) {
			// RW then version increases
			Expr r = ctx.mkFreshConst("r", objs.getSort(t.getName()));
			FuncDecl verFunc = objs.getfuncs(t.getName() + "_VERSION");
			FuncDecl wwFunc = objs.getfuncs("WW_O_" + t.getName());
			FuncDecl wrFunc = objs.getfuncs("WR_O_" + t.getName());
			FuncDecl rwFunc = objs.getfuncs("RW_O_" + t.getName());
			FuncDecl siblingFunc = objs.getfuncs("sibling");
			FuncDecl otimeFunc = objs.getfuncs("otime");
			FuncDecl isUpdateFunc = objs.getfuncs("is_update");

			BoolExpr lhs = (BoolExpr) ctx.mkApp(rwFunc, r, o1, o2);
			BoolExpr rhs = ctx.mkEq(ctx.mkApp(verFunc, r, o2),
					ctx.mkBVAdd((BitVecExpr) ctx.mkApp(verFunc, r, o1), ctx.mkBV(1, ConstantArgs._MAX_VERSIONS_)));
			Expr body = ctx.mkImplies(lhs, rhs);
			Quantifier x = ctx.mkForall(new Expr[] { r, o1, o2 }, body, 1, null, null, null, null);
			result.add(x);

			// WR then version increases
			lhs = (BoolExpr) ctx.mkApp(wrFunc, r, o1, o2);
			rhs = ctx.mkEq(ctx.mkApp(verFunc, r, o2), ctx.mkApp(verFunc, r, o1));
			body = ctx.mkImplies(lhs, rhs);
			x = ctx.mkForall(new Expr[] { r, o1, o2 }, body, 1, null, null, null, null);
			result.add(x);

			// WW then version increases
			lhs = (BoolExpr) ctx.mkApp(wwFunc, r, o1, o2);
			rhs = ctx.mkEq(ctx.mkApp(verFunc, r, o2),
					ctx.mkBVAdd((BitVecExpr) ctx.mkApp(verFunc, r, o1), ctx.mkBV(1, ConstantArgs._MAX_VERSIONS_)));
			body = ctx.mkImplies(lhs, rhs);
			x = ctx.mkForall(new Expr[] { r, o1, o2 }, body, 1, null, null, null, null);
			result.add(x);

			// sibling and PO then version increases
			BoolExpr lhs1 = (BoolExpr) ctx.mkApp(siblingFunc, o1, o2);
			BoolExpr lhs2 = ctx.mkGt((ArithExpr) ctx.mkApp(otimeFunc, o2), (ArithExpr) ctx.mkApp(otimeFunc, o1));
			BoolExpr lhs3 = (BoolExpr) ctx.mkApp(isUpdateFunc, o2);
			lhs = ctx.mkAnd(lhs1, lhs2, lhs3);
			rhs = ctx.mkEq(ctx.mkApp(verFunc, r, o2),
					ctx.mkBVAdd((BitVecExpr) ctx.mkApp(verFunc, r, o1), ctx.mkBV(1, ConstantArgs._MAX_VERSIONS_)));
			body = ctx.mkImplies(lhs, rhs);
			x = ctx.mkForall(new Expr[] { r, o1, o2 }, body, 1, null, null, null, null);
			// result.add(x);
		}
		return result;
	}

	/*
	 * 
	 * 
	 * 
	 */
	/*
	 * 
	 * 
	 * 
	 */
	// EXACT CYCLE EXCLUSION (2)
	public BoolExpr mk_previous_anomaly_exclusion(List<Tuple<String, Tuple<String, String>>> structure) {
		int length = structure.size();
		// bound variables
		Expr[] Os = new Expr[length];
		for (int i = 0; i < length; i++)
			Os[i] = ctx.mkFreshConst("o", objs.getSort("O"));
		// LHS
		FuncDecl otypeFunc = objs.getfuncs("otype");
		BoolExpr[] allLhs = new BoolExpr[length];
		for (int i = 0; i < length; i++) {
			String xs = structure.get(i).y.x;
			FuncDecl cnstrX = objs.getConstructor("OType", xs.substring(1, xs.length() - 1));
			BoolExpr lhsLoop = ctx.mkEq(ctx.mkApp(otypeFunc, Os[i]), ctx.mkApp(cnstrX));
			allLhs[i] = lhsLoop;
		}
		BoolExpr lhs = ctx.mkAnd(allLhs);
		// RHS
		BoolExpr[] allRhs = new BoolExpr[length];
		for (int i = 0; i < length - 1; i++) {
			String op = structure.get(i).x.equals("sibling") ? "sibling" : structure.get(i).x + "_O";
			allRhs[i] = op.equals("sibling") ? (BoolExpr) ctx.mkApp(objs.getfuncs("X"), Os[i], Os[i + 1])
					: (BoolExpr) ctx.mkApp(objs.getfuncs("D"), Os[i], Os[i + 1]);
		}
		String op = structure.get(length - 1).x.equals("sibling") ? "sibling" : structure.get(length - 1).x + "_O";
		allRhs[length - 1] = op.equals("sibling") ? (BoolExpr) ctx.mkApp(objs.getfuncs("X"), Os[length - 1], Os[0])
				: (BoolExpr) ctx.mkApp(objs.getfuncs("D"), Os[length - 1], Os[0]);
		BoolExpr rhs = ctx.mkAnd(allRhs);
		Expr body = ctx.mkImplies(lhs, ctx.mkNot(rhs));
		Quantifier x = ctx.mkForall(Os, body, 1, null, null, null, null);
		return x;
	}

	private List<List<Tuple<String, String>>> genCompleteStructure(
			List<Tuple<String, Tuple<String, String>>> structure) {
		for (Tuple<String, Tuple<String, String>> x : structure)
			System.out.println(x);
		return null;

	}

	// the final assertion, generating a cycle on the dependency graph
	public BoolExpr mk_cycle(boolean findCore, Anomaly unVersionedAnml) {

		List<Tuple<String, Tuple<String, String>>> structure = null;
		Map<Tuple<String, String>, Set<String>> completeStructure = null;
		List<Tuple<String, String>> cycleTxns = null;
		boolean isStepTwo = false;
		int length = ConstantArgs._Current_Cycle_Length;
		Expr[] Os = new Expr[length];

		if (unVersionedAnml != null) {
			structure = unVersionedAnml.getCycleStructure();
			completeStructure = unVersionedAnml.getCompleteStructure();
			cycleTxns = unVersionedAnml.getCycleTxns();
			isStepTwo = (structure != null && structure.size() > 0 && structure.size() == Os.length);
		}

		// how many new operations are here to be instantiated?
		int additionalOperationCount = 0;
		if (completeStructure != null)
			for (Set<String> newSet : completeStructure.values())
				if (newSet != null)
					additionalOperationCount += newSet.size();

		int totalLength = length + additionalOperationCount;

		// variables for the operations that are part of the cycle

		for (int i = 0; i < length; i++)
			Os[i] = ctx.mkFreshConst("o", objs.getSort("O"));
		// variables for the additional ops that are not part of the cycle
		Expr[] additionalOs = new Expr[additionalOperationCount];
		for (int i = 0; i < additionalOperationCount; i++)
			additionalOs[i] = ctx.mkFreshConst("o", objs.getSort("O"));
		// copy the above two arrays into one which will contain all variables
		// (new/original)
		Expr[] allOs = new Expr[totalLength];
		System.arraycopy(Os, 0, allOs, 0, length);
		System.arraycopy(additionalOs, 0, allOs, length, additionalOperationCount);

		// constraints regarding previously found (unversioned) anomaly (limit the
		// solutions to equal ones (structurally))
		BoolExpr depExprs[] = new BoolExpr[length];
		// constraints on vars not being equal
		BoolExpr notEqExprs[] = new BoolExpr[length * (length - 1) / 2];
		int iter = 0;
		for (int i = 0; i < length - 1; i++)
			for (int j = i + 1; j < length; j++)
				notEqExprs[iter++] = ctx.mkNot(ctx.mkEq(allOs[i], allOs[j]));
		Quantifier x = null;
		if (isStepTwo) {
			BoolExpr prevAnmlExprs[] = null;
			prevAnmlExprs = (ConstantArgs._INSTANTIATE_NON_CYCLE_OPS)
					? new BoolExpr[structure.size() + 2 * additionalOperationCount]
					: new BoolExpr[structure.size()];
			prepareCompleteCycle(depExprs, prevAnmlExprs, structure, completeStructure, cycleTxns,
					additionalOperationCount, length, Os, allOs);
			BoolExpr body = ctx.mkAnd(ctx.mkAnd(notEqExprs), ctx.mkAnd(prevAnmlExprs), ctx.mkAnd(depExprs));
			x = ctx.mkExists(allOs, body, 1, null, null, null, null);
		} else {
			prepareBasicCycle(depExprs, Os, length);
			BoolExpr body = ctx.mkAnd(ctx.mkAnd(notEqExprs), ctx.mkAnd(depExprs));
			x = ctx.mkExists(Os, body, 1, null, null, null, null);
		}
		return x;

	}

	private void prepareCompleteCycle(BoolExpr[] depExprs, BoolExpr[] prevAnmlExprs,
			List<Tuple<String, Tuple<String, String>>> structure,
			Map<Tuple<String, String>, Set<String>> completeStructure, List<Tuple<String, String>> cycleTxns,
			int additionalOperationCount, int length, Expr[] Os, Expr[] allOs) {
		// prevAnmlExprs (below) will include original Os types, newly instantiated Os
		// types and the fact that the new ones are sibling with the old ones

		int iter = 0;
		FuncDecl otypeFunc = objs.getfuncs("otype");
		for (int i = 0; i < structure.size(); i++) {
			String xs = structure.get(i).y.x;
			FuncDecl cnstrX = objs.getConstructor("OType", xs.substring(1, xs.length() - 1));
			BoolExpr lhsX = ctx.mkEq(ctx.mkApp(otypeFunc, Os[i]), ctx.mkApp(cnstrX));
			prevAnmlExprs[iter++] = lhsX;
		}

		// These are extra constrinats only for new Ops
		System.out.println(objs.getAllNextVars());
		if (ConstantArgs._INSTANTIATE_NON_CYCLE_OPS) {
			int newOsIter = iter;
			// add extra constraints on newly instantiated (non-cycle) os --
			// these nested loops will add 2*additionalOperationCount new constraints
			for (int i = 0; i < structure.size(); i++) {
				String xs = structure.get(i).y.x;
				Expr parentOld = objs.getfuncs("parent").apply(Os[i]);

				Set<String> newOTypes = completeStructure.get(new Tuple<>(cycleTxns.get(i).x, xs));
				if (newOTypes != null) {
					for (String newOType : newOTypes) {
						FuncDecl cnstrNew = objs.getConstructor("OType", newOType);
						BoolExpr consNewType = ctx.mkEq(ctx.mkApp(otypeFunc, allOs[iter]), ctx.mkApp(cnstrNew));
						Expr parentNew = objs.getfuncs("parent").apply(allOs[iter]);
						prevAnmlExprs[newOsIter++] = consNewType;
						prevAnmlExprs[newOsIter++] = ctx.mkEq(parentNew, parentOld);
						iter++;
					}
				}
			}
		}

		// circular constraints
		for (int i = 0; i < length - 1; i++) {
			String op = structure.get(i).x.equals("sibling") ? "sibling" : structure.get(i).x + "_O";
			depExprs[i] = ctx.mkAnd((BoolExpr) ctx.mkApp(objs.getfuncs(op), Os[i], Os[i + 1]),
					(BoolExpr) ctx.mkApp(objs.getfuncs("X"), Os[i], Os[i + 1]),
					(op.equals("sibling") ? ctx.mkTrue() : (BoolExpr) ctx.mkApp(objs.getfuncs("D"), Os[i], Os[i + 1])));
		}
		// last iter
		String op = structure.get(length - 1).x.equals("sibling") ? "sibling" : structure.get(length - 1).x + "_O";
		depExprs[length - 1] = ctx.mkAnd((BoolExpr) ctx.mkApp(objs.getfuncs(op), Os[length - 1], Os[0]),
				(BoolExpr) ctx.mkApp(objs.getfuncs("X"), Os[length - 1], Os[0]), (op.equals("sibling") ? ctx.mkTrue()
						: (BoolExpr) ctx.mkApp(objs.getfuncs("D"), Os[length - 1], Os[0])));
	}

	private void prepareBasicCycle(BoolExpr depExprs[], Expr[] Os, int length) {
		String dep = "X";
		// a base sibling edge must exist
		depExprs[0] = ctx.mkAnd((BoolExpr) ctx.mkApp(objs.getfuncs("X"), Os[0], Os[1]),
				(BoolExpr) ctx.mkApp(objs.getfuncs("sibling"), Os[0], Os[1]));
		depExprs[1] = (BoolExpr) ctx.mkApp(objs.getfuncs("D"), Os[1], Os[2]);
		depExprs[length - 1] = (BoolExpr) ctx.mkApp(objs.getfuncs("D"), Os[length - 1], Os[0]);
		for (int i = 2; i < length - 1; i++) {
			depExprs[i] = (BoolExpr) ctx.mkApp(objs.getfuncs(dep), Os[i], Os[i + 1]);
			if (dep.equals("X"))
				dep = "D";
			else
				dep = "X";
		}
	}

	// LOOSE CYCLE ENFORCEMENT (4)
	public BoolExpr mk_loose_cycle(boolean findCore, List<Tuple<String, Tuple<String, String>>> structure) {

		int length = ConstantArgs._Current_Cycle_Length;
		Expr[] Os = new Expr[length];
		for (int i = 0; i < length; i++)
			Os[i] = ctx.mkFreshConst("o", objs.getSort("O"));

		BoolExpr notEqExprs[] = new BoolExpr[length * (length - 1) / 2];
		int iter = 0;
		for (int i = 0; i < length - 1; i++)
			for (int j = i + 1; j < length; j++)
				notEqExprs[iter++] = ctx.mkNot(ctx.mkEq(Os[i], Os[j]));

		// constraints regarding previously found anomaly (limit the
		// solutions to structurally close ones )
		BoolExpr prevAnmlExprs[] = null;
		BoolExpr depExprs[] = new BoolExpr[length];
		if (structure != null && structure.size() > 0 && structure.size() == Os.length) {
			prevAnmlExprs = new BoolExpr[structure.size()];
			iter = 0;
			FuncDecl ttypeFunc = objs.getfuncs("ttype");
			FuncDecl parentFunc = objs.getfuncs("parent");
			for (int i = 0; i < structure.size(); i++) {
				String xs = structure.get(i).y.x;
				String ts = xs.split("-")[0];
				// trim the unneccessary | character
				ts = ts.substring(1, ts.length());
				FuncDecl cnstrX = objs.getConstructor("TType", ts);
				BoolExpr lhsX = ctx.mkEq(ctx.mkApp(ttypeFunc, ctx.mkApp(parentFunc, Os[i])), ctx.mkApp(cnstrX));
				prevAnmlExprs[iter++] = lhsX;
			}
			// circular constraints
			for (int i = 0; i < length - 1; i++) {
				String op = structure.get(i).x.equals("sibling") ? "sibling" : structure.get(i).x + "_O";
				depExprs[i] = op.equals("sibling")
						? ctx.mkAnd((BoolExpr) ctx.mkApp(objs.getfuncs("X"), Os[i], Os[i + 1]),
								(BoolExpr) ctx.mkApp(objs.getfuncs("sibling"), Os[i], Os[i + 1]))
						: ctx.mkAnd(/* (BoolExpr) ctx.mkApp(objs.getfuncs(op), Os[i], Os[i + 1]), */
								(BoolExpr) ctx.mkApp(objs.getfuncs("D"), Os[i], Os[i + 1]));
			}
			// last iteration
			String op = structure.get(length - 1).x.equals("sibling") ? "sibling" : structure.get(length - 1).x + "_O";
			depExprs[length - 1] = op.equals("sibling")
					? ctx.mkAnd((BoolExpr) ctx.mkApp(objs.getfuncs("X"), Os[length - 1], Os[0]),
							(BoolExpr) ctx.mkApp(objs.getfuncs("sibling"), Os[length - 1], Os[0]))
					: ctx.mkAnd(/* (BoolExpr) ctx.mkApp(objs.getfuncs(op), Os[length - 1], Os[0]), */
							(BoolExpr) ctx.mkApp(objs.getfuncs("D"), Os[length - 1], Os[0]));

		} else {
			System.err.println("--- something went wrong...");
			System.err.println("structure: " + structure);
			System.out.println("Os: " + Os);

			int next = 1;
			String dep = "D";
			for (int i = 0; i < length; i++) {
				if (i == length - 1)
					next = 0;
				else
					next = i + 1;
				depExprs[i] = (BoolExpr) ctx.mkApp(objs.getfuncs(dep), Os[i], Os[next]);
				if (dep.equals("D"))
					dep = "X";
				else
					dep = "D";

			}
		}
		BoolExpr body = (structure != null && structure.size() > 0 && structure.size() == Os.length)
				? ctx.mkAnd(ctx.mkAnd(notEqExprs), ctx.mkAnd(prevAnmlExprs), ctx.mkAnd(depExprs))
				: ctx.mkAnd(ctx.mkAnd(notEqExprs), ctx.mkAnd(depExprs));
		Quantifier x = ctx.mkExists(Os, body, 1, null, null, null, null);
		return x;

	}

	// LOOSE CYCLE ENFORCEMENT (4)
	public BoolExpr mk_more_loose_cycle(boolean findCore, List<Tuple<String, Tuple<String, String>>> structure) {

		int length = ConstantArgs._Current_Cycle_Length;
		Expr[] Os = new Expr[length];
		for (int i = 0; i < length; i++)
			Os[i] = ctx.mkFreshConst("o", objs.getSort("O"));

		BoolExpr notEqExprs[] = new BoolExpr[length * (length - 1) / 2];
		int iter = 0;
		for (int i = 0; i < length - 1; i++)
			for (int j = i + 1; j < length; j++)
				notEqExprs[iter++] = ctx.mkNot(ctx.mkEq(Os[i], Os[j]));

		// constraints regarding previously found anomaly (limit the
		// solutions to structurally close ones )
		BoolExpr prevAnmlExprs[] = null;
		BoolExpr depExprs[] = new BoolExpr[length];
		if (structure != null && structure.size() > 0 && structure.size() == Os.length) {
			prevAnmlExprs = new BoolExpr[structure.size()];
			iter = 0;
			FuncDecl ttypeFunc = objs.getfuncs("ttype");
			FuncDecl parentFunc = objs.getfuncs("parent");
			for (int i = 0; i < structure.size(); i++) {
				String xs = structure.get(i).y.x;
				String ts = xs.split("-")[0];
				// trim the unneccessary | character
				ts = ts.substring(1, ts.length());
				FuncDecl cnstrX = objs.getConstructor("TType", ts);
				BoolExpr lhsX = ctx.mkEq(ctx.mkApp(ttypeFunc, ctx.mkApp(parentFunc, Os[i])), ctx.mkApp(cnstrX));
				prevAnmlExprs[iter++] = lhsX;
			}
			// circular constraints
			boolean firstFlag = true;
			for (int i = 0; i < length - 1; i++) {
				String op = structure.get(i).x.equals("sibling") ? "sibling" : structure.get(i).x + "_O";
				if (op.equals("sibling") && firstFlag) {
					depExprs[i] = (BoolExpr) ctx.mkApp(objs.getfuncs(op), Os[i], Os[i + 1]);
					firstFlag = false;
				} else
					depExprs[i] = (BoolExpr) ctx.mkApp(objs.getfuncs("D"), Os[i], Os[i + 1]);
			}
			// last iteration
			String op = structure.get(length - 1).x.equals("sibling") ? "sibling" : structure.get(length - 1).x + "_O";
			depExprs[length - 1] = op.equals("sibling")
					? ctx.mkAnd((BoolExpr) ctx.mkApp(objs.getfuncs("X"), Os[length - 1], Os[0]),
							(BoolExpr) ctx.mkApp(objs.getfuncs("sibling"), Os[length - 1], Os[0]))
					: ctx.mkAnd(/* (BoolExpr) ctx.mkApp(objs.getfuncs(op), Os[length - 1], Os[0]), */
							(BoolExpr) ctx.mkApp(objs.getfuncs("D"), Os[length - 1], Os[0]));

		} else {
			System.err.println("--- something went wrong...");
			System.err.println("structure: " + structure);
			System.out.println("Os: " + Os);

			int next = 1;
			String dep = "D";
			for (int i = 0; i < length; i++) {
				if (i == length - 1)
					next = 0;
				else
					next = i + 1;
				depExprs[i] = (BoolExpr) ctx.mkApp(objs.getfuncs(dep), Os[i], Os[next]);
				if (dep.equals("D"))
					dep = "X";
				else
					dep = "D";

			}
		}
		BoolExpr body = (structure != null && structure.size() > 0 && structure.size() == Os.length)
				? ctx.mkAnd(ctx.mkAnd(notEqExprs), ctx.mkAnd(prevAnmlExprs), ctx.mkAnd(depExprs))
				: ctx.mkAnd(ctx.mkAnd(notEqExprs), ctx.mkAnd(depExprs));
		Quantifier x = ctx.mkExists(Os, body, 1, null, null, null, null);
		return x;

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
 */
