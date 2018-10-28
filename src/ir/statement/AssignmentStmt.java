package ir.statement;

import ir.expression.Expression;
import ir.expression.vars.VarExp;

public class AssignmentStmt extends Statement {
	public AssignmentStmt(Expression pathCond, VarExp lhs, Expression rhs) {
		super(pathCond);
		this.lhs = lhs;
		this.rhs = rhs;
	}

	public VarExp getLhs() {
		return lhs;
	}

	public Expression getRhs() {
		return rhs;
	}

	VarExp lhs;
	Expression rhs;

}
