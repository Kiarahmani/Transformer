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

public class SampleApp {
	private Connection connect = null;
	private Statement stmt = null;
	private int _ISOLATION = Connection.TRANSACTION_READ_COMMITTED;
	private int insID;
	Properties p;

	public SampleApp(int insID) {
		this.insID = insID;
		p = new Properties();
		p.setProperty("ID", String.valueOf(insID));
	}

	public void updateName(int key) throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			System.out.println("connected: " + connect);
			PreparedStatement preparedStatement = connect.prepareStatement("update B set name=Poor where id=2");
			preparedStatement.executeUpdate();

		} catch (Exception e) {
			throw e;
		} finally {
		}

	}

	public void updateBalance(int key1, int key2) throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			System.out.println("connected: " + connect);
			PreparedStatement preparedStatement2 = connect.prepareStatement("select * from A where id =?");
			preparedStatement2.setInt(1, 1);
			// preparedStatement2.setInt(2, key);
			ResultSet rs = preparedStatement2.executeQuery();
			rs.next();
			System.out.println(rs.getInt(1));
			PreparedStatement preparedStatement3 = connect.prepareStatement("update A set balance= 100 where id=?");
			preparedStatement3.setInt(1, 1);
			// preparedStatement2.setInt(2, key);
			preparedStatement3.executeUpdate();
			// }
		} catch (

		Exception e) {
			throw e;
		} finally {
		}

	}

	
	
	public void someTxn(int key1, int key2) throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			System.out.println("connected: " + connect);
			PreparedStatement preparedStatement2 = connect.prepareStatement("select * from A where id =?");
			preparedStatement2.setInt(1, 2);
			// preparedStatement2.setInt(2, key);
			ResultSet rs = preparedStatement2.executeQuery();
			rs.next();
			System.out.println(rs.getInt(1));
			PreparedStatement preparedStatement3 = connect.prepareStatement("update A set balance= 100 where id=?");
			preparedStatement3.setInt(1, 1);
			// preparedStatement2.setInt(2, key);
			preparedStatement3.executeUpdate();
			// }
		} catch (

		Exception e) {
			throw e;
		} finally {
		}

	}
	
	
	
	public void select(int key) throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			System.out.println("connected: " + connect);
			// if (key != 12) {
			PreparedStatement preparedStatement = connect.prepareStatement("select * from A where id =1");
			// preparedStatement.setInt(1, key);
			ResultSet rs = preparedStatement.executeQuery();
			rs.next();

			int id = rs.getInt("id");
			int balance = rs.getInt("balance");
			// }
			/*
			 * ResultSet rs1 = null; PreparedStatement preparedStatement1 =
			 * connect.prepareStatement("select * from B where id=?");
			 * preparedStatement1.setInt(1, rs.getInt("id")); rs1 =
			 * preparedStatement1.executeQuery(); rs1.next(); String name =
			 * rs1.getString("name"); System.out.println("(" + id + "," + name + "," +
			 * balance + ")");
			 */
		} catch (Exception e) {
			throw e;
		} finally {
		}

	}

}
