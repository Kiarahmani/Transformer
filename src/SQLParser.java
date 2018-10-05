import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import ir.Table;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.*;
import net.sf.jsqlparser.statement.create.table.CreateTable;

public class SQLParser {
	ArrayList<Table> parse() {

		BufferedReader in = null;
		ArrayList<String> statements = new ArrayList<String>();
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

		try {
			Statement statement;
			for (String s:statements) {
				System.out.println("----------------------");
				statement = CCJSqlParserUtil.parse(s);
				CreateTable createStatement = (CreateTable) statement;
				System.out.println(createStatement.getColumnDefinitions().get(2).getColDataType());
				System.out.println("----------------------");
			}
	
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
