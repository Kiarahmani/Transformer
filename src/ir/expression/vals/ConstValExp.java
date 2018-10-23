package ir.expression.vals;

import ir.Type;

public class ConstValExp extends ValExp {
	private Type type;
	private String SVal;
	private double DVal;
	private int IVal;
	private boolean BVal;

	public Type getType() {
		return this.type;
	}

	public String getSVal() {
		return SVal;
	}

	public double getDVal() {
		return DVal;
	}

	public int getIVal() {
		return IVal;
	}

	public boolean isBVal() {
		return BVal;
	}

	public ConstValExp(int i) {
		this.IVal = i;
	}

	public ConstValExp(boolean b) {
		this.BVal = b;
	}

	public ConstValExp(double d) {
		this.DVal = d;
	}

	public ConstValExp(float d) {
		this.DVal = d;
	}

	public ConstValExp(String s) {
		this.SVal = s;
	}

}
