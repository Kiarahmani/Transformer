package ir.statement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import exceptions.WhereClauseNotKnownException;
import gimpToApp.UnitData;
import ir.expression.BinOpExp;
import ir.expression.BinOpExp.BinOp;
import ir.expression.UnOpExp.UnOp;
import ir.expression.vals.ConstValExp;
import ir.expression.vals.ProjValExp;
import ir.expression.Expression;
import ir.expression.UnOpExp;
import ir.expression.vars.UnknownExp;
import ir.schema.Column;
import ir.schema.Table;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.TablesNamesFinder;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.*;
import net.sf.jsqlparser.expression.operators.relational.*;

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
		try {
			this.whereClause = extractWC();
		} catch (WhereClauseNotKnownException e) {
			e.printStackTrace();
		}

		System.out.println("===" + this.whereClause);
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

	private Expression extractWC() throws WhereClauseNotKnownException {
		switch (this.kind) {
		case SELECT:
			Select select = (Select) statements;
			PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
			return recExtractWC(plainSelect.getWhere());
		case UPDATE:
			Update updateStatement = (Update) statements;
			return recExtractWC(updateStatement.getWhere());
		case DELETE:
			Delete deleteStatement = (Delete) statements;
			return recExtractWC(deleteStatement.getWhere());
		case INSERT:
			Insert insertStatement = (Insert) statements;
			return new UnknownExp("?");
		default:
			break;
		}
		return null;
	}

	private Expression recExtractWC(net.sf.jsqlparser.expression.Expression clause)
			throws WhereClauseNotKnownException {
		switch (clause.getClass().getSimpleName()) {
		case "EqualsTo":
			EqualsTo eq = (EqualsTo) clause;
			return new BinOpExp(BinOp.EQ, recExtractWC(eq.getLeftExpression()), recExtractWC(eq.getRightExpression()));
		case "AndExpression":
			AndExpression ae = (AndExpression) clause;
			return new BinOpExp(BinOp.AND, recExtractWC(ae.getLeftExpression()), recExtractWC(ae.getRightExpression()));
		case "MinorThan":
			MinorThan mt = (MinorThan) clause;
			return new BinOpExp(BinOp.LT, recExtractWC(mt.getLeftExpression()), recExtractWC(mt.getRightExpression()));
		case "GreaterThan":
			GreaterThan gt = (GreaterThan) clause;
			return new BinOpExp(BinOp.GT, recExtractWC(gt.getLeftExpression()), recExtractWC(gt.getRightExpression()));
		case "GreaterThanEquals":
			GreaterThanEquals gte = (GreaterThanEquals) clause;
			return new BinOpExp(BinOp.GEQ, recExtractWC(gte.getLeftExpression()),
					recExtractWC(gte.getRightExpression()));
		case "MinorThanEquals":
			MinorThanEquals mte = (MinorThanEquals) clause;
			return new BinOpExp(BinOp.LEQ, recExtractWC(mte.getLeftExpression()),
					recExtractWC(mte.getRightExpression()));
		case "NotEqualsTo":
			NotEqualsTo neq = (NotEqualsTo) clause;
			return new UnOpExp(UnOp.NOT, new BinOpExp(BinOp.EQ, recExtractWC(neq.getLeftExpression()),
					recExtractWC(neq.getRightExpression())));
		case "Column":
			Column c = null;
			for (Column tc : table.getColumns())
				if (tc.getName().equals(clause.toString())) {
					c = tc;
					break;
				}
			return new ProjValExp(c, table);
		case "JdbcParameter":
			return new UnknownExp("?");
		case "LongValue":
			long lv = ((LongValue) clause).getValue();
			return new ConstValExp(lv);
		default:
			throw new WhereClauseNotKnownException(
					"Query.java.recExtractWC: clase node not handled yet: " + clause.getClass().getSimpleName());
		}

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
