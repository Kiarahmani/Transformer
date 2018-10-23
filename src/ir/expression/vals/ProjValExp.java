package ir.expression.vals;

import ir.schema.Column;
import ir.schema.Table;

// these nodes represent columns; e.g. in conditionals (as opposed to fieldAccess which represent values of columns in a given specific row)
public class ProjValExp extends ValExp {
	Column column;
	Table table;

	public ProjValExp(Column column, Table table) {
		this.table = table;
		this.column = column;
	}

	public String toString() {
		return this.table.getName() + "." + this.column.getName();
	}

}
