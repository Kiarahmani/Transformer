package ir.expression;

/* an expression is either made of */
public abstract class Expression {

	public abstract Expression getUpdateExp(Expression newExp, int index);

}
