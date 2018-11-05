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

	public void updateName(int key, String name) throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			System.out.println("connected: " + connect);
			PreparedStatement preparedStatement = connect.prepareStatement("update B set name=? where id=?");
			preparedStatement.setString(1, name);
			preparedStatement.setInt(2, key);
			preparedStatement.executeUpdate();

		} catch (Exception e) {
			throw e;
		} finally {
		}

	}

	public void updateBalance(int key, int balance) throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			System.out.println("connected: " + connect);

			PreparedStatement preparedStatement1 = connect.prepareStatement("update A set balance=? where id =?");
			preparedStatement1.setInt(1, balance);
			preparedStatement1.setInt(2, key);
			preparedStatement1.executeUpdate();

		} catch (

		Exception e) {
			throw e;
		} finally {
		}

	}

	public void select(int key1, int key2) throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			System.out.println("connected: " + connect);
			// if (key != 12) {
			PreparedStatement preparedStatement = connect.prepareStatement("select * from A where id =?");
			preparedStatement.setInt(1, key1);
			ResultSet rs = preparedStatement.executeQuery();
			rs.next();
			int id = rs.getInt("id");
			int balance = rs.getInt("balance");

			PreparedStatement preparedStatement1 = connect.prepareStatement("select * from B where id =?");
			preparedStatement1.setInt(1, key2);
			ResultSet rs1 = preparedStatement1.executeQuery();
			rs1.next();

		} catch (Exception e) {
			throw e;
		} finally {
		}
	}

	public void select2(int key1, int key2) throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			System.out.println("connected: " + connect);
			// if (key != 12) {

			PreparedStatement preparedStatement1 = connect.prepareStatement("select * from B where id =?");
			preparedStatement1.setInt(1, key2);
			ResultSet rs1 = preparedStatement1.executeQuery();
			rs1.next();

			PreparedStatement preparedStatement = connect.prepareStatement("select * from A where id =?");
			preparedStatement.setInt(1, key1);
			ResultSet rs = preparedStatement.executeQuery();
			rs.next();
			int id = rs.getInt("id");
			int balance = rs.getInt("balance");

		} catch (Exception e) {
			throw e;
		} finally {
		}

	}

}
