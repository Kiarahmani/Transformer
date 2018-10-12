package ir.statement;

// my types assigned to SQL operations which will be used to generate concrete execution paths.
public class SqlStmtType {
	public boolean isUpdate;
	String txnName;
	String kind;
	int number;

	public SqlStmtType(String txnName, String kind, int number, boolean isUpdate) {
		this.kind = kind;
		this.number = number;
		this.isUpdate = isUpdate;
		this.txnName = txnName;
	}

	public String toString() {
		return txnName + "-" + kind + "#" + String.valueOf(number);
	}
}
