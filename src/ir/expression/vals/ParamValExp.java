package ir.expression.vals;

import ir.Type;

public class ParamValExp extends ValExp {
	Type type;
	String name;

	public ParamValExp(String name, Type type) {
		this.type = type;
		this.name = name;
	}

	public Type getType() {
		return this.type;
	}

	public String getName() {
		return this.name;
	}
}
