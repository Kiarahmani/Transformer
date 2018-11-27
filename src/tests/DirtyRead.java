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

public class DirtyRead {
	private Connection connect = null;
	private ResultSet rs = null;
	private int _ISOLATION = Connection.TRANSACTION_READ_COMMITTED;
	private int insID;
	Properties p;

	public DirtyRead(int insID) {
		this.insID = insID;
		p = new Properties();
		p.setProperty("ID", String.valueOf(insID));
	}

	public void updateBalance(int value1, int value2) throws Exception {
		try {
			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			
			PreparedStatement preparedStatement = connect.prepareStatement("update A set balance= ? where id=1");
			preparedStatement.setInt(1, value1);
			preparedStatement.executeUpdate();
			PreparedStatement preparedStatement2 = connect.prepareStatement("update A set balance= ? where id=1");
			preparedStatement2.setInt(1, value2);
			preparedStatement2.executeUpdate();
		} catch (Exception e) {
			throw e;
		} finally {

		}

	}

	public void readBalance(int key) throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			PreparedStatement preparedStatement = connect.prepareStatement("select * from A where id=1");
			ResultSet rs = preparedStatement.executeQuery();
			int result = -1;
			if (rs.next()) {
				result = rs.getInt("balance");
			}
			System.out.println(result);
		} catch (Exception e) {
			throw e;
		} finally {

		}

	}
	
	
	public void updateBalance3(int value1, int value2) throws Exception {
		try {
			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			
			PreparedStatement preparedStatement = connect.prepareStatement("update A set balance= ? where id=1");
			preparedStatement.setInt(1, value1);
			preparedStatement.executeUpdate();
			PreparedStatement preparedStatement2 = connect.prepareStatement("update A set balance= ? where id=1");
			preparedStatement2.setInt(1, value2);
			preparedStatement2.executeUpdate();
			
			PreparedStatement preparedStatement3 = connect.prepareStatement("update A set balance= ? where id=5");
			preparedStatement3.setInt(1, value1);
			preparedStatement3.executeUpdate();
			PreparedStatement preparedStatement4 = connect.prepareStatement("update A set balance= ? where id=4");
			preparedStatement4.setInt(1, value2);
			preparedStatement4.executeUpdate();
		} catch (Exception e) {
			throw e;
		} finally {

		}

	}

	public void readBalance3(int key) throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			PreparedStatement preparedStatement = connect.prepareStatement("select * from A where id=4");
			ResultSet rs = preparedStatement.executeQuery();
			int result = -1;
			if (rs.next()) {
				result = rs.getInt("balance");
			}
			System.out.println(result);
		} catch (Exception e) {
			throw e;
		} finally {

		}

	}
	
	public void updateBalance2(int value1, int value2) throws Exception {
		try {
			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			
			PreparedStatement preparedStatement = connect.prepareStatement("update A set balance= ? where id=2");
			preparedStatement.setInt(1, value1);
			preparedStatement.executeUpdate();
			PreparedStatement preparedStatement2 = connect.prepareStatement("update A set balance= ? where id=3");
			preparedStatement2.setInt(1, value2);
			preparedStatement2.executeUpdate();
		} catch (Exception e) {
			throw e;
		} finally {

		}

	}

	public void readBalance2(int key) throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			PreparedStatement preparedStatement = connect.prepareStatement("select * from A where id=2");
			ResultSet rs = preparedStatement.executeQuery();
			int result = -1;
			if (rs.next()) {
				result = rs.getInt("balance");
			}
			System.out.println(result);
		} catch (Exception e) {
			throw e;
		} finally {

		}

	}

}
