package ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ir.statement.InvokeStmt;
import ir.statement.SqlStmtType;
import ir.statement.Statement;

public class Transaction {
	private String name;
	private ArrayList<Statement> stmts;

	public String getName() {
		return this.name;
	}

	public Transaction(String name) {
		this.name = name;
		this.stmts = new ArrayList<Statement>();
	}

	public void addStmt(Statement stmt) {
		this.stmts.add(stmt);
	}

	public void setTypes() {
		int selectCount = 0;
		int insertCount = 0;
		int deleteCount = 0;
		int updateCount = 0;
		int seq = 0;
		for (Statement s : stmts)
			try {
				InvokeStmt is = (InvokeStmt) s;
				if (is.getQuery().getText().toLowerCase().contains("select"))
					is.setType(new SqlStmtType(name, "select", ++selectCount, false, ++seq));
				else if (is.getQuery().getText().toLowerCase().contains("insert"))
					is.setType(new SqlStmtType(name, "insert", ++insertCount, true, ++seq));
				else if (is.getQuery().getText().toLowerCase().contains("update"))
					is.setType(new SqlStmtType(name, "update", ++updateCount, true, ++seq));
				else if (is.getQuery().getText().toLowerCase().contains("delete"))
					is.setType(new SqlStmtType(name, "delete", ++deleteCount, true, ++seq));

			} catch (Exception e) {
			}

	}

	// return mapping from program order to the stmt name
	public Map<Integer, String> getStmtNamesMap() {
		Map<Integer, String> result = new HashMap<Integer, String>();
		int iter = -1;
		for (Statement s : this.stmts)
			try {
				InvokeStmt is = (InvokeStmt) s;
				result.put(is.getType().getSeq(), is.getType().toString());
			} catch (Exception e) {
			}
		return result;
	}

	public String[] getStmtNames() {
		String[] result = new String[this.stmts.size()];
		int iter = -1;
		for (Statement s : this.stmts)
			try {
				InvokeStmt is = (InvokeStmt) s;
				result[++iter] = is.getType().toString();
			} catch (Exception e) {
			}
		return result;
	}

	public List<String> getUpdateStmtNames() {
		List<String> result = new ArrayList<String>();
		for (Statement s : this.stmts)
			try {
				InvokeStmt is = (InvokeStmt) s;
				if (is.getType().isUpdate)
					result.add(is.getType().toString());
			} catch (Exception e) {
			}
		return result;
	}

	public void printTxn() {
		System.out.println("TXN (" + name + ")");
		for (Statement stmt : stmts)
			System.out.println(" ++ " + ((InvokeStmt) stmt).getType());
	}

}
