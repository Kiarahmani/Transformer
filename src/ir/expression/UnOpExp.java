package ir.expression;

public class UnOpExp extends Expression {
	public enum UnOp {
		NOT
	};

	UnOp op;
	Expression e;

	public UnOpExp(UnOp op, Expression e) {
		this.op = op;
		this.e = e;

	}

	public String toString() {
		return "(" + op + " " + e.toString() + ")";
	}

	@Override
	public Expression getUpdateExp(Expression newExp, int index) {
		return new UnOpExp(this.op, this.e.getUpdateExp(newExp, index));
	}
}
