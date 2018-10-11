package ir;

import java.util.ArrayList;

import ir.statement.Statement;

public class Transaction {
	private String name;
	private ArrayList<Statement> stmts;

	public Transaction(String name) {
		this.name = name;
		this.stmts = new ArrayList<Statement>();
	}

	public void addStmt(Statement stmt) {
		this.stmts.add(stmt);
	}

	public void printTxn() {
		System.out.println("TXN (" + name + ")");
		for (Statement stmt : stmts)
			System.out.println(" __ " + stmt);
	}

}
