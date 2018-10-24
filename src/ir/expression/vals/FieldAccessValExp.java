package ir.expression.vals;

import ir.expression.Expression;
import ir.expression.vars.RowVarExp;
import ir.schema.Column;

public class FieldAccessValExp extends ValExp {
	RowVarExp projectee;
	Column col;

	public FieldAccessValExp(RowVarExp projectee, Column col) {
		this.projectee = projectee;
		this.col = col;
	}

	@Override
	public Expression getUpdateExp(Expression newExp, int index) {
		return this;
	}

}
