import java.util.ArrayList;

import ir.Table;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.*;
import net.sf.jsqlparser.statement.create.table.CreateTable;

public class SQLParser {

	ArrayList<Table> parse() {
		try {
			Statement statement = CCJSqlParserUtil.parse("CREATE TABLE ACCOUNT ("
					+ "    ACC_ID       BIGINT NOT NULL," + "    ACC_NAME     VARCHAR(64) NOT NULL,"
					+ "    ACC_BAL    	 INT NOT NULL," + "    PRIMARY KEY (ACC_ID)" + ");");
			CreateTable selectStatement = (CreateTable) statement;
			System.out.println(selectStatement.getColumnDefinitions());

		} catch (JSQLParserException e) {
			e.printStackTrace();
		}
		return null;
	}

}
