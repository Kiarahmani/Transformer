package ir.expression.vars;

import ir.expression.Expression;
import ir.expression.vars.RowVarExp;
import ir.schema.Column;
import ir.schema.Table;

// these nodes represent columns; e.g. in conditionals (as opposed to fieldAccess which represent values of columns in a given specific row)
public class ProjVarExp extends VarExp {
	Column column;
	RowVarExp rVar;

	public ProjVarExp(String name, Column column, RowVarExp rVar) {
		super(name);
		this.column = column;
		this.rVar = rVar;
	}

	public String toString() {
		return "(" + rVar.toString() + ")." + this.column.getName();
	}

	@Override
	public Expression getUpdateExp(Expression newExp, int index) {
		return this;
	}

}
