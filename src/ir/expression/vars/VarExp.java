package ir.expression.vars;

import ir.expression.Expression;

public abstract class VarExp extends Expression {
	private String name;

	public String getName() {
		return this.name;
	}

	public VarExp(String name) {
		this.name = name;
	}


}
