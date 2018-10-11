package gimpToApp;

import java.util.List;

import soot.SootField;
import soot.SootMethod;
import soot.Value;

public class GValue {
	boolean isGrimpValue;
	SootMethod method;
	GValue base;
	SootField field;
	List<GValue> args;
	Value grimpValue;

	public String shortName() {
		if (grimpValue != null)
			return grimpValue.toString();
		else
			return "-";
	}

	public void setMethod(SootMethod method) {
		this.method = method;
	}

	public void setBase(GValue base) {
		this.base = base;
	}

	public void setField(SootField field) {
		this.field = field;
	}

	public void setArgs(List<GValue> args) {
		this.args = args;
	}

	public void setGrimpValue(Value grimpValue) {
		this.grimpValue = grimpValue;
	}

	// constructor
	public GValue(boolean isGrimpValue) {
		this.isGrimpValue = isGrimpValue;
	}

	public void print() {
		System.out.println(internalPrint(1));
	}

	public String internalPrint(int indent) {
		String space = String.format("\n" + "%0" + String.valueOf(indent * 3) + "d", 0).replace("0", " ");
		String methodName = (method == null) ? "null" : method.getName();
		String argsName = "";
		if (args != null)
			for (GValue a : args)
				argsName += "|" + a.shortName() + "|";

		if (isGrimpValue) {
			return space + "<<GrimpValue: " + grimpValue + ">> ";
		} else {
			if (base != null)
				return space + "<<GValue: " + base.internalPrint(indent + 1) + "(" + methodName + ") " + argsName
						+ ">>";
			else
				return space + "<<GValue: " + "-" + "(" + methodName + ") " + argsName + ">>";

		}
	}

}
