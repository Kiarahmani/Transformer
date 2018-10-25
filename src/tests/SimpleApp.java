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

public class SimpleApp {
	private Connection connect = null;
	private ResultSet rs = null;
	private int _ISOLATION = Connection.TRANSACTION_READ_COMMITTED;
	private int insID;
	Properties p;

	public SimpleApp(int insID) {
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
			PreparedStatement ps = connect.prepareStatement("update B set name=Kia where id=1");
			int i = ps.executeUpdate();

		} catch (Exception e) {
			throw e;
		} finally {
		}

	}

	public void updateBalance(int key) throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			System.out.println("connected: " + connect);
			PreparedStatement ps = connect.prepareStatement("update A set balance= 5 where id=1");
			ps.executeUpdate("update A set balance= 5 where id=1");
		} catch (Exception e) {
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
			PreparedStatement ps = connect.prepareStatement("select * from A where id=1");
			rs = ps.executeQuery("select * from A where id=1");
			rs.next();
			int id = rs.getInt("id");
			int balance = rs.getInt("balance");
			ps = connect.prepareStatement("select * from B where id=1");
			rs = ps.executeQuery("select * from B where id=1");
			String name = rs.getString("name");
			System.out.println("(" + id + "," + name + "," + balance + ")");

		} catch (Exception e) {
			throw e;
		} finally {

		}

	}

}
