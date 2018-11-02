package ir.statement;

import ir.expression.Expression;

public class Statement {
	private Expression pathCond;

	public Statement(Expression pathCond) {
		this.setPathCond(pathCond);
	}

	public Expression getPathCond() {
		return pathCond;
	}

	public void setPathCond(Expression pathCond) {
		this.pathCond = pathCond;
	}
}
