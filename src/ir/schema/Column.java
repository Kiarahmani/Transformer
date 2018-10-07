package ir.schema;

import ir.Type;

public class Column {
	public String name;
	private Type type;
	private boolean isPK;

	public Column(String name, Type type, boolean isPK) {
		this.name = name;
		this.type = type;
		this.isPK = isPK;
	}

	public String getName() {
		return this.name;
	}

	public Type getType() {
		return this.type;
	}

	public boolean isPK() {
		return this.isPK;
	}

	public void printColumn() {
		if (this.isPK)
			System.out.print(" |" + this.name + "(" + this.type + ",PK)|");
		else
			System.out.print(" |" + this.name + "(" + this.type + ")|");
	}

}
