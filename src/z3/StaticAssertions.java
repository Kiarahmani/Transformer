package z3;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Quantifier;
import com.microsoft.z3.Sort;
import com.microsoft.z3.Symbol;
import com.sun.org.apache.bcel.internal.Constants;
import z3.ConstantArgs;

public class StaticAssertions {
	Context ctx;
	DeclaredObjects objs;
	Expr o1, o2, o3;

	public StaticAssertions(Context ctx, DeclaredObjects objs) {
		this.ctx = ctx;
		this.objs = objs;
		o1 = ctx.mkFreshConst("o", objs.getSort("O"));
		o2 = ctx.mkFreshConst("o", objs.getSort("O"));
		o3 = ctx.mkFreshConst("o", objs.getSort("O"));
	}

	public Quantifier mk_par_then_sib() {
		BoolExpr lhs = ctx.mkEq(ctx.mkApp(objs.getfuncs("parent"), o1), ctx.mkApp(objs.getfuncs("parent"), o2));
		BoolExpr rhs = (BoolExpr) ctx.mkApp(objs.getfuncs("sibling"), o1, o2);
		BoolExpr body = ctx.mkImplies(lhs, rhs);
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

	public Quantifier mk_sib_then_par() {
		BoolExpr rhs = ctx.mkEq(ctx.mkApp(objs.getfuncs("parent"), o1), ctx.mkApp(objs.getfuncs("parent"), o2));
		BoolExpr lhs = (BoolExpr) ctx.mkApp(objs.getfuncs("sibling"), o1, o2);
		BoolExpr body = ctx.mkImplies(lhs, rhs);
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

	public BoolExpr mk_ar_on_writes() {
		BoolExpr lhs = (BoolExpr) ctx.mkApp(objs.getfuncs("ar"), o1, o2);
		BoolExpr rhs1 = (BoolExpr) ctx.mkApp(objs.getfuncs("is_update"), o1);
		BoolExpr rhs2 = (BoolExpr) ctx.mkApp(objs.getfuncs("is_update"), o2);
		BoolExpr body = ctx.mkImplies(lhs, ctx.mkAnd(rhs1, rhs2));
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

	public BoolExpr mk_vis_then_ar() {
		BoolExpr lhs1 = (BoolExpr) ctx.mkApp(objs.getfuncs("vis"), o1, o2);
		BoolExpr lhs2 = (BoolExpr) ctx.mkApp(objs.getfuncs("is_update"), o1);
		BoolExpr lhs3 = (BoolExpr) ctx.mkApp(objs.getfuncs("is_update"), o2);
		BoolExpr rhs = (BoolExpr) ctx.mkApp(objs.getfuncs("ar"), o1, o2);
		BoolExpr body = ctx.mkImplies(ctx.mkAnd(lhs1, lhs2, lhs3), rhs);
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

	public BoolExpr mk_types_then_eq() {
		BoolExpr lhs1 = ctx.mkEq(ctx.mkApp(objs.getfuncs("parent"), o1), ctx.mkApp(objs.getfuncs("parent"), o2));
		BoolExpr lhs2 = ctx.mkEq(ctx.mkApp(objs.getfuncs("otype"), o1), ctx.mkApp(objs.getfuncs("otype"), o2));
		BoolExpr rhs = (BoolExpr) ctx.mkEq(o1, o2);
		BoolExpr body = ctx.mkImplies(ctx.mkAnd(lhs1, lhs2), rhs);
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

	public BoolExpr mk_no_loops_o() {
		BoolExpr ass1 = (BoolExpr) ctx.mkApp(objs.getfuncs("WR_O"), o1, o1);
		BoolExpr ass2 = (BoolExpr) ctx.mkApp(objs.getfuncs("RW_O"), o1, o1);
		BoolExpr ass3 = (BoolExpr) ctx.mkApp(objs.getfuncs("WW_O"), o1, o1);
		BoolExpr body = ctx.mkNot(ctx.mkOr(ass1, ass2, ass3));
		Quantifier x = ctx.mkForall(new Expr[] { o1 }, body, 1, null, null, null, null);
		return x;
	}

	public BoolExpr mk_trans_ar() {
		BoolExpr lhs1 = (BoolExpr) ctx.mkApp(objs.getfuncs("ar"), o1, o2);
		BoolExpr lhs2 = (BoolExpr) ctx.mkApp(objs.getfuncs("ar"), o2, o3);
		BoolExpr rhs = (BoolExpr) ctx.mkApp(objs.getfuncs("ar"), o1, o3);
		BoolExpr body = ctx.mkImplies(ctx.mkAnd(lhs1, lhs2), rhs);
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2, o3 }, body, 1, null, null, null, null);
		return x;
	}

	public BoolExpr mk_total_ar() {
		BoolExpr lhs1 = (BoolExpr) ctx.mkApp(objs.getfuncs("is_update"), o1);
		BoolExpr lhs2 = (BoolExpr) ctx.mkApp(objs.getfuncs("is_update"), o2);
		BoolExpr lhs3 = (BoolExpr) ctx.mkNot(ctx.mkEq(o1, o2));
		BoolExpr lhs4 = ctx.mkNot((BoolExpr) ctx.mkApp(objs.getfuncs("sibling"), o1, o2));
		BoolExpr rhs1 = (BoolExpr) ctx.mkApp(objs.getfuncs("ar"), o1, o2);
		BoolExpr rhs2 = (BoolExpr) ctx.mkApp(objs.getfuncs("ar"), o2, o1);
		BoolExpr body = ctx.mkImplies(ctx.mkAnd(lhs1, lhs2, lhs3, lhs4), ctx.mkXor(rhs1, rhs2));
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

	public Quantifier mk_wr_then_vis() {
		BoolExpr lhs = (BoolExpr) ctx.mkApp(objs.getfuncs("WR_O"), o1, o2);
		BoolExpr rhs = (BoolExpr) ctx.mkApp(objs.getfuncs("vis"), o1, o2);
		BoolExpr body = ctx.mkImplies(lhs, rhs);
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

	public Quantifier mk_rw_then_not_vis() {
		BoolExpr lhs = (BoolExpr) ctx.mkApp(objs.getfuncs("RW_O"), o1, o2);
		BoolExpr rhs = (BoolExpr) ctx.mkApp(objs.getfuncs("vis"), o2, o1);
		BoolExpr body = ctx.mkImplies(lhs, ctx.mkNot(rhs));
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

	public Quantifier mk_ww_then_ar() {
		BoolExpr lhs = (BoolExpr) ctx.mkApp(objs.getfuncs("WW_O"), o1, o2);
		BoolExpr rhs = (BoolExpr) ctx.mkApp(objs.getfuncs("ar"), o1, o2);
		BoolExpr body = ctx.mkImplies(lhs, rhs);
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

	public Quantifier mk_irreflx_ar() {
		BoolExpr body = (BoolExpr) ctx.mkApp(objs.getfuncs("ar"), o1, o1);
		Quantifier x = ctx.mkForall(new Expr[] { o1 }, ctx.mkNot(body), 1, null, null, null, null);
		return x;
	}

	public Quantifier mk_irreflx_sibling() {
		BoolExpr body = (BoolExpr) ctx.mkApp(objs.getfuncs("sibling"), o1, o1);
		Quantifier x = ctx.mkForall(new Expr[] { o1 }, ctx.mkNot(body), 1, null, null, null, null);
		return x;
	}

	public Quantifier mk_gen_dep() {
		BoolExpr lhs = (BoolExpr) ctx.mkApp(objs.getfuncs("D"), o1, o2);
		BoolExpr rhs1 = ctx.mkNot((BoolExpr) ctx.mkApp(objs.getfuncs("sibling"), o1, o2));
		BoolExpr rhs2 = ctx.mkOr((BoolExpr) ctx.mkApp(objs.getfuncs("WW_O"), o1, o2),
				(BoolExpr) ctx.mkApp(objs.getfuncs("WR_O"), o1, o2),
				(BoolExpr) ctx.mkApp(objs.getfuncs("RW_O"), o1, o2));
		BoolExpr body = ctx.mkImplies(lhs, ctx.mkAnd(rhs1, rhs2));
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

	public Quantifier mk_gen_depx() {
		BoolExpr lhs = (BoolExpr) ctx.mkApp(objs.getfuncs("X"), o1, o2);
		BoolExpr rhs1 = (BoolExpr) ctx.mkApp(objs.getfuncs("sibling"), o1, o2);
		BoolExpr rhs2 = (BoolExpr) ctx.mkApp(objs.getfuncs("D"), o1, o2);
		BoolExpr body = ctx.mkImplies(lhs, ctx.mkOr(rhs1, rhs2));
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

	// the final assertion, generating a cycle on the dependency graph
	public Quantifier mk_cycle() {
		int length = ConstantArgs._DEP_CYCLE_LENGTH;
		Expr[] Os = new Expr[length];
		for (int i = 0; i < length; i++)
			Os[i] = ctx.mkFreshConst("o", objs.getSort("O"));

		BoolExpr notEqExprs[] = new BoolExpr[length];
		for (int i = 0; i < length - 1; i++)
			notEqExprs[i] = ctx.mkNot(ctx.mkEq(Os[i], Os[i + 1]));
		notEqExprs[length - 1] = ctx.mkNot(ctx.mkEq(Os[length - 1], Os[0]));

		BoolExpr depExprs[] = new BoolExpr[length];
		for (int i = 1; i < length - 1; i++)
			depExprs[i] = (BoolExpr) ctx.mkApp(objs.getfuncs("X"), Os[i], Os[i + 1]);
		depExprs[length - 1] = (BoolExpr) ctx.mkApp(objs.getfuncs("X"), Os[length - 1], Os[0]);
		depExprs[0] = (BoolExpr) ctx.mkApp(objs.getfuncs("D"), Os[0], Os[1]);

		BoolExpr body = ctx.mkAnd(ctx.mkAnd(notEqExprs), ctx.mkAnd(depExprs));
		Quantifier x = ctx.mkExists(Os, body, 1, null, null, null, null);
		return x;
	}

	//

	//
	//
}
