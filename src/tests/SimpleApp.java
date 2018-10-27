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
import java.util.concurrent.ThreadLocalRandom;

public class SimpleApp {
	private Connection connect = null;
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
			PreparedStatement ps = connect.prepareStatement("update A set balance= 5 where id=?");
			ps.setInt(1, key--);
			ps.executeUpdate();
			int randomNum = ThreadLocalRandom.current().nextInt(100, 5000 + 1);
			
			PreparedStatement ps2 = connect.prepareStatement("update A set balance= 5 where id=?");
			ps2.setInt(1, randomNum);
			ps2.executeUpdate();

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
			ResultSet rs1 = ps.executeQuery("select * from A where id=1");
			rs1.next();
			//
			rs1.next();
			//
			//
			rs1.next();
			int id = rs1.getInt("id");
			int balance = rs1.getInt("balance");
			ps = connect.prepareStatement("select * from B where id=?");
			ps.setInt(1,balance+1200);
			ResultSet rs2 = ps.executeQuery();
			rs2.next();
			String name = rs2.getString("name");
			System.out.println("(" + id + "," + name + "," + balance + ")");

		} catch (Exception e) {
			throw e;
		} finally {

		}

	}

}
