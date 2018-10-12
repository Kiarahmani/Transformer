package ir.statement;

public class InvokeStmt extends Statement {
	private SqlStmtType type;
	Query query;

	public InvokeStmt(Query query) {
		this.query = query;
	}
	
	public void setType(SqlStmtType type) {
		this.type=type;
	}
	
	public Query getQuery() {
		return this.query;
	}
	
	public String toString() {
		return this.query.text;
	}

	public SqlStmtType getType() {
		return type;
	}
}
