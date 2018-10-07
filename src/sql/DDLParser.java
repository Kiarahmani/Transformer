package sql;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import exceptions.SqlTypeNotFoundException;
import ir.Type;
import ir.schema.Column;
import ir.schema.Table;
import utils.Utils;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.*;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.Index;

public class DDLParser {

	public ArrayList<Table> parse() {
		BufferedReader in = null;
		ArrayList<String> statements = new ArrayList<String>();
		ArrayList<Table> tables = new ArrayList<Table>();
		// pasre the file
		try {
			in = new BufferedReader(new FileReader("src/tests/ddl.sql"));
			String read = null;
			String iter_s = "";
			while ((read = in.readLine()) != null) {
				iter_s += read;
				if (read.contains(";")) {
					statements.add(iter_s);
					iter_s = "";
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		// create statements and later the tables
		try {
			Statement statement;
			for (String s : statements) {
				statement = CCJSqlParserUtil.parse(s);
				CreateTable createStatement = (CreateTable) statement;
				tables.add(createTable(createStatement));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return tables;
	}

	private Table createTable(CreateTable ct) {
		// TODO Auto-generated method stub
		Table t = new Table(ct.getTable().getName());
		List<String> pkList = new ArrayList<String>();
		// filter out non interesting (for now) schema constraints
		for (Index i : ct.getIndexes())
			if (i.getType().equals("PRIMARY KEY"))
				pkList.addAll(i.getColumnsNames());

		for (ColumnDefinition cd : ct.getColumnDefinitions()) {
			try {
				t.addColumn(new Column(cd.getColumnName(), Utils.convertType(cd.getColDataType().getDataType()),
						pkList.contains(cd.getColumnName())));
			} catch (SqlTypeNotFoundException e) {
				e.printStackTrace();
			}
		}
		return t;
	}
}

//