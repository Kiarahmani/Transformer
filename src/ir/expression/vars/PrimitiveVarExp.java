package ir.expression.vars;

import ir.Type;

public class PrimitiveVarExp extends VarExp {
	private Type type;

	public Type getType() {
		return type;
	}

	public PrimitiveVarExp(String name, Type type) {
		super(name);
		this.type = type;
	}

}
