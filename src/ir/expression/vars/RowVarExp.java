package ir.expression.vars;

import ir.expression.Expression;
import ir.schema.Table;

public class RowVarExp extends VarExp {

	private Table table;

	public RowVarExp(String name, Table table) {
		super(name);
		this.table = table;
	}

	public Table getTable() {
		return this.table;
	}

	@Override
	public Expression getUpdateExp(Expression newExp, int index) {
		return this;
	}

}
