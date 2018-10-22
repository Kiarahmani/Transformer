package ir.statement;

import ir.expression.Expression;
import ir.expression.VarExp;

public class AssignmentStmt extends Statement {
	public VarExp getLhs() {
		return lhs;
	}

	public Expression getRhs() {
		return rhs;
	}

	VarExp lhs;
	Expression rhs;

	public AssignmentStmt(VarExp lhs, Expression rhs) {
		this.lhs = lhs;
		this.rhs = rhs;
	}
}
