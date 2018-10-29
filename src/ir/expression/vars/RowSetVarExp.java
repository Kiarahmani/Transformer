package ir.expression.vars;

import ir.expression.Expression;
import ir.schema.Table;

public class RowSetVarExp extends VarExp {

	private Table table;
	private Expression whereClause;

	public RowSetVarExp(String name, Table table, Expression wh) {
		super(name);
		this.table = table;
		this.whereClause = wh;
	}

	public Expression getWhClause() {
		return this.whereClause;
	}



	public Table getTable() {
		return this.table;
	}

	public String toString() {
		return "ROW SET: " + this.table.getName() + " WHERE: " + whereClause;
	}

	@Override
	public Expression getUpdateExp(Expression newExp, int index) {
		this.whereClause = this.whereClause.getUpdateExp(newExp, index);
		return this;
	}

}
