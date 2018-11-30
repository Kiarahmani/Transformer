package tests;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class TestApp {
	private Connection connect = null;
	private ResultSet rs = null;
	private int _ISOLATION = Connection.TRANSACTION_READ_COMMITTED;
	private int insID;
	Properties p;

	public TestApp(int insID) {
		this.insID = insID;
		p = new Properties();
		p.setProperty("ID", String.valueOf(insID));
	}

	public void writeTxn(int value1, int value2, int value3) throws Exception {
		try {
			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			// update #1
			PreparedStatement preparedStatement = connect.prepareStatement("update A set balance= ? where id=1");
			preparedStatement.setInt(1, value1);
			preparedStatement.executeUpdate();
			// update #2
			PreparedStatement preparedStatement2 = connect.prepareStatement("update A set balance= ? where id=1");
			preparedStatement2.setInt(1, value2);
			preparedStatement2.executeUpdate();
			// update #3
			//PreparedStatement preparedStatement3 = connect.prepareStatement("update A set balance= ? where id=1");
			//preparedStatement3.setInt(1, value3);
			//preparedStatement3.executeUpdate();
		} catch (Exception e) {
			throw e;
		} finally {

		}

	}

	public void readTxn(int key) throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			// select #1
			PreparedStatement preparedStatement = connect.prepareStatement("select * from A where id=1");
			ResultSet rs = preparedStatement.executeQuery();
			int result = -1;
			if (rs.next()) {
				result = rs.getInt("balance");
			}
			System.out.println(result);
			// select #2
			PreparedStatement preparedStatement2 = connect.prepareStatement("select * from A where id=1");
			ResultSet rs2 = preparedStatement2.executeQuery();
			int result2 = -1;
			if (rs2.next()) {
				result2 = rs2.getInt("balance");
			}
			System.out.println(result2);
		} catch (Exception e) {
			throw e;
		} finally {

		}

	}
	
	
	
	
	

}
