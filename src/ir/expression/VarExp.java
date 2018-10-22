package ir.expression;

public class VarExp extends Expression {
	private String name;

	public String getName() {
		return this.name;
	}

	public VarExp(String name) {
		this.name = name;
	}
}
