package ir.expression.vals;

import ir.Type;
import ir.expression.Expression;

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

	public String toString() {
		return this.name + "_param";
	}

	@Override
	public Expression getUpdateExp(Expression newExp, int index) {
		return this;
	}
}
