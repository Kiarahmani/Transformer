package ir.statement;

import ir.expression.Expression;

public class Statement {
	Expression pathCond;

	public Statement(Expression pathCond) {
		this.pathCond = pathCond;
	}
}
