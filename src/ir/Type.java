package ir;

public enum Type {
	INT, REAL, STRING, BOOLEAN;

	public String toZ3String() {
		switch (this) {
		case INT:
			return "Int";
		case REAL:
			return "Real";
		case STRING:
			return "String";
		case BOOLEAN:
			return "Bool";
		default:
			return null;
		}
	}

}
