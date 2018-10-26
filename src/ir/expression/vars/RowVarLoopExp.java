package ir.expression.vars;

import ir.expression.Expression;
import ir.schema.Table;

public class RowVarLoopExp extends RowVarExp {

	public RowVarLoopExp(String name, Table table, RowSetVarExp belongsTo) {
		super(name, table, belongsTo);
		// TODO Auto-generated constructor stub
	}


	public String toString() {
		return super.toString();
	}

	@Override
	public Expression getUpdateExp(Expression newExp, int index) {
		return this;
	}

}
