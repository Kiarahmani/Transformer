package z3;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.Quantifier;

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
		Expr[] Ts = new Expr[limit+1];
		for (int i = 0; i < limit+1; i++)
			Ts[i] = ctx.mkFreshConst("t", objs.getSort("T"));
		Expr body = ctx.mkNot(ctx.mkDistinct(Ts));
		Quantifier x = ctx.mkForall(Ts, body, 1, null, null, null, null);
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

}
