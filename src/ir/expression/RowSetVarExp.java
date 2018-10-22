package ir.expression;

import ir.schema.Table;

public class RowSetVarExp extends VarExp {

	private Table table;

	public RowSetVarExp(String name, Table table) {
		super(name);
		this.table = table;
	}

	public Table getTable() {
		return this.table;
	}

}
