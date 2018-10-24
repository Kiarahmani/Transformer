package ir.expression.vars;

import ir.Type;
import ir.expression.Expression;

public class PrimitiveVarExp extends VarExp {
	private Type type;

	public Type getType() {
		return type;
	}

	public PrimitiveVarExp(String name, Type type) {
		super(name);
		this.type = type;
	}

	@Override
	public Expression getUpdateExp(Expression newExp, int index) {
		return this;
	}

}
