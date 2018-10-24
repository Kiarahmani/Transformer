package ir.expression.vars;

import ir.expression.Expression;
import ir.schema.Table;

public class RowVarExp extends VarExp {

	private Table table;
	private RowSetVarExp belongsTo;

	public RowVarExp(String name, Table table, RowSetVarExp belongsTo) {
		super(name);
		this.table = table;
		this.belongsTo = belongsTo;
	}

	public Table getTable() {
		return this.table;
	}

	public String toString() {
		return "ROW:" + this.table.getName() + "-" + this.getName();
	}

	@Override
	public Expression getUpdateExp(Expression newExp, int index) {
		return this;
	}

}
