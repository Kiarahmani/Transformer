package ir.schema;

import java.util.ArrayList;
import ir.schema.Column;
import exceptions.ColumnDoesNotExist;

public class Table {
	private String name;
	private ArrayList<Column> columns;

	public Table(String name) {
		this.name = name;
		columns = new ArrayList<Column>();
	}

	public void addColumn(Column c) {
		this.columns.add(c);
	}

	public Column getColumn(int i) {
		return this.columns.get(i);
	}

	public Column getColumn(String n) throws ColumnDoesNotExist {
		Column result = null;
		for (Column c : this.columns)
			if (c.getName().equals(n)) {
				result = c;
				break;
			}
		if (result == null)
			throw new ColumnDoesNotExist(n);
		return result;
	}

	public void printTable() {
		System.out.println("\n------------------");
		System.out.println("Table <<" + name + ">>");
		for (Column c : this.columns)
			c.printColumn();
		
	}

}