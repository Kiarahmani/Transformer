package ir.statement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gimpToApp.UnitData;
import ir.expression.Expression;
import ir.schema.Column;
import ir.schema.Table;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.TablesNamesFinder;

public class Query {
	public enum Kind {
		SELECT, INSERT, DELETE, UPDATE
	};

	UnitData data;
	ArrayList<Table> tables;
	String text;
	Kind kind;
	Table table;
	List<Column> s_columns;
	List<Expression> i_values;
	Map<Column, Expression> u_updates;
	Expression whereClause;
	Statement statements;

	public Query(String query, UnitData data, ArrayList<Table> tables) {
		this.data = data;
		this.tables = tables;
		this.text = query.substring(1, query.length() - 1);
		//
		// analyze the text and generate appropriate objects
		try {
			statements = CCJSqlParserUtil.parse(text);
		} catch (JSQLParserException e) {
			e.printStackTrace();
		}
		this.kind = extractKind();
		this.table = extractTable();
		this.whereClause = extractWC();
		this.s_columns = extractSCols();
		this.i_values = extractIVals();
		this.u_updates = extractUfuncs();
	}

	private Map<Column, Expression> extractUfuncs() {
		// TODO Auto-generated method stub
		return null;
	}

	private List<Expression> extractIVals() {
		// TODO Auto-generated method stub
		return null;
	}

	private List<Column> extractSCols() {
		// TODO Auto-generated method stub
		return null;
	}

	private Expression extractWC() {
		switch (this.kind) {
		case SELECT:
			Select selectStatement = (Select) statements;
			System.out.println("===" + selectStatement.getSelectBody());
			break;
		case UPDATE:
			Update updateStatement = (Update) statements;
			System.out.println(updateStatement.getWhere());
			break;
		case DELETE:
			Delete deleteStatement = (Delete) statements;
			break;
		case INSERT:
			Insert insertStatement = (Insert) statements;
			break;
		default:
			break;
		}
		return null;
	}

	private Table extractTable() {
		TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
		List<String> tableList = tablesNamesFinder.getTableList(statements);
		for (Table t : tables)
			if (t.getName().toLowerCase().equals(tableList.get(0).toLowerCase()))
				return t;
		return null;
	}

	private Kind extractKind() {
		switch (statements.getClass().getSimpleName()) {
		case "Select":
			return Kind.SELECT;
		case "Update":
			return Kind.UPDATE;
		case "Insert":
			return Kind.INSERT;
		case "Delete":
			return Kind.DELETE;
		default:
			return null;
		}
	}

	public String toString() {
		String k = this.kind.toString();
		String t = this.table.getName();
		String wc = this.whereClause.toString();
		String v = this.i_values.toString();
		String u = this.u_updates.toString();
		String c = this.s_columns.toString();
		switch (this.kind) {
		case SELECT:
			return k + "[" + t + ":" + c + "] " + " <<" + wc + ">>";
		case INSERT:
			return k + "[" + t + "] " + v;
		case DELETE:
			return k + "[" + t + "] " + "<<" + wc + ">>";
		case UPDATE:
			return k + "[" + t + "] " + u + " <<" + wc + ">>";

		default:
			return "UNKNOWN QUERY";
		}
	}

	// GETTER & SETTERS
	public String getText() {
		return text;
	}

}
