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

import ir.expression.PrimitiveVarExp;
import ir.expression.RowSetVarExp;
import ir.expression.RowVarExp;
import ir.expression.VarExp;
import ir.schema.Column;
import ir.schema.Table;

public class DynamicAssertsions {
	Context ctx;
	DeclaredObjects objs;
	Expr o1, o2, o3;

	public DynamicAssertsions(Context ctx, DeclaredObjects objs) {
		this.ctx = ctx;
		this.objs = objs;
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
		BoolExpr lhs = ctx.mkTrue();
		for (Column c : t.getColumns())
			if (c.isPK()) {
				Expr proj1 = ctx.mkApp(objs.getfuncs(t.getName() + "_PROJ_" + c.getName()), r1);
				Expr proj2 = ctx.mkApp(objs.getfuncs(t.getName() + "_PROJ_" + c.getName()), r2);
				lhs = ctx.mkAnd(lhs, ctx.mkEq(proj1, proj2));
			}
		BoolExpr rhs = ctx.mkEq(r1, r2);
		BoolExpr body = ctx.mkImplies(lhs, rhs);
		Quantifier x = ctx.mkForall(new Expr[] { r1, r2 }, body, 1, null, null, null, null);
		return x;
	}

	/*
	 * 
	 * 
	 * RULES
	 * 
	 * 
	 */

	public List<BoolExpr> return_conditions_rw_then(Expr vo1, Expr vo2, Expr vt1, Expr vt2) {
		List<BoolExpr> result = new ArrayList<BoolExpr>();
		BoolExpr cond1 = (BoolExpr) ctx.mkApp(objs.getfuncs("is_update"), vo2);
		BoolExpr cond2 = ctx.mkNot((BoolExpr) ctx.mkApp(objs.getfuncs("is_update"), vo1));
		result.add(ctx.mkAnd(cond1, cond2));
		return result;
	}

	public List<BoolExpr> return_conditions_wr_then(Expr vo1, Expr vo2, Expr vt1, Expr vt2) {
		List<BoolExpr> result = new ArrayList<BoolExpr>();
		BoolExpr cond1 = (BoolExpr) ctx.mkApp(objs.getfuncs("is_update"), vo1);
		BoolExpr cond2 = ctx.mkNot((BoolExpr) ctx.mkApp(objs.getfuncs("is_update"), vo2));
		result.add(ctx.mkAnd(cond1, cond2));
		return result;
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

}
