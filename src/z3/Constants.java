package z3;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Quantifier;
import com.microsoft.z3.Sort;
import com.microsoft.z3.Symbol;

public class Constants {
	Context ctx;
	DeclaredObjects objs;

	public Constants(Context ctx, DeclaredObjects objs) {
		this.ctx = ctx;
		this.objs = objs;

	}

	public Quantifier mk_par_then_sib() {
		Expr o1 = ctx.mkFreshConst("o", objs.getSort("O"));
		Expr o2 = ctx.mkFreshConst("o", objs.getSort("O"));
		BoolExpr lhs = ctx.mkEq(ctx.mkApp(objs.getfuncs("parent"), o1), ctx.mkApp(objs.getfuncs("parent"), o2));
		BoolExpr rhs = (BoolExpr) ctx.mkApp(objs.getfuncs("sibling"), o1, o2);
		BoolExpr body = ctx.mkImplies(lhs, rhs);
		Quantifier x = ctx.mkForall(new Expr[] { o1, o2 }, body, 1, null, null, null, null);
		return x;
	}

}
